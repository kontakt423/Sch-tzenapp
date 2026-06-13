package com.schuetzentracker.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.schuetzentracker.SchutzenTrackerApp
import com.schuetzentracker.ui.main.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

// ────────────────────────────────────────────────
// TRAINING REMINDER WORKER
// ────────────────────────────────────────────────

@HiltWorker
class TrainingReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "Zeit zu trainieren! 🎯"
        val message = inputData.getString(KEY_MESSAGE) ?: "Dein Schiesstraining wartet."
        showNotification(title, message)
        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat
            .Builder(applicationContext, SchutzenTrackerApp.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        applicationContext
            .getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
        const val NOTIFICATION_ID = 1001
        const val TAG = "training_reminder"
    }
}

// ────────────────────────────────────────────────
// REMINDER MANAGER
// ────────────────────────────────────────────────

class ReminderManager(private val workManager: WorkManager) {

    fun scheduleDailyReminder(hourOfDay: Int) {
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            if (before(now)) add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        val delay = target.timeInMillis - now.timeInMillis

        val data = Data.Builder()
            .putString(TrainingReminderWorker.KEY_TITLE, "🎯 Trainingszeit!")
            .putString(TrainingReminderWorker.KEY_MESSAGE, "Vergiss dein tägliches Schiesstraining nicht!")
            .build()

        val request = PeriodicWorkRequestBuilder<TrainingReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(TrainingReminderWorker.TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "daily_reminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleOneTimeReminder(hours: Long, message: String) {
        val data = Data.Builder()
            .putString(TrainingReminderWorker.KEY_TITLE, "🎯 Training-Erinnerung")
            .putString(TrainingReminderWorker.KEY_MESSAGE, message)
            .build()

        workManager.enqueue(
            OneTimeWorkRequestBuilder<TrainingReminderWorker>()
                .setInitialDelay(hours, TimeUnit.HOURS)
                .setInputData(data)
                .build()
        )
    }

    fun cancelAll() {
        workManager.cancelAllWorkByTag(TrainingReminderWorker.TAG)
    }
}
