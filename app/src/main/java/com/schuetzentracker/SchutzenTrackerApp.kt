package com.schuetzentracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SchutzenTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Erinnerungs-Kanal
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Training-Erinnerungen",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Erinnerungen für geplante Trainingseinheiten"
                }
            )

            // Achievement-Kanal
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ACHIEVEMENTS,
                    "Achievements",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Benachrichtigungen für freigeschaltete Achievements"
                }
            )
        }
    }

    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_ACHIEVEMENTS = "achievements"
    }
}
