package com.schuetzentracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.schuetzentracker.api.ApiProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.userPreferencesDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ds = context.userPreferencesDataStore

    private object Keys {
        val SHOOTER_NAME         = stringPreferencesKey("shooter_name")
        val CLUB_NAME            = stringPreferencesKey("club_name")
        val MEMBER_NUMBER        = stringPreferencesKey("member_number")
        val DEFAULT_LOCATION     = stringPreferencesKey("default_location")
        val DEFAULT_DISCIPLINE_ID= longPreferencesKey("default_discipline_id")
        val DAILY_REMINDER       = booleanPreferencesKey("daily_reminder_enabled")
        val REMINDER_HOUR        = intPreferencesKey("reminder_hour")
        val GEMINI_API_KEY       = stringPreferencesKey("gemini_api_key")
        val CLAUDE_API_KEY       = stringPreferencesKey("custom_api_key")
        val API_PROVIDER         = stringPreferencesKey("api_provider")
        val SHOW_DECIMAL_RINGS   = booleanPreferencesKey("show_decimal_rings")
        val FIRST_LAUNCH_DONE    = booleanPreferencesKey("first_launch_done")
    }

    val userPreferences: Flow<UserPreferences> = ds.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toPrefs() }

    val shooterName: Flow<String>  = userPreferences.map { it.shooterName }
    val geminiApiKey: Flow<String> = userPreferences.map { it.geminiApiKey }
    val claudeApiKey: Flow<String> = userPreferences.map { it.claudeApiKey }
    val apiProvider: Flow<ApiProvider> = userPreferences.map { it.apiProvider }

    suspend fun updateShooterName(v: String)       = edit { it[Keys.SHOOTER_NAME] = v }
    suspend fun updateClubName(v: String)           = edit { it[Keys.CLUB_NAME] = v }
    suspend fun updateMemberNumber(v: String)       = edit { it[Keys.MEMBER_NUMBER] = v }
    suspend fun updateDefaultLocation(v: String)    = edit { it[Keys.DEFAULT_LOCATION] = v }
    suspend fun updateDefaultDisciplineId(id: Long) = edit { it[Keys.DEFAULT_DISCIPLINE_ID] = id }
    suspend fun updateDailyReminder(v: Boolean)     = edit { it[Keys.DAILY_REMINDER] = v }
    suspend fun updateReminderHour(v: Int)          = edit { it[Keys.REMINDER_HOUR] = v.coerceIn(0, 23) }
    suspend fun updateGeminiApiKey(v: String)       = edit { it[Keys.GEMINI_API_KEY] = v.trim() }
    suspend fun updateClaudeApiKey(v: String)       = edit { it[Keys.CLAUDE_API_KEY] = v.trim() }
    suspend fun updateApiProvider(v: ApiProvider)   = edit { it[Keys.API_PROVIDER] = v.name }
    suspend fun markFirstLaunchDone()               = edit { it[Keys.FIRST_LAUNCH_DONE] = true }
    suspend fun clearAll()                          = ds.edit { it.clear() }

    private suspend fun edit(block: (MutablePreferences) -> Unit) = ds.edit { block(it) }

    private fun Preferences.toPrefs() = UserPreferences(
        shooterName     = this[Keys.SHOOTER_NAME] ?: "",
        clubName        = this[Keys.CLUB_NAME] ?: "",
        memberNumber    = this[Keys.MEMBER_NUMBER] ?: "",
        defaultLocation = this[Keys.DEFAULT_LOCATION] ?: "Schießstand",
        defaultDisciplineId = this[Keys.DEFAULT_DISCIPLINE_ID] ?: 0L,
        dailyReminderEnabled = this[Keys.DAILY_REMINDER] ?: false,
        reminderHour    = this[Keys.REMINDER_HOUR] ?: 18,
        geminiApiKey    = this[Keys.GEMINI_API_KEY] ?: "",
        claudeApiKey    = this[Keys.CLAUDE_API_KEY] ?: "",
        apiProvider     = this[Keys.API_PROVIDER]
            ?.let { runCatching { ApiProvider.valueOf(it) }.getOrNull() }
            ?: ApiProvider.GEMINI,
        showDecimalRings = this[Keys.SHOW_DECIMAL_RINGS] ?: false,
        firstLaunchDone  = this[Keys.FIRST_LAUNCH_DONE] ?: false
    )
}

data class UserPreferences(
    val shooterName: String = "",
    val clubName: String = "",
    val memberNumber: String = "",
    val defaultLocation: String = "Schießstand",
    val defaultDisciplineId: Long = 0L,
    val dailyReminderEnabled: Boolean = false,
    val reminderHour: Int = 18,
    val geminiApiKey: String = "",
    val claudeApiKey: String = "",
    val apiProvider: ApiProvider = ApiProvider.GEMINI,
    val showDecimalRings: Boolean = false,
    val firstLaunchDone: Boolean = false
)
