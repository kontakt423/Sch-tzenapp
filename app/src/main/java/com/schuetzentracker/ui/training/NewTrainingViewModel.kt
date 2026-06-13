package com.schuetzentracker.ui.training

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schuetzentracker.api.GeminiVisionService
import com.schuetzentracker.api.TargetAnalysisService
import com.schuetzentracker.data.repository.DisciplineRepository
import com.schuetzentracker.data.repository.TrainingRepository
import com.schuetzentracker.data.repository.UserPreferencesRepository
import com.schuetzentracker.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class NewTrainingViewModel @Inject constructor(
    private val trainingRepo: TrainingRepository,
    private val disciplineRepo: DisciplineRepository,
    private val analysisService: TargetAnalysisService,
    private val prefsRepo: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "NewTrainingVM"
    private var analysisSeriesIndex = 0

    private val _uiState = MutableStateFlow(NewTrainingUiState())
    val uiState: StateFlow<NewTrainingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            disciplineRepo.getAll().collect { disciplines ->
                _uiState.update { state ->
                    state.copy(
                        availableDisciplines = disciplines,
                        selectedDiscipline = state.selectedDiscipline ?: disciplines.firstOrNull()
                    )
                }
            }
        }
    }

    // ── Mode & Discipline ────────────────────────────────────────────

    fun selectMode(mode: TrainingMode) = _uiState.update { it.copy(selectedMode = mode) }

    fun selectDiscipline(d: DisciplineModel) {
        _uiState.update { state ->
            val updatedSeries = state.series.map { series ->
                SeriesUiModel(
                    shots = List(d.shotsPerSeries) { i -> series.shots.getOrElse(i) { 0 } },
                    targetImagePath = series.targetImagePath
                )
            }
            state.copy(selectedDiscipline = d, series = updatedSeries)
        }
    }

    fun updateLocation(v: String)         = _uiState.update { it.copy(location = v) }
    fun updateCompetitionName(v: String)  = _uiState.update { it.copy(competitionName = v) }

    // ── Serien ──────────────────────────────────────────────────────

    fun addSeries() {
        val d = _uiState.value.selectedDiscipline ?: return
        _uiState.update { it.copy(series = it.series + SeriesUiModel(List(d.shotsPerSeries) { 0 })) }
    }

    fun removeSeries(index: Int) {
        _uiState.update { state ->
            state.copy(series = state.series.toMutableList().also { it.removeAt(index) })
        }
    }

    fun updateShot(seriesIdx: Int, shotIdx: Int, rings: Int) {
        _uiState.update { state ->
            val updated = state.series.toMutableList()
            val shots = updated[seriesIdx].shots.toMutableList()
            if (shotIdx < shots.size) shots[shotIdx] = rings
            updated[seriesIdx] = updated[seriesIdx].copy(shots = shots)
            state.copy(series = updated)
        }
    }

    // ── Foto & KI-Analyse ───────────────────────────────────────────

    fun setAnalysisSeriesIndex(index: Int) { analysisSeriesIndex = index }

    fun onTargetImageSelected(uri: Uri) {
        val idx = analysisSeriesIndex
        if (idx >= _uiState.value.series.size) return
        _uiState.update { state ->
            val updated = state.series.toMutableList()
            updated[idx] = updated[idx].copy(targetImagePath = uri.toString())
            state.copy(series = updated)
        }
        analyzeImage(idx, uri)
    }

    fun analyzeCurrentImage(seriesIdx: Int) {
        val path = _uiState.value.series.getOrNull(seriesIdx)?.targetImagePath ?: run {
            _uiState.update { it.copy(errorMessage = "Kein Bild vorhanden. Bitte zuerst ein Foto aufnehmen.") }
            return
        }
        analyzeImage(seriesIdx, Uri.parse(path))
    }

    private fun analyzeImage(seriesIdx: Int, uri: Uri) {
        val disc = _uiState.value.selectedDiscipline ?: run {
            _uiState.update { it.copy(errorMessage = "Keine Disziplin ausgewählt.") }
            return
        }

        // Lade-Spinner aktivieren
        _uiState.update { state ->
            val set = state.analyzingSeriesIndices.toMutableSet().also { it.add(seriesIdx) }
            state.copy(analyzingSeriesIndices = set, errorMessage = null)
        }

        viewModelScope.launch {
            try {
                // Key immer frisch aus DataStore laden – garantiert aktuell
                val prefs = prefsRepo.userPreferences.first()
                if (prefs.geminiApiKey.isNotBlank()) {
                    analysisService.updateServices(
                        gemini   = GeminiVisionService(prefs.geminiApiKey),
                        claude   = null,
                        provider = prefs.apiProvider
                    )
                }

                if (!analysisService.isConfigured) {
                    showError(seriesIdx,
                        "Kein API-Key gesetzt.\n" +
                        "Einstellungen → KI-Analyse → Gemini Key eintragen.\n" +
                        "Kostenlos: aistudio.google.com/app/apikey"
                    )
                    return@launch
                }

                val bitmap = readBitmapFromUri(uri) ?: run {
                    showError(seriesIdx, "Bild konnte nicht geladen werden.")
                    return@launch
                }

                Log.d(TAG, "Starte Analyse: ${bitmap.width}×${bitmap.height}, Disziplin=${disc.name}")

                trainingRepo.analyzeTargetImage(
                    bitmap          = bitmap,
                    seriesId        = 0L,
                    maxRingsPerShot = disc.maxRingsPerShot,
                    disciplineName  = disc.name
                )
                    .onSuccess { analysis ->
                        Log.d(TAG, "Analyse OK: ${analysis.totalRings} Ringe / ${analysis.shotCount} Schüsse")
                        _uiState.update { state ->
                            val results  = state.analysisResults.toMutableMap().also { it[seriesIdx] = analysis }
                            val spinning = state.analyzingSeriesIndices.toMutableSet().also { it.remove(seriesIdx) }
                            val series   = if (analysis.detectedRings.size == disc.shotsPerSeries) {
                                state.series.toMutableList().also { list ->
                                    list[seriesIdx] = list[seriesIdx].copy(
                                        shots = analysis.detectedRings.map { it.rings }
                                    )
                                }
                            } else state.series
                            state.copy(series = series, analysisResults = results, analyzingSeriesIndices = spinning)
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Analyse fehlgeschlagen", error)
                        showError(seriesIdx, error.message ?: "Unbekannter Fehler")
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Unerwarteter Fehler", e)
                showError(seriesIdx, "Fehler: ${e.message}")
            }
        }
    }

    private fun showError(seriesIdx: Int, message: String) {
        _uiState.update { state ->
            val spinning = state.analyzingSeriesIndices.toMutableSet().also { it.remove(seriesIdx) }
            state.copy(analyzingSeriesIndices = spinning, errorMessage = message)
        }
    }

    private fun readBitmapFromUri(uri: Uri): android.graphics.Bitmap? = try {
        when (uri.scheme) {
            "content" -> context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            "file"    -> BitmapFactory.decodeFile(uri.path)
            else      -> context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
    } catch (e: Exception) { Log.e(TAG, "Bild-Lese-Fehler: ${e.message}"); null }

    // ── Bedingungen ──────────────────────────────────────────────────

    fun updateWeather(v: WeatherCondition)  = _uiState.update { it.copy(conditions = it.conditions.copy(weather = v)) }
    fun updateWind(v: WindCondition)        = _uiState.update { it.copy(conditions = it.conditions.copy(wind = v)) }
    fun updateFatigue(v: FatigueLevel)      = _uiState.update { it.copy(conditions = it.conditions.copy(fatigue = v)) }
    fun updateStress(v: StressLevel)        = _uiState.update { it.copy(conditions = it.conditions.copy(stress = v)) }
    fun updateEquipment(v: String)          = _uiState.update { it.copy(conditions = it.conditions.copy(equipment = v)) }
    fun updateNotes(v: String)              = _uiState.update { it.copy(notes = v) }
    fun appendNote(note: String)            = _uiState.update { state ->
        val sep = if (state.notes.isBlank()) "" else ", "
        state.copy(notes = state.notes + sep + note)
    }
    fun dismissError()                      = _uiState.update { it.copy(errorMessage = null) }

    // ── Speichern ────────────────────────────────────────────────────

    fun saveSession(onSaved: () -> Unit) {
        val state = _uiState.value
        val disc  = state.selectedDiscipline ?: return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                val session = TrainingSession(
                    disciplineId   = disc.id,
                    disciplineName = disc.name,
                    disciplineModel = disc,
                    mode           = state.selectedMode,
                    date           = LocalDateTime.now(),
                    location       = state.location,
                    competitionName = state.competitionName.ifBlank { null },
                    series         = state.series.mapIndexed { i, su ->
                        Series(
                            number = i + 1,
                            shots  = su.shots.mapIndexed { j, rings ->
                                Shot(
                                    number = j + 1, rings = rings,
                                    zone = when {
                                        rings == disc.maxRingsPerShot      -> ShotZone.BULL
                                        rings >= disc.maxRingsPerShot - 2  -> ShotZone.INNER
                                        rings >= disc.maxRingsPerShot - 5  -> ShotZone.MIDDLE
                                        rings > 0                          -> ShotZone.OUTER
                                        else                               -> ShotZone.MISS
                                    }
                                )
                            },
                            targetImagePath = su.targetImagePath,
                            analysisResult  = state.analysisResults[i]
                        )
                    },
                    conditions = state.conditions,
                    notes      = state.notes
                )
                trainingRepo.saveSession(session)
                onSaved()
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, errorMessage = "Speichern fehlgeschlagen: ${error.message}") }
            }
        }
    }
}

// ── UI-Modelle (einzige Definition – NICHT in NewTrainingScreen.kt wiederholen) ──

data class SeriesUiModel(
    val shots: List<Int>,
    val targetImagePath: String? = null
)

data class NewTrainingUiState(
    val selectedMode: TrainingMode = TrainingMode.TRAINING,
    val selectedDiscipline: DisciplineModel? = null,
    val availableDisciplines: List<DisciplineModel> = emptyList(),
    val location: String = "Schießstand",
    val competitionName: String = "",
    val series: List<SeriesUiModel> = emptyList(),
    val conditions: ShootingConditions = ShootingConditions(),
    val notes: String = "",
    val analysisResults: Map<Int, TargetAnalysisResult> = emptyMap(),
    val analyzingSeriesIndices: Set<Int> = emptySet(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val canSave: Boolean get() =
        selectedDiscipline != null &&
        series.isNotEmpty() &&
        series.any { s -> s.shots.any { it > 0 } }

    fun isAnalyzing(seriesIndex: Int) = seriesIndex in analyzingSeriesIndices
}
