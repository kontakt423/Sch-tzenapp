package com.schuetzentracker.di

import android.content.Context
import androidx.work.WorkManager
import com.schuetzentracker.BuildConfig
import com.schuetzentracker.api.ClaudeVisionService
import com.schuetzentracker.api.GeminiVisionService
import com.schuetzentracker.api.TargetAnalysisService
import com.schuetzentracker.data.database.AppDatabase
import com.schuetzentracker.data.database.dao.*
import com.schuetzentracker.data.repository.UserPreferencesRepository
import com.schuetzentracker.util.BackupService
import com.schuetzentracker.util.PdfExportService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase = AppDatabase.create(ctx)

    @Provides fun provideDisciplineDao(db: AppDatabase): DisciplineDao = db.disciplineDao()
    @Provides fun provideSessionDao(db: AppDatabase): TrainingSessionDao = db.trainingSessionDao()
    @Provides fun provideSeriesDao(db: AppDatabase): SeriesDao = db.seriesDao()
    @Provides fun provideShotDao(db: AppDatabase): ShotDao = db.shotDao()
    @Provides fun provideGoalDao(db: AppDatabase): TrainingGoalDao = db.trainingGoalDao()
    @Provides fun provideAchievementDao(db: AppDatabase): AchievementDao = db.achievementDao()
    @Provides fun provideStatisticsDao(db: AppDatabase): StatisticsDao = db.statisticsDao()

    @Provides @Singleton
    fun provideUserPrefs(@ApplicationContext ctx: Context): UserPreferencesRepository =
        UserPreferencesRepository(ctx)

    @Provides @Singleton
    fun provideTargetAnalysisService(prefsRepo: UserPreferencesRepository): TargetAnalysisService {
        val prefs = runBlocking { prefsRepo.userPreferences.first() }
        val gemini = if (prefs.geminiApiKey.isNotBlank()) GeminiVisionService(prefs.geminiApiKey) else null
        val claudeKey = prefs.claudeApiKey.ifBlank { BuildConfig.ANTHROPIC_API_KEY }
        val claude = if (claudeKey.isNotBlank()) ClaudeVisionService(claudeKey) else null
        return TargetAnalysisService(geminiService = gemini, claudeService = claude, activeProvider = prefs.apiProvider)
    }

    @Provides @Singleton
    fun provideBackupService(@ApplicationContext ctx: Context): BackupService = BackupService(ctx)

    @Provides @Singleton
    fun provideWorkManager(@ApplicationContext ctx: Context): WorkManager = WorkManager.getInstance(ctx)

    @Provides @Singleton
    fun providePdfExportService(@ApplicationContext ctx: Context): PdfExportService = PdfExportService(ctx)
}
