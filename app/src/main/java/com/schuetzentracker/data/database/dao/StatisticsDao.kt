package com.schuetzentracker.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticsDao {

    @Query("""
        SELECT (dateTimestamp / (7 * 24 * 3600)) AS weekNumber,
               AVG(CAST(totalRings AS FLOAT)) AS avgRings,
               COUNT(*) AS sessionCount
        FROM training_sessions
        WHERE disciplineId = :disciplineId AND dateTimestamp > :since
        GROUP BY weekNumber
        ORDER BY weekNumber ASC
    """)
    fun getWeeklyStats(disciplineId: Long, since: Long): Flow<List<WeeklyStatsRow>>

    @Query("""
        SELECT strftime('%Y-%m', datetime(dateTimestamp, 'unixepoch')) AS month,
               AVG(CAST(totalRings AS FLOAT)) AS avgRings,
               MAX(totalRings) AS bestRings,
               COUNT(*) AS sessionCount
        FROM training_sessions
        WHERE disciplineId = :disciplineId
        GROUP BY month
        ORDER BY month DESC
        LIMIT 12
    """)
    fun getMonthlyStats(disciplineId: Long): Flow<List<MonthlyStatsRow>>

    @Query("""
        SELECT fatigue, AVG(CAST(totalRings AS FLOAT)) AS avgRings
        FROM training_sessions
        WHERE disciplineId = :disciplineId
        GROUP BY fatigue
    """)
    fun getFatigueCorrelation(disciplineId: Long): Flow<List<CorrelationRow>>

    @Query("""
        SELECT weather, AVG(CAST(totalRings AS FLOAT)) AS avgRings
        FROM training_sessions
        WHERE disciplineId = :disciplineId
        GROUP BY weather
    """)
    fun getWeatherCorrelation(disciplineId: Long): Flow<List<WeatherCorrelationRow>>

    @Query("""
        SELECT DISTINCT date(datetime(dateTimestamp, 'unixepoch')) AS day
        FROM training_sessions
        ORDER BY day DESC
    """)
    fun getTrainingDays(): Flow<List<String>>

    @Query("""
        SELECT mode, COUNT(*) AS count, AVG(CAST(totalRings AS FLOAT)) AS avgRings
        FROM training_sessions
        GROUP BY mode
    """)
    fun getStatsByMode(): Flow<List<ModeStatsRow>>
}

// ── Query-Ergebnis-Klassen ────────────────────────────────────────────────────
// Jede Klasse muss exakt die Spaltennamen der Query widerspiegeln.

data class WeeklyStatsRow(
    val weekNumber: Long,
    val avgRings: Double,
    val sessionCount: Int
)

data class MonthlyStatsRow(
    val month: String,
    val avgRings: Double,
    val bestRings: Int,
    val sessionCount: Int
)

data class CorrelationRow(
    val fatigue: String,
    val avgRings: Double
)

data class WeatherCorrelationRow(
    val weather: String,
    val avgRings: Double
)

data class ModeStatsRow(
    val mode: String,
    val count: Int,
    val avgRings: Double
)
