package com.schuetzentracker.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schuetzentracker.data.repository.DisciplineRepository
import com.schuetzentracker.data.repository.StatisticsRepository
import com.schuetzentracker.model.DisciplineModel
import com.schuetzentracker.model.FatigueLevel
import com.schuetzentracker.model.TrainingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statsRepo: StatisticsRepository,
    private val disciplineRepo: DisciplineRepository
) : ViewModel() {

    private val _selectedDiscipline = MutableStateFlow<DisciplineModel?>(null)
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Disziplinen laden
            disciplineRepo.getAll().collect { disciplines ->
                _uiState.update { it.copy(disciplines = disciplines) }
            }
        }
        viewModelScope.launch {
            statsRepo.getTotalSessionCount().collect { count ->
                _uiState.update { it.copy(totalSessions = count) }
            }
        }
        viewModelScope.launch {
            statsRepo.getTrainingStreak().collect { streak ->
                _uiState.update { it.copy(trainingStreak = streak) }
            }
        }
        viewModelScope.launch {
            statsRepo.getStatsByMode().collect { rows ->
                val mapped = rows.mapNotNull { row ->
                    val mode = runCatching { TrainingMode.valueOf(row.mode) }.getOrNull()
                        ?: return@mapNotNull null
                    mode to row.count
                }
                _uiState.update { it.copy(modeStats = mapped) }
            }
        }
        // Disziplin-Filter beobachten
        viewModelScope.launch {
            _selectedDiscipline.collect { discipline ->
                loadDisciplineStats(discipline)
            }
        }
    }

    fun selectDiscipline(d: DisciplineModel?) {
        _selectedDiscipline.value = d
        _uiState.update { it.copy(selectedDiscipline = d) }
    }

    private fun loadDisciplineStats(discipline: DisciplineModel?) {
        if (discipline == null) return
        val id = discipline.id

        viewModelScope.launch {
            statsRepo.getPersonalBest(id).collect { pb ->
                _uiState.update { it.copy(personalBest = pb) }
            }
        }
        viewModelScope.launch {
            statsRepo.getWeeklyStats(id).collect { data ->
                _uiState.update { it.copy(weeklyData = data) }
            }
        }
        viewModelScope.launch {
            statsRepo.getMonthlyStats(id).collect { data ->
                val avg = data.map { it.avgRings }.average().takeIf { data.isNotEmpty() && !it.isNaN() } ?: 0.0
                _uiState.update { it.copy(monthlyData = data, averageRings = avg) }
            }
        }
        viewModelScope.launch {
            statsRepo.getFatigueCorrelation(id).collect { rows ->
                val mapped = rows.map { row ->
                    val label = runCatching { FatigueLevel.valueOf(row.fatigue).label }.getOrElse { row.fatigue }
                    label to row.avgRings
                }
                _uiState.update { it.copy(fatigueCorrelation = mapped) }
            }
        }
    }
}
