package com.project.safety

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.storage.FirebaseStorage
import com.project.safety.databinding.ActivityVoiceMessageBinding
import com.project.safety.utils.Constants
import com.project.safety.utils.SharedPrefManager
import java.io.File
import java.io.IOException

class VoiceMessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoiceMessageBinding
    private lateinit var sharedPref: SharedPrefManager
    private lateinit var storage: FirebaseStorage
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var recordingTime = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isEmergencyMode = false
    
    // Location data
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentAddress: String? = null
    private var locationLink: String? = null
    
    // Auto-stop timer configuration (in seconds)
    private val AUTO_STOP_DURATION = 15
    private var autoStopEnabled = true

    private val PERMISSION_REQUEST_CODE = 200
    private val SMS_PERMISSION_CODE = 201

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = SharedPrefManager.getInstance(this)
        storage = FirebaseStorage.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geocoder = Geocoder(this)
        
        // Check if launched in emergency mode
        isEmergencyMode = intent.getBooleanExtra("EMERGENCY_MODE", false)
        
        // Fetch location immediately
        fetchCurrentLocation()

        setupClickListeners()
        checkPermissions()
        
        // Auto-start recording in emergency mode
        if (isEmergencyMode) {
            binding.tvTitle.text = "EMERGENCY Voice Alert"
            binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.sos_red))
            binding.tvDescription.text = "Recording will start automatically and send to emergency contacts"
            
            // Start recording after a short delay to ensure permissions
            handler.postDelayed({
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                    == PackageManager.PERMISSION_GRANTED) {
                    startRecording()
                }
            }, 500)
        }
    }

    private fun setupClickListeners() {
        binding.btnRecord.setOnClickListener {
            if (!isRecording) {
                startRecording()
            } else {
                stopRecording()
            }
        }

        binding.btnSendToContacts.setOnClickListener {
            sendToEmergencyContacts()
        }

        binding.btnCancel.setOnClickListener {
            cancelRecording()
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                Constants.LOCATION_REQUEST_CODE
            )
            binding.tvLocationStatus.text = "📍 Location permission required"
            return
        }

        binding.tvLocationStatus.text = "📍 Fetching location..."
        
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLatitude = it.latitude
                    currentLongitude = it.longitude
                    locationLink = "https://maps.google.com/?q=${it.latitude},${it.longitude}"
                    
                    // Get address from coordinates
                    try {
                        val addresses: List<Address>? = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            currentAddress = buildString {
                                if (address.featureName != null) append("${address.featureName}, ")
                                if (address.locality != null) append("${address.locality}, ")
                                if (address.adminArea != null) append("${address.adminArea}, ")
                                if (address.countryName != null) append(address.countryName)
                            }
                        } else {
                            currentAddress = "Lat: ${String.format("%.6f", it.latitude)}, Lng: ${String.format("%.6f", it.longitude)}"
                        }
                    } catch (e: Exception) {
                        currentAddress = "Lat: ${String.format("%.6f", it.latitude)}, Lng: ${String.format("%.6f", it.longitude)}"
                        e.printStackTrace()
                    }
                    
                    binding.tvLocationStatus.text = "📍 $currentAddress"
                    binding.tvStatus.text = "Tap to start recording"
                    Toast.makeText(this, "Location ready", Toast.LENGTH_SHORT).show()
                } ?: run {
                    binding.tvLocationStatus.text = "📍 Location unavailable - Enable GPS"
                    binding.tvStatus.text = "Tap to start recording"
                    Toast.makeText(this, "Unable to get location. Recording will proceed without location.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                binding.tvLocationStatus.text = "📍 Location unavailable"
                binding.tvStatus.text = "Tap to start recording"
                Toast.makeText(this, "Location error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
            checkPermissions()
            return
        }

        try {
            // Create audio file
            audioFile = File(externalCacheDir, "emergency_voice_${System.currentTimeMillis()}.mp3")

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            recordingTime = 0
            binding.btnRecord.text = "STOP RECORDING"
            binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, R.color.sos_red)
            binding.tvStatus.text = "Recording..."
            binding.btnSendToContacts.isEnabled = false

            // Start timer
            startTimer()

            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Failed to start recording: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            binding.btnRecord.text = "START RECORDING"
            binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, R.color.safe_green)
            binding.tvStatus.text = "Recording saved (${recordingTime}s)"
            binding.btnSendToContacts.isEnabled = true

            handler.removeCallbacksAndMessages(null)

            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
            
            // Auto-send in emergency mode
            if (isEmergencyMode) {
                binding.tvStatus.text = "Sending to emergency contacts..."
                handler.postDelayed({
                    sendToEmergencyContacts()
                }, 1000)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error stopping recording: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun startTimer() {
        handler.post(object : Runnable {
            override fun run() {
                if (isRecording) {
                    recordingTime++
                    binding.tvTimer.text = formatTime(recordingTime)
                    
                    // Auto-stop after configured duration
                    if (autoStopEnabled && recordingTime >= AUTO_STOP_DURATION) {
                        Toast.makeText(this@VoiceMessageActivity, "Auto-stopping recording", Toast.LENGTH_SHORT).show()
                        stopRecording()
                        return
                    }
                    
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    private fun sendToEmergencyContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_CODE)
            return
        }
        if (audioFile == null || !audioFile!!.exists()) {
            Toast.makeText(this, "No recording found", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnSendToContacts.isEnabled = false

        try {
            // Verify file exists and has content
            if (audioFile!!.length() == 0L) {
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnSendToContacts.isEnabled = true
                Toast.makeText(this, "Recording file is empty", Toast.LENGTH_SHORT).show()
                return
            }

            // Upload to Firebase Storage with improved error handling
            val timestamp = System.currentTimeMillis()
            val fileName = "emergency_voice_${timestamp}.mp3"
            
            try {
                // Get storage reference - use default bucket
                val storageRef = storage.reference
                val audioRef = storageRef.child("emergency_voice/$fileName")
                val fileUri = Uri.fromFile(audioFile)
                
                android.util.Log.d("VoiceUpload", "Starting upload: ${audioFile!!.absolutePath}")
                android.util.Log.d("VoiceUpload", "File size: ${audioFile!!.length()} bytes")
                android.util.Log.d("VoiceUpload", "Storage path: emergency_voice/$fileName")
                
                audioRef.putFile(fileUri)
                    .addOnProgressListener { taskSnapshot ->
                        val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                        binding.tvStatus.text = "Uploading... $progress%"
                        android.util.Log.d("VoiceUpload", "Upload progress: $progress%")
                    }
                    .addOnSuccessListener { taskSnapshot ->
                        android.util.Log.d("VoiceUpload", "Upload successful, getting download URL")
                        audioRef.downloadUrl.addOnSuccessListener { uri ->
                            val audioUrl = uri.toString()
                            android.util.Log.d("VoiceUpload", "Download URL: $audioUrl")
                            sendVoiceMessageToContacts(audioUrl)
                        }.addOnFailureListener { e ->
                            binding.progressBar.visibility = android.view.View.GONE
                            binding.btnSendToContacts.isEnabled = true
                            android.util.Log.e("VoiceUpload", "Failed to get download URL", e)
                            Toast.makeText(this, "Failed to get download URL: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.btnSendToContacts.isEnabled = true
                        android.util.Log.e("VoiceUpload", "Upload failed", e)
                        
                        val errorMsg = when {
                            e.message?.contains("does not exist") == true -> 
                                "Firebase Storage not configured. Please check Firebase console and ensure Storage is enabled."
                            e.message?.contains("permission") == true -> 
                                "Permission denied. Please check Firebase Storage rules."
                            e.message?.contains("network") == true -> 
                                "Network error. Please check your internet connection."
                            else -> "Upload failed: ${e.message}"
                        }
                        
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                    }
            } catch (e: Exception) {
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnSendToContacts.isEnabled = true
                android.util.Log.e("VoiceUpload", "Exception during upload", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnSendToContacts.isEnabled = true
            Toast.makeText(this, "Failed to upload: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendVoiceMessageToContacts(audioUrl: String) {
        val contacts = sharedPref.getEmergencyContacts()

        if (contacts.isEmpty()) {
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnSendToContacts.isEnabled = true
            Toast.makeText(this, "No emergency contacts found. Please add emergency contacts first.", Toast.LENGTH_LONG).show()
            return
        }

        // Create comprehensive emergency message with location and timestamp
        val timestamp = System.currentTimeMillis()
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault())
        val formattedTime = dateFormat.format(java.util.Date(timestamp))
        
        val message = buildString {
            append("🚨 EMERGENCY VOICE ALERT 🚨\n\n")
            append("🎙️ Voice Recording:\n$audioUrl\n\n")
            
            // Add location information if available
            if (locationLink != null) {
                append("📍 Location:\n")
                if (currentAddress != null) {
                    append("$currentAddress\n")
                }
                append("Google Maps: $locationLink\n\n")
            } else {
                append("📍 Location: Unavailable\n\n")
            }
            
            append("🕐 Time: $formattedTime\n\n")
            append("Sent from Women Safety App")
        }

        // Send via SMS to all emergency contacts using SmsManager
        try {
            val smsManager = android.telephony.SmsManager.getDefault()
            var successCount = 0
            var failCount = 0

            for (contact in contacts) {
                try {
                    // Split message if too long
                    val parts = smsManager.divideMessage(message)
                    smsManager.sendMultipartTextMessage(
                        contact.number,
                        null,
                        parts,
                        null,
                        null
                    )
                    successCount++
                } catch (e: Exception) {
                    failCount++
                    e.printStackTrace()
                }
            }

            binding.progressBar.visibility = android.view.View.GONE
            binding.btnSendToContacts.isEnabled = true

            if (successCount > 0) {
                Toast.makeText(
                    this,
                    "✅ Voice message sent to $successCount contact(s)" + 
                    if (failCount > 0) "\n⚠️ Failed to send to $failCount contact(s)" else "",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this, "❌ Failed to send messages. Please check SMS permissions.", Toast.LENGTH_LONG).show()
            }
            
            finish()
        } catch (e: Exception) {
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnSendToContacts.isEnabled = true
            Toast.makeText(this, "Error sending SMS: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun cancelRecording() {
        if (isRecording) {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            handler.removeCallbacksAndMessages(null)
        }

        audioFile?.delete()
        audioFile = null

        Toast.makeText(this, "Recording cancelled", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendToEmergencyContacts()
            } else {
                Toast.makeText(this, "SMS permission required to send alert", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            mediaRecorder?.apply {
                stop()
                release()
            }
        }
        handler.removeCallbacksAndMessages(null)
    }
}
