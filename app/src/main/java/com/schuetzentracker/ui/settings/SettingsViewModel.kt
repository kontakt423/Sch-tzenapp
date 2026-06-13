package com.schuetzentracker.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.schuetzentracker.api.ApiProvider
import com.schuetzentracker.api.ClaudeVisionService
import com.schuetzentracker.api.GeminiVisionService
import com.schuetzentracker.api.TargetAnalysisService
import com.schuetzentracker.data.repository.UserPreferencesRepository
import com.schuetzentracker.util.BackupService
import com.schuetzentracker.util.ReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepo: UserPreferencesRepository,
    private val analysisService: TargetAnalysisService,
    private val backupService: BackupService,
    workManager: WorkManager
) : ViewModel() {

    private val reminderManager = ReminderManager(workManager)

    // Lokale StateFlows – verhindert Jumping Cursor beim Tippen
    private val _shooterName          = MutableStateFlow("")
    private val _clubName             = MutableStateFlow("")
    private val _memberNumber         = MutableStateFlow("")
    private val _defaultLocation      = MutableStateFlow("Schießstand")
    private val _geminiApiKey         = MutableStateFlow("")
    private val _claudeApiKey         = MutableStateFlow("")
    private val _selectedProvider     = MutableStateFlow(ApiProvider.GEMINI)
    private val _dailyReminderEnabled = MutableStateFlow(false)
    private val _reminderHour         = MutableStateFlow(18)
    private val _backupMessage        = MutableStateFlow<String?>(null)

    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        _shooterName, _clubName, _memberNumber, _defaultLocation, _geminiApiKey
    ) { name, club, member, location, gemini ->
        SettingsUiState(
            shooterName = name, clubName = club,
            memberNumber = member, defaultLocation = location, geminiApiKey = gemini
        )
    }.combine(
        combine(_claudeApiKey, _selectedProvider, _dailyReminderEnabled, _reminderHour) {
                claude, provider, reminder, hour ->
            PartialSettingsState(claude, provider, reminder, hour)
        }
    ) { base, partial ->
        base.copy(
            claudeApiKey = partial.claudeApiKey,
            selectedProvider = partial.selectedProvider,
            dailyReminderEnabled = partial.dailyReminderEnabled,
            reminderHour = partial.reminderHour
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        viewModelScope.launch {
            val prefs = prefsRepo.userPreferences.first()
            _shooterName.value          = prefs.shooterName
            _clubName.value             = prefs.clubName
            _memberNumber.value         = prefs.memberNumber
            _defaultLocation.value      = prefs.defaultLocation
            _geminiApiKey.value         = prefs.geminiApiKey
            _claudeApiKey.value         = prefs.claudeApiKey
            _selectedProvider.value     = prefs.apiProvider
            _dailyReminderEnabled.value = prefs.dailyReminderEnabled
            _reminderHour.value         = prefs.reminderHour
            applyKeys(prefs.geminiApiKey, prefs.claudeApiKey, prefs.apiProvider)
        }
        setupDebounce()
    }

    @OptIn(FlowPreview::class)
    private fun setupDebounce() {
        viewModelScope.launch { _shooterName.debounce(800).drop(1).collect { prefsRepo.updateShooterName(it) } }
        viewModelScope.launch { _clubName.debounce(800).drop(1).collect { prefsRepo.updateClubName(it) } }
        viewModelScope.launch { _memberNumber.debounce(800).drop(1).collect { prefsRepo.updateMemberNumber(it) } }
        viewModelScope.launch { _defaultLocation.debounce(800).drop(1).collect { prefsRepo.updateDefaultLocation(it) } }
    }

    fun updateName(v: String)            { _shooterName.value = v }
    fun updateClubName(v: String)        { _clubName.value = v }
    fun updateMemberNumber(v: String)    { _memberNumber.value = v }
    fun updateDefaultLocation(v: String) { _defaultLocation.value = v }

    fun updateGeminiApiKey(key: String) {
        _geminiApiKey.value = key
        viewModelScope.launch {
            prefsRepo.updateGeminiApiKey(key)
            applyKeys(key, _claudeApiKey.value, _selectedProvider.value)
        }
    }

    fun updateClaudeApiKey(key: String) {
        _claudeApiKey.value = key
        viewModelScope.launch {
            prefsRepo.updateClaudeApiKey(key)
            applyKeys(_geminiApiKey.value, key, _selectedProvider.value)
        }
    }

    fun selectProvider(provider: ApiProvider) {
        _selectedProvider.value = provider
        viewModelScope.launch {
            prefsRepo.updateApiProvider(provider)
            applyKeys(_geminiApiKey.value, _claudeApiKey.value, provider)
        }
    }

    fun toggleDailyReminder(enabled: Boolean) {
        _dailyReminderEnabled.value = enabled
        viewModelScope.launch {
            prefsRepo.updateDailyReminder(enabled)
            if (enabled) reminderManager.scheduleDailyReminder(_reminderHour.value)
            else reminderManager.cancelAll()
        }
    }

    fun setReminderHour(hour: Int) {
        _reminderHour.value = hour
        viewModelScope.launch {
            prefsRepo.updateReminderHour(hour)
            if (_dailyReminderEnabled.value) reminderManager.scheduleDailyReminder(hour)
        }
    }

    fun exportData() {
        viewModelScope.launch {
            _backupMessage.value = "⏳ Exportiere Daten..."
            backupService.exportToAppFiles()
                .onSuccess { path -> _backupMessage.value = "✅ Backup gespeichert:\n$path" }
                .onFailure { e  -> _backupMessage.value  = "❌ Export fehlgeschlagen: ${e.message}" }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            _backupMessage.value = "⏳ Importiere Backup..."
            backupService.importFromUri(uri)
                .onSuccess { stats ->
                    _backupMessage.value = "✅ Import erfolgreich:\n" +
                        "${stats.disciplinesImported} Disziplinen, " +
                        "${stats.sessionsImported} Trainings importiert."
                }
                .onFailure { e -> _backupMessage.value = "❌ Import fehlgeschlagen: ${e.message}" }
        }
    }

    fun clearBackupMessage() { _backupMessage.value = null }

    private fun applyKeys(geminiKey: String, claudeKey: String, provider: ApiProvider) {
        analysisService.updateServices(
            gemini   = if (geminiKey.isNotBlank()) GeminiVisionService(geminiKey) else null,
            claude   = if (claudeKey.isNotBlank()) ClaudeVisionService(claudeKey) else null,
            provider = provider
        )
    }
}

private data class PartialSettingsState(
    val claudeApiKey: String,
    val selectedProvider: ApiProvider,
    val dailyReminderEnabled: Boolean,
    val reminderHour: Int
)

data class SettingsUiState(
    val shooterName: String = "",
    val clubName: String = "",
    val memberNumber: String = "",
    val defaultLocation: String = "Schießstand",
    val dailyReminderEnabled: Boolean = false,
    val reminderHour: Int = 18,
    val geminiApiKey: String = "",
    val claudeApiKey: String = "",
    val selectedProvider: ApiProvider = ApiProvider.GEMINI
)
