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
// ANTHROPIC VISION API
// ────────────────────────────────────────────────

class ClaudeVisionService(
    private val apiKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Analysiert ein Scheibenfotos mit Claude Vision.
     * Erkennt automatisch:
     *  - Einschläge (Löcher) und deren Ringwert
     *  - Schusspflaster (überkleben alter Schüsse)
     *  - Gesamtringzahl und Schussanzahl
     *  - Streukreis / Treffergruppe
     */
    suspend fun analyzeTarget(
        bitmap: Bitmap,
        discipline: String,
        maxRingsPerShot: Int = 10
    ): Result<TargetAnalysisResult> = withContext(Dispatchers.IO) {
        runCatching {
            val base64Image = bitmapToBase64(bitmap)

            val prompt = buildAnalysisPrompt(discipline, maxRingsPerShot)

            val requestBody = buildRequestBody(base64Image, prompt)

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
                ?: throw Exception("Leere API-Antwort")

            if (!response.isSuccessful) {
                throw Exception("API-Fehler ${response.code}: $responseBody")
            }

            parseApiResponse(responseBody, maxRingsPerShot)
        }
    }

    // ────────────────────────────────────────────
    // PROMPT ENGINEERING
    // ────────────────────────────────────────────

    private fun buildAnalysisPrompt(discipline: String, maxRings: Int): String = """
        Du bist ein Experte für Schützensport und analysierst Fotos von Zielscheiben.
        
        Disziplin: $discipline (max. $maxRings Ringe pro Schuss)
        
        AUFGABE:
        Analysiere das Scheibenfotos sorgfältig und identifiziere:
        
        1. EINSCHLÄGE (Schusslöcher):
           - Erkenne jedes Schussloch auf der Scheibe
           - Bestimme den Ringwert (0-$maxRings) für jeden Einschlag
           - Beachte: Ein Schuss zählt zum höheren Ring, wenn er die Ringlinie berührt
        
        2. SCHUSSPFLASTER (Reparaturpflaster):
           - Erkenne weiße/helle rechteckige oder runde Aufkleber auf der Scheibe
           - Das sind Pflaster, die alte Schusslöcher abdecken
           - Schätze den Ringwert der Einschläge UNTER den Pflastern
           - Zähle Pflaster-Schüsse zum Gesamtergebnis dazu
        
        3. TREFFERGRUPPE:
           - Beurteile, wo die meisten Treffer liegen (Mitte/Oben/Unten/Links/Rechts)
        
        Antworte AUSSCHLIESSLICH im folgenden JSON-Format (kein Text davor oder danach):
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
          "notes": "Gutes Trefferbild, leicht nach oben versetzt"
        }
        
        group_center muss eines sein: BULL, INNER, MIDDLE, OUTER, MISS
        x und y sind relative Koordinaten (0.0 = links/oben, 1.0 = rechts/unten)
        
        Wenn du einen Einschlag nicht sicher erkennen kannst, setze confidence < 0.7.
    """.trimIndent()

    private fun buildRequestBody(base64Image: String, prompt: String): String {
        return """
        {
            "model": "claude-sonnet-4-6",
            "max_tokens": 2000,
            "messages": [
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "image",
                            "source": {
                                "type": "base64",
                                "media_type": "image/jpeg",
                                "data": "$base64Image"
                            }
                        },
                        {
                            "type": "text",
                            "text": ${gson.toJson(prompt)}
                        }
                    ]
                }
            ]
        }
        """.trimIndent()
    }

    // ────────────────────────────────────────────
    // RESPONSE PARSING
    // ────────────────────────────────────────────

    private fun parseApiResponse(responseBody: String, maxRings: Int): TargetAnalysisResult {
        val apiResponse = gson.fromJson(responseBody, JsonObject::class.java)
        val textContent = apiResponse
            .getAsJsonArray("content")
            .first { it.asJsonObject.get("type").asString == "text" }
            .asJsonObject
            .get("text").asString

        // JSON aus der Antwort extrahieren
        val jsonString = extractJson(textContent)
        val analysisJson = gson.fromJson(jsonString, JsonObject::class.java)

        val shots = analysisJson.getAsJsonArray("shots").map { shotEl ->
            val shot = shotEl.asJsonObject
            DetectedShot(
                x = shot.get("x").asFloat,
                y = shot.get("y").asFloat,
                rings = shot.get("rings").asInt.coerceIn(0, maxRings),
                isPatch = shot.get("is_patch").asBoolean,
                confidence = shot.get("confidence").asFloat
            )
        }

        val groupCenterStr = analysisJson.get("group_center").asString
        val groupCenter = runCatching { ShotZone.valueOf(groupCenterStr) }.getOrNull()

        return TargetAnalysisResult(
            detectedRings = shots,
            totalRings = analysisJson.get("total_rings").asInt,
            shotCount = analysisJson.get("shot_count").asInt,
            patchesDetected = analysisJson.get("patches_count").asInt,
            patchRings = analysisJson.get("patch_rings").asInt,
            groupSize = null,  // Berechnung benötigt Kalibrierung
            groupCenter = groupCenter,
            confidence = analysisJson.get("overall_confidence").asFloat,
            rawApiResponse = textContent
        )
    }

    private fun extractJson(text: String): String {
        // JSON aus der KI-Antwort extrahieren (falls Text darum herum)
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) {
            text.substring(start, end + 1)
        } else {
            throw Exception("Kein gültiges JSON in der Antwort gefunden:\n$text")
        }
    }

    // ────────────────────────────────────────────
    // HELPER
    // ────────────────────────────────────────────

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Qualität 85% – gute Balance zwischen Qualität und Upload-Größe
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
