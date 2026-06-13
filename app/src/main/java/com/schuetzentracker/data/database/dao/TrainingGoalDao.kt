package com.schuetzentracker.data.database.dao

import androidx.room.*
import com.schuetzentracker.data.database.entities.TrainingGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingGoalDao {

    @Query("SELECT * FROM training_goals ORDER BY isAchieved ASC, deadlineTimestamp ASC")
    fun getAll(): Flow<List<TrainingGoalEntity>>

    @Query("SELECT * FROM training_goals WHERE disciplineId = :disciplineId AND isAchieved = 0")
    fun getActiveForDiscipline(disciplineId: Long): Flow<List<TrainingGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: TrainingGoalEntity): Long

    @Update
    suspend fun update(goal: TrainingGoalEntity)

    @Query("UPDATE training_goals SET isAchieved = 1, achievedTimestamp = :timestamp WHERE id = :id")
    suspend fun markAchieved(id: Long, timestamp: Long)

    @Delete
    suspend fun delete(goal: TrainingGoalEntity)
}
