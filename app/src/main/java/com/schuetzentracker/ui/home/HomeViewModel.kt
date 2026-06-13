package com.schuetzentracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schuetzentracker.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val trainingRepo: TrainingRepository,
    private val statsRepo: StatisticsRepository,
    private val achievementRepo: AchievementRepository,
    private val disciplineRepo: DisciplineRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        trainingRepo.getAllSessions(),
        statsRepo.getTotalSessionCount(),
        statsRepo.getTrainingStreak(),
        achievementRepo.getAllAchievements(),
        disciplineRepo.getAll()
    ) { sessions, totalCount, streak, achievements, disciplines ->

        val avgRings = sessions.take(10).map { it.totalRings }
            .let { if (it.isEmpty()) 0.0 else it.average() }

        val competitionCount = sessions.count { it.isCompetition }

        val newAchievements = achievements.filter { it.isUnlocked }
            .sortedByDescending { it.unlockedAt }.take(3)

        HomeUiState(
            totalSessions = totalCount,
            averageRings = avgRings,
            trainingStreak = streak,
            competitionCount = competitionCount,
            recentSessions = sessions.take(5),
            newAchievements = newAchievements,
            noDisciplinesConfigured = disciplines.isEmpty(),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
}
