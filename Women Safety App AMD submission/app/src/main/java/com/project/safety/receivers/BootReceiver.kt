package com.project.safety.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.project.safety.services.ShakeDetectionService
import com.project.safety.utils.SharedPrefManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val sharedPref = SharedPrefManager.getInstance(context)

            // Restart shake detection service if enabled
            if (sharedPref.isShakeSOSEnabled()) {
                val shakeServiceIntent = Intent(context, ShakeDetectionService::class.java)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(shakeServiceIntent)
                } else {
                    context.startService(shakeServiceIntent)
                }
            }

            // Restart location tracking if SOS was active
            if (sharedPref.isSOSActive()) {
                // Restart SOS service
                val sosServiceIntent = Intent(context,
                    Class.forName("project.safety.services.SOSService"))
                sosServiceIntent.action = "action_start_sos"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(sosServiceIntent)
                } else {
                    context.startService(sosServiceIntent)
                }
            }
        }
    }
}
