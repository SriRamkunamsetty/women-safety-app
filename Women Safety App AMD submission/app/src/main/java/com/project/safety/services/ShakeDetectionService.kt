package com.project.safety.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.Vibrator
import com.project.safety.utils.Constants
import com.project.safety.utils.SharedPrefManager

class ShakeDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var vibrator: Vibrator
    private lateinit var sharedPref: SharedPrefManager

    private var lastShakeTime: Long = 0
    private var shakeCount = 0

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        sharedPref = SharedPrefManager.getInstance(this)

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            val acceleration = Math.sqrt((x * x + y * y + z * z).toDouble()) - SensorManager.GRAVITY_EARTH
            val currentTime = System.currentTimeMillis()

            if (acceleration > 12) { // Shake threshold
                if (currentTime - lastShakeTime > 1000) { // 1 second gap
                    shakeCount = 1
                } else {
                    shakeCount++
                }

                lastShakeTime = currentTime

                if (shakeCount >= Constants.SHAKE_THRESHOLD) {
                    triggerSOS()
                    shakeCount = 0
                }
            }
        }
    }

    private fun triggerSOS() {
        // Vibrate to confirm
        vibrator.vibrate(500)

        // Start SOS Service
        val intent = Intent(this, SOSService::class.java)
        intent.action = Constants.ACTION_START_SOS

        startService(intent)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }
}
