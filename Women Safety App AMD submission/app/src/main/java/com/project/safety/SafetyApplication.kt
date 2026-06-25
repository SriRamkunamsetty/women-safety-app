package com.project.safety

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.project.safety.utils.Constants

class SafetyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // SOS Emergency Channel
            val sosChannel = NotificationChannel(
                Constants.SOS_CHANNEL_ID,
                "SOS Emergency",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency SOS alerts and notifications"
                setSound(null, null)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // Location Tracking Channel
            val locationChannel = NotificationChannel(
                Constants.LOCATION_CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background location tracking notifications"
                setSound(null, null)
            }

            // General Notifications Channel
            val generalChannel = NotificationChannel(
                Constants.GENERAL_CHANNEL_ID,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannels(
                listOf(sosChannel, locationChannel, generalChannel)
            )
        }
    }
}
