package com.schuetzentracker.data.repository

import android.graphics.Bitmap
import com.google.gson.Gson
import com.schuetzentracker.api.TargetAnalysisService
import com.schuetzentracker.data.database.dao.*
import com.schuetzentracker.data.database.entities.*
import com.schuetzentracker.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

// ────────────────────────────────────────────────
// DISCIPLINE REPOSITORY
// ────────────────────────────────────────────────

@Singleton
class DisciplineRepository @Inject constructor(
    private val dao: DisciplineDao
) {
    fun getAll(): Flow<List<DisciplineModel>> =
        dao.getAll().map { it.map { e -> e.toDomain() } }

    fun getCompetitionCapable(): Flow<List<DisciplineModel>> =
        dao.getCompetitionCapable().map { it.map { e -> e.toDomain() } }

    suspend fun getById(id: Long): DisciplineModel? =
        dao.getById(id)?.toDomain()

    suspend fun save(discipline: DisciplineModel): Long =
        dao.insert(discipline.toEntity())

    suspend fun update(discipline: DisciplineModel) =
        dao.update(discipline.toEntity())

    suspend fun delete(discipline: DisciplineModel) =
        dao.delete(discipline.toEntity())

    suspend fun count(): Int = dao.count()
}

// ────────────────────────────────────────────────
// TRAINING REPOSITORY
// ────────────────────────────────────────────────

@Singleton
class TrainingRepository @Inject constructor(
    private val sessionDao: TrainingSessionDao,
    private val seriesDao: SeriesDao,
    private val shotDao: ShotDao,
    private val disciplineDao: DisciplineDao,
    private val analysisService: TargetAnalysisService,
    private val achievementRepo: AchievementRepository
) {
    private val gson = Gson()

    fun getAllSessions(): Flow<List<TrainingSession>> =
        sessionDao.getAllWithSeries().map { it.map { e -> e.toDomain() } }

    fun getSessionsByDiscipline(disciplineId: Long): Flow<List<TrainingSession>> =
        sessionDao.getByDiscipline(disciplineId).map { it.map { e -> e.toDomain() } }

    fun getCompetitionSessions(): Flow<List<TrainingSession>> =
        sessionDao.getByModes(listOf("COMPETITION", "QUALIFICATION"))
            .map { it.map { e -> e.toDomain() } }

    suspend fun getSession(id: Long): TrainingSession? =
        sessionDao.getById(id)?.toDomain()

    suspend fun saveSession(session: TrainingSession): Long {
        val entity = session.toEntity()
        val sessionId = sessionDao.insert(entity)

        session.series.forEach { series ->
            val seriesId = seriesDao.insert(series.toEntity(sessionId, gson))
            shotDao.insertAll(series.shots.map { it.toEntity(seriesId) })
        }

        achievementRepo.checkAndUnlock(session)
        return sessionId
    }

    suspend fun deleteSession(id: Long) = sessionDao.deleteById(id)

    fun getPersonalBest(disciplineId: Long): Flow<Int?> =
        sessionDao.getPersonalBest(disciplineId)

    fun getTotalCount(): Flow<Int> = sessionDao.getTotalCount()

    suspend fun analyzeTargetImage(
        bitmap: Bitmap,
        seriesId: Long,
        maxRingsPerShot: Int,
        disciplineName: String
    ): Result<TargetAnalysisResult> {
        val result = analysisService.analyzeTarget(bitmap, disciplineName, maxRingsPerShot)
        result.onSuccess { if (seriesId > 0L) seriesDao.updateAnalysis(seriesId, gson.toJson(it)) }
        return result
    }

    data class SeriesAnalysisContext(
        val targetImagePath: String?,
        val analysisResult: TargetAnalysisResult?,
        val disciplineName: String,
        val maxRingsPerShot: Int
    )

    suspend fun getSeriesAnalysisContext(seriesId: Long): SeriesAnalysisContext? {
        val seriesWithShots = seriesDao.getById(seriesId) ?: return null
        val sessionWithSeries = sessionDao.getById(seriesWithShots.series.sessionId)
        val discipline = sessionWithSeries?.session?.disciplineId
            ?.let { disciplineDao.getById(it) }
        return SeriesAnalysisContext(
            targetImagePath = seriesWithShots.series.targetImagePath,
            analysisResult = seriesWithShots.series.analysisJson?.let { json ->
                runCatching { gson.fromJson(json, TargetAnalysisResult::class.java) }.getOrNull()
            },
            disciplineName = sessionWithSeries?.session?.disciplineName ?: "",
            maxRingsPerShot = discipline?.maxRingsPerShot ?: 10
        )
    }
}

// ────────────────────────────────────────────────
// STATISTICS REPOSITORY
// ────────────────────────────────────────────────

@Singleton
class StatisticsRepository @Inject constructor(
    private val sessionDao: TrainingSessionDao,
    private val statsDao: StatisticsDao
) {
    fun getPersonalBest(disciplineId: Long): Flow<Int?> =
        sessionDao.getPersonalBest(disciplineId)

    fun getTotalSessionCount(): Flow<Int> = sessionDao.getTotalCount()

    fun getWeeklyStats(disciplineId: Long, weeksBack: Int = 12): Flow<List<WeeklyStatsRow>> {
        val since = System.currentTimeMillis() / 1000 - (weeksBack * 7 * 24 * 3600L)
        return statsDao.getWeeklyStats(disciplineId, since)
    }

    fun getMonthlyStats(disciplineId: Long): Flow<List<MonthlyStatsRow>> =
        statsDao.getMonthlyStats(disciplineId)

    fun getFatigueCorrelation(disciplineId: Long): Flow<List<CorrelationRow>> =
        statsDao.getFatigueCorrelation(disciplineId)

    fun getWeatherCorrelation(disciplineId: Long): Flow<List<WeatherCorrelationRow>> =
        statsDao.getWeatherCorrelation(disciplineId)

    fun getTrainingStreak(): Flow<Int> =
        statsDao.getTrainingDays().map { calculateStreak(it) }

    fun getStatsByMode(): Flow<List<ModeStatsRow>> =
        statsDao.getStatsByMode()

    private fun calculateStreak(days: List<String>): Int {
        if (days.isEmpty()) return 0
        var streak = 0
        var expected = LocalDate.now()
        for (dayStr in days.sortedDescending()) {
            val day = runCatching { LocalDate.parse(dayStr) }.getOrNull() ?: break
            if (day == expected) { streak++; expected = expected.minusDays(1) }
            else break
        }
        return streak
    }
}

// ────────────────────────────────────────────────
// GOALS REPOSITORY
// ────────────────────────────────────────────────

@Singleton
class GoalsRepository @Inject constructor(
    private val goalDao: TrainingGoalDao
) {
    fun getAllGoals(): Flow<List<TrainingGoal>> =
        goalDao.getAll().map { it.map { e -> e.toDomain() } }

    fun getActiveGoals(disciplineId: Long): Flow<List<TrainingGoal>> =
        goalDao.getActiveForDiscipline(disciplineId).map { it.map { e -> e.toDomain() } }

    suspend fun saveGoal(goal: TrainingGoal) = goalDao.insert(goal.toEntity())
    suspend fun deleteGoal(goal: TrainingGoal) = goalDao.delete(goal.toEntity())
    suspend fun markAchieved(goalId: Long) =
        goalDao.markAchieved(goalId, System.currentTimeMillis() / 1000)
}

// ────────────────────────────────────────────────
// ACHIEVEMENTS REPOSITORY
// ────────────────────────────────────────────────

@Singleton
class AchievementRepository @Inject constructor(
    private val achievementDao: AchievementDao
) {
    fun getAllAchievements(): Flow<List<Achievement>> =
        achievementDao.getAll().map { unlocked ->
            Achievements.ALL.map { achievement ->
                val found = unlocked.find { it.id == achievement.id }
                if (found?.unlockedTimestamp != null) {
                    achievement.copy(
                        unlockedAt = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(found.unlockedTimestamp),
                            ZoneId.systemDefault()
                        )
                    )
                } else achievement
            }
        }

    suspend fun checkAndUnlock(session: TrainingSession) {
        val now = System.currentTimeMillis() / 1000
        unlock("first_session", now)
        if (session.isCompetition) unlock("competition", now)
        val hasPerfect = session.series.any { s ->
            s.shots.isNotEmpty() && s.shots.all { it.rings >= 10 }
        }
        if (hasPerfect) unlock("perfect_series", now)
    }

    suspend fun unlockById(id: String) {
        unlock(id, System.currentTimeMillis() / 1000)
    }

    private suspend fun unlock(id: String, ts: Long) {
        achievementDao.unlock(AchievementEntity(id, ts))
    }
}

// ────────────────────────────────────────────────
// MAPPER: Entity ↔ Domain
// ────────────────────────────────────────────────

fun DisciplineEntity.toDomain() = DisciplineModel(
    id = id, name = name, shortName = shortName,
    shotsPerSeries = shotsPerSeries, maxRingsPerShot = maxRingsPerShot,
    defaultSeriesCount = defaultSeriesCount, caliber = caliber,
    weaponType = weaponType, weaponModel = weaponModel,
    distanceMeters = distanceMeters, timeLimitSeconds = timeLimitSeconds,
    targetDescription = targetDescription, notes = notes,
    isCompetitionCapable = isCompetitionCapable == 1, sortOrder = sortOrder
)

fun DisciplineModel.toEntity() = DisciplineEntity(
    id = id, name = name, shortName = shortName,
    shotsPerSeries = shotsPerSeries, maxRingsPerShot = maxRingsPerShot,
    defaultSeriesCount = defaultSeriesCount, caliber = caliber,
    weaponType = weaponType, weaponModel = weaponModel,
    distanceMeters = distanceMeters, timeLimitSeconds = timeLimitSeconds,
    targetDescription = targetDescription, notes = notes,
    isCompetitionCapable = if (isCompetitionCapable) 1 else 0, sortOrder = sortOrder
)

fun SessionWithSeries.toDomain() = TrainingSession(
    id = session.id,
    disciplineId = session.disciplineId,
    disciplineName = session.disciplineName,
    mode = runCatching { TrainingMode.valueOf(session.mode) }.getOrDefault(TrainingMode.TRAINING),
    date = LocalDateTime.ofInstant(Instant.ofEpochSecond(session.dateTimestamp), ZoneId.systemDefault()),
    location = session.location,
    series = series.map { it.toDomain() },
    conditions = ShootingConditions(
        weather = runCatching { WeatherCondition.valueOf(session.weather) }.getOrDefault(WeatherCondition.INDOOR),
        wind = runCatching { WindCondition.valueOf(session.wind) }.getOrDefault(WindCondition.NONE),
        light = runCatching { LightCondition.valueOf(session.light) }.getOrDefault(LightCondition.ARTIFICIAL),
        temperature = session.temperature,
        fatigue = runCatching { FatigueLevel.valueOf(session.fatigue) }.getOrDefault(FatigueLevel.NORMAL),
        stress = runCatching { StressLevel.valueOf(session.stress) }.getOrDefault(StressLevel.NORMAL),
        equipment = session.equipment
    ),
    notes = session.notes,
    targetImagePath = session.targetImagePath,
    competitionName = session.competitionName
)

fun SeriesWithShots.toDomain() = Series(
    id = series.id, number = series.number,
    shots = shots.map {
        Shot(it.number, it.rings, it.decimal,
            runCatching { ShotZone.valueOf(it.zone) }.getOrDefault(ShotZone.UNKNOWN))
    },
    targetImagePath = series.targetImagePath,
    timeTakenSeconds = series.timeTakenSeconds,
    analysisResult = series.analysisJson?.let { json ->
        runCatching { Gson().fromJson(json, TargetAnalysisResult::class.java) }.getOrNull()
    }
)

fun TrainingSession.toEntity(): TrainingSessionEntity {
    val offset = ZoneId.systemDefault().rules.getOffset(date)
    return TrainingSessionEntity(
        id = id, disciplineId = disciplineId, disciplineName = disciplineName,
        mode = mode.name,
        dateTimestamp = date.toEpochSecond(offset),
        location = location, notes = notes,
        targetImagePath = targetImagePath, competitionName = competitionName,
        weather = conditions.weather.name, wind = conditions.wind.name,
        light = conditions.light.name, temperature = conditions.temperature,
        fatigue = conditions.fatigue.name, stress = conditions.stress.name,
        equipment = conditions.equipment,
        totalRings = totalRings, totalShots = totalShots, seriesCount = series.size
    )
}

fun Series.toEntity(sessionId: Long, gson: Gson) = SeriesEntity(
    id = id, sessionId = sessionId, number = number,
    targetImagePath = targetImagePath, totalRings = totalRings,
    timeTakenSeconds = timeTakenSeconds,
    analysisJson = analysisResult?.let { gson.toJson(it) }
)

fun Shot.toEntity(seriesId: Long) = ShotEntity(
    seriesId = seriesId, number = number,
    rings = rings, decimal = decimal, zone = zone.name
)

fun TrainingGoalEntity.toDomain() = TrainingGoal(
    id = id, disciplineId = disciplineId, disciplineName = disciplineName,
    targetRings = targetRings,
    deadline = deadlineTimestamp?.let { LocalDate.ofEpochDay(it / 86400) },
    description = description,
    isAchieved = isAchieved == 1,
    achievedDate = achievedTimestamp?.let { LocalDate.ofEpochDay(it / 86400) }
)

fun TrainingGoal.toEntity() = TrainingGoalEntity(
    id = id, disciplineId = disciplineId, disciplineName = disciplineName,
    targetRings = targetRings,
    deadlineTimestamp = deadline?.toEpochDay()?.times(86400),
    description = description,
    isAchieved = if (isAchieved) 1 else 0,
    achievedTimestamp = achievedDate?.toEpochDay()?.times(86400)
)
