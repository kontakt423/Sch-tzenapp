package com.schuetzentracker.data.database.dao

import androidx.room.*
import com.schuetzentracker.data.database.entities.DisciplineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DisciplineDao {

    @Query("SELECT * FROM disciplines ORDER BY sortOrder ASC, name ASC")
    fun getAll(): Flow<List<DisciplineEntity>>

    @Query("SELECT * FROM disciplines WHERE id = :id")
    suspend fun getById(id: Long): DisciplineEntity?

    @Query("SELECT * FROM disciplines WHERE isCompetitionCapable = 1 ORDER BY name ASC")
    fun getCompetitionCapable(): Flow<List<DisciplineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(discipline: DisciplineEntity): Long

    @Update
    suspend fun update(discipline: DisciplineEntity)

    @Delete
    suspend fun delete(discipline: DisciplineEntity)

    @Query("DELETE FROM disciplines WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM disciplines")
    suspend fun count(): Int
}
