package com.schuetzentracker.util

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.schuetzentracker.data.database.AppDatabase
import com.schuetzentracker.data.database.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class BackupService @Inject constructor(
    private val context: Context
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToAppFiles(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val db = AppDatabase.create(context)
            val disciplines  = db.disciplineDao().getAll().first()
            val sessions     = db.trainingSessionDao().getAllWithSeries().first()
            val goals        = db.trainingGoalDao().getAll().first()
            val achievements = db.achievementDao().getAll().first()

            val backup = BackupData(
                version      = 1,
                exportDate   = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                disciplines  = disciplines,
                sessions     = sessions.map { it.session },
                series       = sessions.flatMap { s -> s.series.map { it.series } },
                shots        = sessions.flatMap { s -> s.series.flatMap { sr -> sr.shots } },
                goals        = goals,
                achievements = achievements
            )

            val filename = "SchuetzenTracker_${
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            }.json"
            val file = File(context.filesDir, filename)
            file.writeText(gson.toJson(backup), Charsets.UTF_8)
            file.absolutePath
        }
    }

    suspend fun importFromUri(uri: Uri): Result<ImportStats> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() }
                ?: throw Exception("Datei konnte nicht gelesen werden.")

            val backup = gson.fromJson(json, BackupData::class.java)
                ?: throw Exception("Ungültiges Backup-Format.")

            val db = AppDatabase.create(context)
            var disciplinesImported = 0
            var sessionsImported    = 0
            var shotsImported       = 0

            backup.disciplines?.forEach { d ->
                db.disciplineDao().insert(d.copy(id = 0))
                disciplinesImported++
            }

            backup.sessions?.forEach { session ->
                val newSessionId = db.trainingSessionDao().insert(session.copy(id = 0))
                backup.series?.filter { it.sessionId == session.id }?.forEach { series ->
                    val newSeriesId = db.seriesDao().insert(series.copy(id = 0, sessionId = newSessionId))
                    backup.shots?.filter { it.seriesId == series.id }?.forEach { shot ->
                        db.shotDao().insert(shot.copy(id = 0, seriesId = newSeriesId))
                        shotsImported++
                    }
                }
                sessionsImported++
            }

            ImportStats(disciplinesImported, sessionsImported, shotsImported)
        }
    }
}

data class BackupData(
    val version: Int = 1,
    val exportDate: String = "",
    val disciplines: List<DisciplineEntity>? = null,
    val sessions: List<TrainingSessionEntity>? = null,
    val series: List<SeriesEntity>? = null,
    val shots: List<ShotEntity>? = null,
    val goals: List<TrainingGoalEntity>? = null,
    val achievements: List<AchievementEntity>? = null
)

data class ImportStats(
    val disciplinesImported: Int,
    val sessionsImported: Int,
    val shotsImported: Int
)
