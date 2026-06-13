package com.schuetzentracker.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schuetzentracker.data.repository.TrainingRepository
import com.schuetzentracker.model.TrainingMode
import com.schuetzentracker.model.TrainingSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiaryViewModelImpl @Inject constructor(
    private val repository: TrainingRepository
) : ViewModel() {

    private val _modeFilter = MutableStateFlow<TrainingMode?>(null)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<DiaryUiState> = combine(
        repository.getAllSessions(),
        _modeFilter,
        _searchQuery
    ) { sessions, modeFilter, query ->
        val filtered = sessions
            .filter { s -> modeFilter == null || s.mode == modeFilter }
            .filter { s ->
                if (query.isBlank()) true
                else s.disciplineName.contains(query, ignoreCase = true)
                        || s.notes.contains(query, ignoreCase = true)
                        || s.location.contains(query, ignoreCase = true)
                        || s.competitionName?.contains(query, ignoreCase = true) == true
            }

        val grouped = groupByMonth(filtered)
        val avg = filtered.map { it.totalRings }.average()
            .takeIf { filtered.isNotEmpty() && !it.isNaN() } ?: 0.0

        DiaryUiState(
            sessions = filtered,
            groupedSessions = grouped,
            selectedMode = modeFilter,
            averageRings = avg,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DiaryUiState()
    )

    fun filterByMode(mode: TrainingMode?) { _modeFilter.value = mode }
    fun search(query: String) { _searchQuery.value = query }

    fun deleteSession(id: Long) {
        viewModelScope.launch { repository.deleteSession(id) }
    }

    private fun groupByMonth(sessions: List<TrainingSession>): Map<String, List<TrainingSession>> {
        val fmt = DateTimeFormatter.ofPattern("MMMM yyyy")
        return sessions.groupBy { it.date.format(fmt) }
    }
}
