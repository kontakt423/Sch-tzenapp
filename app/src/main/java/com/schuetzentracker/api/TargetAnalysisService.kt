package com.schuetzentracker.api

import android.graphics.Bitmap
import com.schuetzentracker.model.TargetAnalysisResult

// ────────────────────────────────────────────────
// API-ANBIETER
// ────────────────────────────────────────────────

enum class ApiProvider(val displayName: String, val description: String) {
    GEMINI(
        "Google Gemini",
        "Kostenlos • Gemini 1.5 Flash • 1.500 Anfragen/Tag"
    ),
    CLAUDE(
        "Anthropic Claude",
        "Kostenpflichtig • Claude Vision • Sehr hohe Genauigkeit"
    )
}

// ────────────────────────────────────────────────
// UNIFIED ANALYSIS SERVICE
// Wählt zur Laufzeit den richtigen Anbieter.
// Keys können jederzeit ohne App-Neustart gewechselt werden.
// ────────────────────────────────────────────────

class TargetAnalysisService(
    private var geminiService: GeminiVisionService?,
    private var claudeService: ClaudeVisionService?,
    private var activeProvider: ApiProvider = ApiProvider.GEMINI
) {
    /** Wird vom SettingsViewModel aufgerufen wenn Keys geändert werden */
    fun updateServices(
        gemini: GeminiVisionService?,
        claude: ClaudeVisionService?,
        provider: ApiProvider
    ) {
        geminiService  = gemini
        claudeService  = claude
        activeProvider = provider
    }

    fun setProvider(provider: ApiProvider) {
        activeProvider = provider
    }

    val isConfigured: Boolean
        get() = when (activeProvider) {
            ApiProvider.GEMINI -> geminiService != null
            ApiProvider.CLAUDE -> claudeService != null
        }

    val currentProvider: ApiProvider get() = activeProvider

    suspend fun analyzeTarget(
        bitmap: Bitmap,
        discipline: String,
        maxRingsPerShot: Int = 10
    ): Result<TargetAnalysisResult> = when (activeProvider) {
        ApiProvider.GEMINI -> {
            geminiService?.analyzeTarget(bitmap, discipline, maxRingsPerShot)
                ?: Result.failure(Exception(
                    "Kein Gemini API-Key gesetzt.\n" +
                    "Einstellungen → KI-Analyse → Gemini Key eintragen.\n" +
                    "Kostenlos unter: aistudio.google.com/app/apikey"
                ))
        }
        ApiProvider.CLAUDE -> {
            claudeService?.analyzeTarget(bitmap, discipline, maxRingsPerShot)
                ?: Result.failure(Exception(
                    "Kein Claude API-Key gesetzt.\n" +
                    "Einstellungen → KI-Analyse → Claude Key eintragen."
                ))
        }
    }
}
