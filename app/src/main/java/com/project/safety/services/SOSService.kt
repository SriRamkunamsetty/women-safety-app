package com.project.safety.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.project.safety.MainActivity
import com.project.safety.R
import com.project.safety.utils.Constants
import com.project.safety.utils.SharedPrefManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SOSService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sharedPref: SharedPrefManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var db: FirebaseFirestore
    private var isRecording = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPref = SharedPrefManager.getInstance(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        db = FirebaseFirestore.getInstance()
        createNotificationChannel()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.ACTION_START_SOS -> {
                startSOSProcedure()
            }
            Constants.ACTION_STOP_SOS -> {
                stopSOSProcedure()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startSOSProcedure() {
        sharedPref.setSOSActive(true)

        // 1. Send emergency notifications
        sendEmergencyNotifications()

        // 2. Start location tracking
        startContinuousLocationUpdates()

        // 3. Start audio recording
        startAudioRecording()

        // 4. Make emergency calls
        makeEmergencyCalls()

        // 5. Send SMS to contacts
        sendSMSToContacts()
    }

    private fun sendEmergencyNotifications() {
        CoroutineScope(Dispatchers.IO).launch {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val locationStr = "${it.latitude},${it.longitude}"
                    val timestamp = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
                        .format(Date())

                    // Send to Firestore
                    val emergencyData = hashMapOf(
                        "type" to "SOS",
                        "location" to locationStr,
                        "timestamp" to timestamp,
                        "status" to "active"
                    )

                    db.collection("emergencies")
                        .add(emergencyData)
                        .addOnSuccessListener { documentReference ->
                            sharedPref.setCurrentEmergencyId(documentReference.id)
                        }
                }
            }
        }
    }

    private fun startContinuousLocationUpdates() {
        // Implementation for continuous location updates every 10 seconds
        // Using FusedLocationProviderClient with LocationRequest
    }

    private fun startAudioRecording() {
        try {
            mediaRecorder = MediaRecorder()
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            val fileName = "SOS_${System.currentTimeMillis()}.mp4"
            val filePath = "${externalCacheDir?.absolutePath}/$fileName"
            mediaRecorder.setOutputFile(filePath)

            mediaRecorder.prepare()
            mediaRecorder.start()
            isRecording = true

            sharedPref.setRecordingPath(filePath)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun makeEmergencyCalls() {
        val contacts = sharedPref.getEmergencyContacts()
        if (contacts.isNotEmpty()) {
            val primaryContact = contacts[0]
            // Use ACTION_CALL intent with telephone number
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = android.net.Uri.parse("tel:${primaryContact.number}")
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        // Also call 112 (National Emergency)
        val emergencyIntent = Intent(Intent.ACTION_CALL).apply {
            data = android.net.Uri.parse("tel:112")
        }
        emergencyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(emergencyIntent)
    }

    private fun sendSMSToContacts() {
        val contacts = sharedPref.getEmergencyContacts()
        val smsManager = SmsManager.getDefault()

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val locationLink = "https://maps.google.com/?q=${it.latitude},${it.longitude}"
                val message = """
                    EMERGENCY SOS ALERT!
                    I need immediate help!
                    My location: $locationLink
                    Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}
                    Please help immediately!
                """.trimIndent()

                for (contact in contacts) {
                    try {
                        smsManager.sendTextMessage(
                            contact.number,
                            null,
                            message,
                            null,
                            null
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.SOS_CHANNEL_ID,
                "SOS Emergency",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency SOS alerts"
                setSound(null, null)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, Constants.SOS_CHANNEL_ID)
            .setContentTitle("SOS ACTIVE")
            .setContentText("Emergency mode activated. Help is on the way.")
            .setSmallIcon(R.drawable.ic_sos)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()

        startForeground(Constants.SOS_NOTIFICATION_ID, notification)
    }

    private fun stopSOSProcedure() {
        sharedPref.setSOSActive(false)

        if (isRecording) {
            try {
                mediaRecorder.stop()
                mediaRecorder.release()
                isRecording = false

                // Upload recording to Firebase Storage
                uploadEvidenceToCloud()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        stopForeground(true)
        stopSelf()
    }

    private fun uploadEvidenceToCloud() {
        // Implementation for uploading to Firebase Storage
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopSOSProcedure()
        super.onDestroy()
    }
}
