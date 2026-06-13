package com.schuetzentracker.ui.analysis

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schuetzentracker.api.ClaudeVisionService
import com.schuetzentracker.api.GeminiVisionService
import com.schuetzentracker.api.TargetAnalysisService
import com.schuetzentracker.data.repository.TrainingRepository
import com.schuetzentracker.data.repository.UserPreferencesRepository
import com.schuetzentracker.model.TargetAnalysisResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TargetAnalysisViewModel @Inject constructor(
    private val repository: TrainingRepository,
    private val analysisService: TargetAnalysisService,
    private val prefsRepo: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "TargetAnalysisVM"

    private val _uiState = MutableStateFlow(TargetAnalysisUiState())
    val uiState: StateFlow<TargetAnalysisUiState> = _uiState.asStateFlow()

    private var currentSeriesId: Long = 0L
    private var disciplineName: String = ""

    fun load(seriesId: Long) {
        currentSeriesId = seriesId
        viewModelScope.launch {
            val ctx = repository.getSeriesAnalysisContext(seriesId)
            if (ctx == null) {
                _uiState.update { it.copy(isAnalyzing = false, error = "Serie nicht gefunden.") }
                return@launch
            }
            disciplineName = ctx.disciplineName

            when {
                ctx.analysisResult != null -> {
                    // Gespeichertes Ergebnis direkt anzeigen
                    _uiState.update { state ->
                        state.copy(
                            imagePath = ctx.targetImagePath,
                            result = ctx.analysisResult,
                            maxRingsPerShot = ctx.maxRingsPerShot,
                            isAnalyzing = false,
                            error = null
                        )
                    }
                }
                ctx.targetImagePath != null -> {
                    // Bild vorhanden aber noch nicht analysiert → Analyse starten
                    _uiState.update { state ->
                        state.copy(
                            imagePath = ctx.targetImagePath,
                            maxRingsPerShot = ctx.maxRingsPerShot,
                            isAnalyzing = true,
                            error = null
                        )
                    }
                    startAnalysis(ctx.targetImagePath, ctx.maxRingsPerShot)
                }
                else -> {
                    // Kein Bild vorhanden
                    _uiState.update { state ->
                        state.copy(
                            imagePath = null,
                            maxRingsPerShot = ctx.maxRingsPerShot,
                            isAnalyzing = false,
                            error = null
                        )
                    }
                }
            }
        }
    }

    fun retry() {
        val imagePath = _uiState.value.imagePath ?: run {
            _uiState.update { it.copy(error = "Kein Bild vorhanden.") }
            return
        }
        _uiState.update { it.copy(error = null, result = null, isAnalyzing = true) }
        startAnalysis(imagePath, _uiState.value.maxRingsPerShot)
    }

    private fun startAnalysis(imagePath: String, maxRings: Int) {
        viewModelScope.launch {
            try {
                // API-Keys immer frisch laden
                val prefs = prefsRepo.userPreferences.first()
                analysisService.updateServices(
                    gemini   = if (prefs.geminiApiKey.isNotBlank()) GeminiVisionService(prefs.geminiApiKey) else null,
                    claude   = if (prefs.claudeApiKey.isNotBlank()) ClaudeVisionService(prefs.claudeApiKey) else null,
                    provider = prefs.apiProvider
                )

                if (!analysisService.isConfigured) {
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            error = "Kein API-Key gesetzt.\n" +
                                "Einstellungen → KI-Analyse → Gemini Key eintragen.\n" +
                                "Kostenlos: aistudio.google.com/app/apikey"
                        )
                    }
                    return@launch
                }

                val bitmap = readBitmap(imagePath) ?: run {
                    _uiState.update { it.copy(isAnalyzing = false, error = "Bild konnte nicht geladen werden.") }
                    return@launch
                }

                Log.d(TAG, "Starte KI-Analyse: ${bitmap.width}×${bitmap.height}, Disziplin=$disciplineName")

                repository.analyzeTargetImage(
                    bitmap          = bitmap,
                    seriesId        = currentSeriesId,
                    maxRingsPerShot = maxRings,
                    disciplineName  = disciplineName
                )
                    .onSuccess { result ->
                        Log.d(TAG, "Analyse OK: ${result.totalRings} Ringe / ${result.shotCount} Schüsse")
                        _uiState.update { it.copy(isAnalyzing = false, result = result, error = null) }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Analyse fehlgeschlagen: ${error.message}")
                        _uiState.update { it.copy(isAnalyzing = false, error = error.message ?: "Unbekannter Fehler") }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Unerwarteter Fehler", e)
                _uiState.update { it.copy(isAnalyzing = false, error = "Fehler: ${e.message}") }
            }
        }
    }

    private fun readBitmap(path: String): android.graphics.Bitmap? {
        val uri = Uri.parse(path)
        return try {
            when (uri.scheme) {
                "content" -> context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                "file"    -> BitmapFactory.decodeFile(uri.path)
                else      -> context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bild-Lese-Fehler: ${e.message}")
            null
        }
    }

    fun applyResultToShots() {
        // Ergebnis ist bereits in der DB gespeichert (analyzeTargetImage → seriesDao.updateAnalysis)
    }
}

data class TargetAnalysisUiState(
    val isAnalyzing: Boolean = false,
    val result: TargetAnalysisResult? = null,
    val imagePath: String? = null,
    val maxRingsPerShot: Int = 10,
    val error: String? = null
)
