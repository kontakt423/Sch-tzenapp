package com.schuetzentracker.data.database.dao

import androidx.room.*
import com.schuetzentracker.data.database.entities.ShotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShotDao {

    @Query("SELECT * FROM shots WHERE seriesId = :seriesId ORDER BY number ASC")
    fun getForSeries(seriesId: Long): Flow<List<ShotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shot: ShotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shots: List<ShotEntity>)
}
