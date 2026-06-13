package com.schuetzentracker.model

import java.time.LocalDate
import java.time.LocalDateTime

// ────────────────────────────────────────────────
// DISZIPLIN – vollständig benutzerdefiniert
// Keine voreingestellten Disziplinen.
// Der User legt alle Disziplinen selbst an.
// ────────────────────────────────────────────────

data class DisciplineModel(
    val id: Long = 0,
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
    val isCompetitionCapable: Boolean = true,
    val sortOrder: Int = 0
) {
    val maxRingsTotal: Int get() = maxRingsPerShot * shotsPerSeries
    val displayName: String get() = name
    val hasTimeLimit: Boolean get() = timeLimitSeconds > 0
}

// ────────────────────────────────────────────────
// TRAINING-MODUS
// ────────────────────────────────────────────────

enum class TrainingMode(
    val displayName: String,
    val emoji: String,
    val description: String
) {
    TRAINING("Training", "🎯", "Freies Training ohne Wertung"),
    COMPETITION("Wettkampf", "🏅", "Offizieller Wettkampf"),
    PRACTICE("Übungsschießen", "📋", "Gezieltes Technik-Training"),
    QUALIFICATION("Qualifikation", "📊", "Qualifikationsrunde")
}

// ────────────────────────────────────────────────
// TRAININGSEINHEIT
// ────────────────────────────────────────────────

data class TrainingSession(
    val id: Long = 0,
    val disciplineId: Long,
    val disciplineName: String = "",
    val disciplineModel: DisciplineModel? = null,
    val mode: TrainingMode = TrainingMode.TRAINING,
    val date: LocalDateTime,
    val location: String = "Schießstand",
    val series: List<Series> = emptyList(),
    val conditions: ShootingConditions,
    val notes: String = "",
    val targetImagePath: String? = null,
    val competitionName: String? = null
) {
    val totalRings: Int get() = series.sumOf { it.totalRings }
    val totalShots: Int get() = series.sumOf { it.shots.size }
    val isCompetition: Boolean get() =
        mode == TrainingMode.COMPETITION || mode == TrainingMode.QUALIFICATION

    val maxPossible: Int get() {
        val disc = disciplineModel ?: return series.size * 100
        return series.size * disc.maxRingsPerShot * disc.shotsPerSeries
    }

    val percentage: Double get() =
        if (maxPossible == 0) 0.0 else totalRings.toDouble() / maxPossible * 100
}

data class Series(
    val id: Long = 0,
    val number: Int,
    val shots: List<Shot>,
    val targetImagePath: String? = null,
    val analysisResult: TargetAnalysisResult? = null,
    val timeTakenSeconds: Int? = null
) {
    val totalRings: Int get() = shots.sumOf { it.rings }
}

data class Shot(
    val number: Int,
    val rings: Int,
    val decimal: Double? = null,
    val zone: ShotZone = ShotZone.UNKNOWN
)

enum class ShotZone { BULL, INNER, MIDDLE, OUTER, MISS, UNKNOWN }

// ────────────────────────────────────────────────
// KI-ANALYSE
// ────────────────────────────────────────────────

data class TargetAnalysisResult(
    val detectedRings: List<DetectedShot>,
    val totalRings: Int,
    val shotCount: Int,
    val patchesDetected: Int,
    val patchRings: Int,
    val groupSize: Double?,
    val groupCenter: ShotZone?,
    val confidence: Float,
    val rawApiResponse: String
)

data class DetectedShot(
    val x: Float,
    val y: Float,
    val rings: Int,
    val isPatch: Boolean,
    val confidence: Float
)

// ────────────────────────────────────────────────
// SCHIESSBEDINGUNGEN
// ────────────────────────────────────────────────

data class ShootingConditions(
    val weather: WeatherCondition = WeatherCondition.INDOOR,
    val wind: WindCondition = WindCondition.NONE,
    val light: LightCondition = LightCondition.ARTIFICIAL,
    val temperature: Int? = null,
    val fatigue: FatigueLevel = FatigueLevel.NORMAL,
    val stress: StressLevel = StressLevel.NORMAL,
    val equipment: String = ""
)

enum class WeatherCondition(val emoji: String, val label: String) {
    INDOOR("🏛️", "Indoor"),
    SUNNY("☀️", "Sonnig"),
    CLOUDY("☁️", "Bewölkt"),
    WINDY("💨", "Windig"),
    RAIN("🌧️", "Regen"),
    HOT("🌡️", "Sehr heiß")
}

enum class WindCondition(val label: String) {
    NONE("Kein Wind"),
    LIGHT("Leicht (1-3 m/s)"),
    MODERATE("Mittel (4-6 m/s)"),
    STRONG("Stark (7+ m/s)"),
    GUSTY("Böig")
}

enum class LightCondition(val label: String) {
    ARTIFICIAL("Künstlich"),
    NATURAL("Tageslicht"),
    MIXED("Gemischt"),
    DIM("Gedämpft")
}

enum class FatigueLevel(val label: String, val emoji: String) {
    FRESH("Ausgeruht", "😊"),
    NORMAL("Normal", "😐"),
    TIRED("Müde", "😴"),
    EXHAUSTED("Erschöpft", "😩")
}

enum class StressLevel(val label: String) {
    RELAXED("Entspannt"),
    NORMAL("Normal"),
    SLIGHTLY("Leicht angespannt"),
    HIGH("Angespannt")
}

// ────────────────────────────────────────────────
// TRAININGSZIELE
// ────────────────────────────────────────────────

data class TrainingGoal(
    val id: Long = 0,
    val disciplineId: Long,
    val disciplineName: String = "",
    val targetRings: Int,
    val deadline: LocalDate?,
    val description: String,
    val isAchieved: Boolean = false,
    val achievedDate: LocalDate? = null
)

// ────────────────────────────────────────────────
// ACHIEVEMENTS
// ────────────────────────────────────────────────

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val unlockedAt: LocalDateTime? = null
) {
    val isUnlocked get() = unlockedAt != null
}

object Achievements {
    val ALL = listOf(
        Achievement("first_session",    "Erstes Training",    "Erste Trainingseinheit",              "🎯"),
        Achievement("ten_sessions",     "10 Einheiten",       "10 Trainingseinheiten",               "🏅"),
        Achievement("hundred_sessions", "100 Einheiten",      "100 Trainingseinheiten",              "🏆"),
        Achievement("perfect_series",   "Perfekte Serie",     "Alle Schüsse in den 10er",            "⭐"),
        Achievement("new_pb",           "Neuer Rekord",       "Persönlichen Beststand verbessert",   "🎖️"),
        Achievement("streak_7",         "Wochenserie",        "7 Tage hintereinander trainiert",     "🔥"),
        Achievement("ai_user",          "KI-Schütze",         "Erste KI-Scheibenanalyse",            "🤖"),
        Achievement("competition",      "Wettkampf",          "Erstes Wettkampfergebnis",            "🥇"),
        Achievement("first_discipline", "Eigene Disziplin",   "Erste eigene Disziplin angelegt",     "📋")
    )
}

// ────────────────────────────────────────────────
// STATISTIKEN
// ────────────────────────────────────────────────

enum class Trend(val label: String, val emoji: String) {
    IMPROVING("Verbesserung", "📈"),
    STABLE("Stabil", "➡️"),
    DECLINING("Rückgang", "📉"),
    INSUFFICIENT_DATA("Zu wenig Daten", "❓")
}
