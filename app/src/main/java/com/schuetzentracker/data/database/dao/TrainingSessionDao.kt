package com.schuetzentracker.data.database.dao

import androidx.room.*
import com.schuetzentracker.data.database.entities.SessionWithSeries
import com.schuetzentracker.data.database.entities.TrainingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingSessionDao {

    @Transaction
    @Query("SELECT * FROM training_sessions ORDER BY dateTimestamp DESC")
    fun getAllWithSeries(): Flow<List<SessionWithSeries>>

    @Transaction
    @Query("SELECT * FROM training_sessions WHERE disciplineId = :disciplineId ORDER BY dateTimestamp DESC")
    fun getByDiscipline(disciplineId: Long): Flow<List<SessionWithSeries>>

    @Transaction
    @Query("SELECT * FROM training_sessions WHERE mode IN (:modes) ORDER BY dateTimestamp DESC")
    fun getByModes(modes: List<String>): Flow<List<SessionWithSeries>>

    @Transaction
    @Query("SELECT * FROM training_sessions WHERE id = :id")
    suspend fun getById(id: Long): SessionWithSeries?

    @Query("SELECT * FROM training_sessions ORDER BY dateTimestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 5): Flow<List<TrainingSessionEntity>>

    @Query("SELECT MAX(totalRings) FROM training_sessions WHERE disciplineId = :disciplineId")
    fun getPersonalBest(disciplineId: Long): Flow<Int?>

    @Query("SELECT AVG(CAST(totalRings AS FLOAT)) FROM training_sessions WHERE disciplineId = :disciplineId AND dateTimestamp > :since")
    fun getAverageRings(disciplineId: Long, since: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM training_sessions")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM training_sessions WHERE mode IN ('COMPETITION','QUALIFICATION')")
    fun getCompetitionCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: TrainingSessionEntity): Long

    @Update
    suspend fun update(session: TrainingSessionEntity)

    @Query("DELETE FROM training_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
