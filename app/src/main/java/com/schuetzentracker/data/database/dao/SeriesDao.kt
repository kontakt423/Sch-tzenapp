package com.schuetzentracker.data.database.dao

import androidx.room.*
import com.schuetzentracker.data.database.entities.SeriesEntity
import com.schuetzentracker.data.database.entities.SeriesWithShots
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {

    @Transaction
    @Query("SELECT * FROM series WHERE sessionId = :sessionId ORDER BY number ASC")
    fun getForSession(sessionId: Long): Flow<List<SeriesWithShots>>

    @Transaction
    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun getById(id: Long): SeriesWithShots?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(series: SeriesEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(series: List<SeriesEntity>): List<Long>

    @Update
    suspend fun update(series: SeriesEntity)

    @Query("UPDATE series SET analysisJson = :json WHERE id = :id")
    suspend fun updateAnalysis(id: Long, json: String)
}
