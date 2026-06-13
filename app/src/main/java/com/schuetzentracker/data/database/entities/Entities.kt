package com.schuetzentracker.data.database.entities

import androidx.room.*

// ────────────────────────────────────────────────
// DISZIPLIN-ENTITY (vollständig benutzerdefiniert)
// ────────────────────────────────────────────────

@Entity(tableName = "disciplines")
data class DisciplineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val shortName: String = "",
    val shotsPerSeries: Int = 10,
    val maxRingsPerShot: Int = 10,
    val defaultSeriesCount: Int = 1,
    val caliber: String = "",
    val weaponType: String = "",
    val weaponModel: String = "",
    val distanceMeters: Int = 10,
    val timeLimitSeconds: Int = 0,
    val targetDescription: String = "",
    val notes: String = "",
    val isCompetitionCapable: Int = 1,  // Boolean als Int (Room-kompatibel)
    val sortOrder: Int = 0
)

// ────────────────────────────────────────────────
// TRAININGSEINHEIT
// ────────────────────────────────────────────────

@Entity(
    tableName = "training_sessions",
    foreignKeys = [ForeignKey(
        entity = DisciplineEntity::class,
        parentColumns = ["id"],
        childColumns = ["disciplineId"],
        onDelete = ForeignKey.SET_DEFAULT   // Disziplin gelöscht → disciplineId bleibt (orphan)
    )],
    indices = [Index("disciplineId")]
)
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val disciplineId: Long,
    val disciplineName: String = "",        // Gecacht (kein JOIN nötig für Listen)
    val mode: String = "TRAINING",          // TrainingMode.name()
    val dateTimestamp: Long,
    val location: String,
    val notes: String,
    val targetImagePath: String?,
    val competitionName: String?,
    // Bedingungen (flattened)
    val weather: String,
    val wind: String,
    val light: String,
    val temperature: Int?,
    val fatigue: String,
    val stress: String,
    val equipment: String,
    // Performance-Cache
    val totalRings: Int,
    val totalShots: Int,
    val seriesCount: Int
)

// ────────────────────────────────────────────────
// SERIE
// ────────────────────────────────────────────────

@Entity(
    tableName = "series",
    foreignKeys = [ForeignKey(
        entity = TrainingSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class SeriesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val number: Int,
    val targetImagePath: String?,
    val totalRings: Int,
    val timeTakenSeconds: Int?,
    val analysisJson: String?
)

// ────────────────────────────────────────────────
// EINZELSCHUSS
// ────────────────────────────────────────────────

@Entity(
    tableName = "shots",
    foreignKeys = [ForeignKey(
        entity = SeriesEntity::class,
        parentColumns = ["id"],
        childColumns = ["seriesId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("seriesId")]
)
data class ShotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seriesId: Long,
    val number: Int,
    val rings: Int,
    val decimal: Double?,
    val zone: String
)

// ────────────────────────────────────────────────
// TRAININGSZIELE
// ────────────────────────────────────────────────

@Entity(tableName = "training_goals")
data class TrainingGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val disciplineId: Long,
    val disciplineName: String = "",
    val targetRings: Int,
    val deadlineTimestamp: Long?,
    val description: String,
    val isAchieved: Int = 0,             // Boolean als Int
    val achievedTimestamp: Long?
)

// ────────────────────────────────────────────────
// ACHIEVEMENTS
// ────────────────────────────────────────────────

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val unlockedTimestamp: Long?
)