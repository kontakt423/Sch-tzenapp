package com.schuetzentracker.data.database.entities

import androidx.room.Embedded
import androidx.room.Relation

// ────────────────────────────────────────────────
// ROOM RELATIONS
//
// Diese Klassen müssen in einer EIGENEN Datei stehen
// (getrennt von @Entity-Klassen), damit Room KSP 2.x
// sie korrekt verarbeiten kann.
// ────────────────────────────────────────────────

data class SessionWithSeries(
    @Embedded
    val session: TrainingSessionEntity,

    @Relation(
        entity = SeriesEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val series: List<SeriesWithShots>
)

data class SeriesWithShots(
    @Embedded
    val series: SeriesEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "seriesId"
    )
    val shots: List<ShotEntity>
)
