package com.project.safety.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.project.safety.MainActivity
import com.project.safety.R
import com.project.safety.utils.Constants
import com.project.safety.utils.SharedPrefManager
import java.text.SimpleDateFormat
import java.util.*

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var notificationManager: NotificationManager
    private lateinit var sharedPref: SharedPrefManager
    private lateinit var db: FirebaseFirestore

    private var isTracking = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPref = SharedPrefManager.getInstance(this)
        db = FirebaseFirestore.getInstance()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        setupLocationRequest()
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.ACTION_START_TRACKING -> startTracking()
            Constants.ACTION_STOP_TRACKING -> stopTracking()
        }
        return START_STICKY
    }

    private fun setupLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = Constants.LOCATION_UPDATE_INTERVAL
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            maxWaitTime = 10000
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    updateLocation(location)
                }
            }
        }
    }

    private fun startTracking() {
        if (isTracking) return

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            isTracking = true
            startForegroundService()

            // Save tracking start time
            sharedPref.setTrackingStartTime(System.currentTimeMillis())
        }
    }

    private fun stopTracking() {
        if (!isTracking) return

        fusedLocationClient.removeLocationUpdates(locationCallback)
        isTracking = false

        stopForeground(true)
        stopSelf()
    }

    private fun updateLocation(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        val timestamp = System.currentTimeMillis()

        // Update notification
        updateLocationNotification(latitude, longitude)

        // Save to local storage (SharedPreferences)
        sharedPref.setLastKnownLocation("$latitude,$longitude")

        // Save to Firestore if tracking is active
        if (sharedPref.isSOSActive()) {
            saveLocationToFirestore(latitude, longitude, timestamp)
        }

        // Save to route history
        saveToRouteHistory(latitude, longitude, timestamp)
    }

    private fun saveLocationToFirestore(latitude: Double, longitude: Double, timestamp: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val locationData = hashMapOf(
                "latitude" to latitude,
                "longitude" to longitude,
                "timestamp" to timestamp,
                "emergencyId" to sharedPref.getCurrentEmergencyId()
            )

            db.collection("location_updates")
                .add(locationData)
                .addOnSuccessListener {
                    // Location saved successfully
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
        }
    }

    private fun saveToRouteHistory(latitude: Double, longitude: Double, timestamp: Long) {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val today = dateFormat.format(Date())

        val routePoint = hashMapOf<String, Any>(
            "lat" to latitude,
            "lng" to longitude,
            "time" to SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        )

        val routeHistory = sharedPref.getRouteHistory(today) ?: mutableListOf()
        routeHistory.add(routePoint)

        // Keep only last 100 points
        if (routeHistory.size > 100) {
            routeHistory.removeAt(0)
        }

        sharedPref.saveRouteHistory(today, routeHistory)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.LOCATION_CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background location tracking for safety"
                setSound(null, null)
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

        val notification = NotificationCompat.Builder(this, Constants.LOCATION_CHANNEL_ID)
            .setContentTitle("Location Tracking Active")
            .setContentText("Your location is being tracked for safety")
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(Constants.LOCATION_NOTIFICATION_ID, notification)
    }

    private fun updateLocationNotification(latitude: Double, longitude: Double) {
        val notification = NotificationCompat.Builder(this, Constants.LOCATION_CHANNEL_ID)
            .setContentTitle("Live Location")
            .setContentText("Lat: ${"%.6f".format(latitude)}, Lng: ${"%.6f".format(longitude)}")
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(Constants.LOCATION_NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTracking()
        super.onDestroy()
    }
}
