package com.schuetzentracker.api

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.schuetzentracker.model.DetectedShot
import com.schuetzentracker.model.ShotZone
import com.schuetzentracker.model.TargetAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// ────────────────────────────────────────────────
// GOOGLE GEMINI VISION API
//
// Kostenlos nutzbar:
//  - Gemini 1.5 Flash: 15 Anfragen/Min, 1.500/Tag
//  - API Key holen: https://aistudio.google.com/app/apikey
//
// Endpoint:
//  POST https://generativelanguage.googleapis.com/v1beta/
//       models/gemini-1.5-flash:generateContent?key=API_KEY
// ────────────────────────────────────────────────

class GeminiVisionService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // Gemini 1.5 Flash ist kostenlos und sehr schnell für Bildanalyse
    private val MODEL = "gemini-1.5-flash"
    private val BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    suspend fun analyzeTarget(
        bitmap: Bitmap,
        discipline: String,
        maxRingsPerShot: Int = 10
    ): Result<TargetAnalysisResult> = withContext(Dispatchers.IO) {
        runCatching {
            val base64Image = bitmapToBase64(bitmap)
            val prompt = buildPrompt(discipline, maxRingsPerShot)
            val body = buildRequestBody(base64Image, prompt)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
                ?: throw Exception("Leere Antwort von Gemini API")

            if (!response.isSuccessful) {
                val errJson = runCatching {
                    gson.fromJson(responseBody, JsonObject::class.java)
                        .getAsJsonObject("error")?.get("message")?.asString
                }.getOrNull()
                throw Exception("Gemini API Fehler ${response.code}: ${errJson ?: responseBody}")
            }

            parseGeminiResponse(responseBody, maxRingsPerShot)
        }
    }

    // ────────────────────────────────────────────
    // PROMPT (identisch zu Claude – gleiches JSON-Format)
    // ────────────────────────────────────────────

    private fun buildPrompt(discipline: String, maxRings: Int): String = """
        Du bist ein Experte für Schützensport und analysierst Fotos von Zielscheiben.
        
        Disziplin: $discipline (max. $maxRings Ringe pro Schuss)
        
        AUFGABE:
        Analysiere das Scheibenfotos sorgfältig und identifiziere:
        
        1. EINSCHLÄGE (Schusslöcher):
           - Erkenne jedes Schussloch auf der Scheibe
           - Bestimme den Ringwert (0-$maxRings) für jeden Einschlag
           - Ein Schuss zählt zum höheren Ring, wenn er die Ringlinie berührt
        
        2. SCHUSSPFLASTER (Reparaturpflaster):
           - Erkenne weiße/helle rechteckige oder runde Aufkleber
           - Das sind Pflaster, die alte Schusslöcher abdecken
           - Schätze den Ringwert der Einschläge UNTER den Pflastern
        
        3. TREFFERGRUPPE:
           - Beurteile, wo die meisten Treffer liegen
        
        Antworte AUSSCHLIESSLICH im folgenden JSON-Format (kein Text davor oder danach, keine Markdown-Backticks):
        {
          "shots": [
            {
              "x": 0.5,
              "y": 0.5,
              "rings": 10,
              "is_patch": false,
              "confidence": 0.95
            }
          ],
          "total_rings": 87,
          "shot_count": 10,
          "patches_count": 2,
          "patch_rings": 18,
          "group_center": "BULL",
          "overall_confidence": 0.88,
          "notes": "Gutes Trefferbild"
        }
        
        group_center muss eines sein: BULL, INNER, MIDDLE, OUTER, MISS, UNKNOWN
        x und y sind relative Koordinaten (0.0 = links/oben, 1.0 = rechts/unten)
    """.trimIndent()

    // ────────────────────────────────────────────
    // REQUEST BODY im Gemini-Format
    // ────────────────────────────────────────────

    private fun buildRequestBody(base64Image: String, prompt: String): String {
        // Gemini erwartet: contents[].parts[] mit text und inlineData
        return """
        {
          "contents": [
            {
              "parts": [
                {
                  "inline_data": {
                    "mime_type": "image/jpeg",
                    "data": "$base64Image"
                  }
                },
                {
                  "text": ${gson.toJson(prompt)}
                }
              ]
            }
          ],
          "generationConfig": {
            "temperature": 0.1,
            "maxOutputTokens": 2000,
            "responseMimeType": "text/plain"
          }
        }
        """.trimIndent()
    }

    // ────────────────────────────────────────────
    // RESPONSE PARSING
    // Gemini-Antwortstruktur:
    // { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
    // ────────────────────────────────────────────

    private fun parseGeminiResponse(responseBody: String, maxRings: Int): TargetAnalysisResult {
        val root = gson.fromJson(responseBody, JsonObject::class.java)

        val textContent = root
            .getAsJsonArray("candidates")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString
            ?: throw Exception("Kein Text in Gemini-Antwort. Antwort: $responseBody")

        // JSON aus dem Text extrahieren (Gemini gibt manchmal ```json ... ``` zurück)
        val jsonString = extractJson(textContent)
        val analysis = gson.fromJson(jsonString, JsonObject::class.java)

        val shots = analysis.getAsJsonArray("shots")?.map { el ->
            val s = el.asJsonObject
            DetectedShot(
                x = s.get("x").asFloat,
                y = s.get("y").asFloat,
                rings = s.get("rings").asInt.coerceIn(0, maxRings),
                isPatch = s.get("is_patch").asBoolean,
                confidence = s.get("confidence").asFloat
            )
        } ?: emptyList()

        val groupCenterStr = runCatching { analysis.get("group_center").asString }.getOrDefault("UNKNOWN")
        val groupCenter = runCatching { ShotZone.valueOf(groupCenterStr) }.getOrDefault(ShotZone.UNKNOWN)

        return TargetAnalysisResult(
            detectedRings = shots,
            totalRings = runCatching { analysis.get("total_rings").asInt }.getOrDefault(shots.sumOf { it.rings }),
            shotCount = runCatching { analysis.get("shot_count").asInt }.getOrDefault(shots.size),
            patchesDetected = runCatching { analysis.get("patches_count").asInt }.getOrDefault(0),
            patchRings = runCatching { analysis.get("patch_rings").asInt }.getOrDefault(0),
            groupSize = null,
            groupCenter = groupCenter,
            confidence = runCatching { analysis.get("overall_confidence").asFloat }.getOrDefault(0.8f),
            rawApiResponse = textContent
        )
    }

    private fun extractJson(text: String): String {
        // Markdown-Code-Blöcke entfernen falls vorhanden
        val cleaned = text
            .replace("```json", "")
            .replace("```", "")
            .trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        return if (start >= 0 && end > start) cleaned.substring(start, end + 1)
        else throw Exception("Kein JSON in Gemini-Antwort:\n$text")
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
