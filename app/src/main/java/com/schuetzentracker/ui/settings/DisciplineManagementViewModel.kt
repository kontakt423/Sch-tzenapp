package com.schuetzentracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schuetzentracker.data.repository.DisciplineRepository
import com.schuetzentracker.data.repository.AchievementRepository
import com.schuetzentracker.model.DisciplineModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DisciplineManagementViewModel @Inject constructor(
    private val disciplineRepo: DisciplineRepository,
    private val achievementRepo: AchievementRepository
) : ViewModel() {

    val uiState: StateFlow<DisciplineManagementUiState> = disciplineRepo.getAll()
        .map { DisciplineManagementUiState(disciplines = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DisciplineManagementUiState()
        )

    fun add(discipline: DisciplineModel) {
        viewModelScope.launch {
            disciplineRepo.save(discipline)
            // Achievement: Erste eigene Disziplin
            if (disciplineRepo.count() == 1) {
                achievementRepo.unlockById("first_discipline")
            }
        }
    }

    fun update(discipline: DisciplineModel) {
        viewModelScope.launch { disciplineRepo.update(discipline) }
    }

    fun delete(discipline: DisciplineModel) {
        viewModelScope.launch { disciplineRepo.delete(discipline) }
    }
}
