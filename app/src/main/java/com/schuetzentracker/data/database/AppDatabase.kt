package com.schuetzentracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.schuetzentracker.data.database.dao.*
import com.schuetzentracker.data.database.entities.*

@Database(
    entities = [
        DisciplineEntity::class,
        TrainingSessionEntity::class,
        SeriesEntity::class,
        ShotEntity::class,
        TrainingGoalEntity::class,
        AchievementEntity::class,
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun disciplineDao(): DisciplineDao
    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun seriesDao(): SeriesDao
    abstract fun shotDao(): ShotDao
    abstract fun trainingGoalDao(): TrainingGoalDao
    abstract fun achievementDao(): AchievementDao
    abstract fun statisticsDao(): StatisticsDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun create(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "schuetzen_tracker.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
