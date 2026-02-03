package com.project.safety

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.project.safety.databinding.ActivityMainBinding
import com.project.safety.services.SOSService
import com.project.safety.utils.Constants
import com.project.safety.utils.SharedPrefManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPref: SharedPrefManager

    // SOS Triple Tap Variables
    private var sosClickCount = 0
    private var sosLastClickTime = 0L
    private val CLICK_TIME_INTERVAL = 2000L // 2 seconds to complete 3 taps

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = SharedPrefManager.getInstance(this)
        checkPermissions()
        setupClickListeners()
        checkSOSStatus()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CALL_PHONE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                Constants.PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun setupClickListeners() {
        // SOS Button
        // SOS Button
        binding.btnSOS.setOnClickListener {
            handleSOSClick()
            activateSOS() // Keep existing functionality as per plan
        }

        // Profile Dashboard
        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileDashboardActivity::class.java))
        }

        // Live Location
        binding.btnLiveLocation.setOnClickListener {
            startActivity(Intent(this, LiveLocationActivity::class.java))
        }

        // Emergency Contacts
        binding.btnEmergencyContacts.setOnClickListener {
            startActivity(Intent(this, EmergencyContactsActivity::class.java))
        }

        // Fake Call
        binding.btnFakeCall.setOnClickListener {
            startActivity(Intent(this, FakeCallActivity::class.java))
        }

        // Incident Report
        binding.btnIncidentReport.setOnClickListener {
            startActivity(Intent(this, IncidentReportActivity::class.java))
        }

        // Settings
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Safety Tips
        binding.btnSafetyTips.setOnClickListener {
            startActivity(Intent(this, SafetyTipsActivity::class.java))
        }
        
        // Voice Alert - Quick emergency voice message
        binding.btnVoiceAlert.setOnClickListener {
            val intent = Intent(this, VoiceMessageActivity::class.java)
            intent.putExtra("EMERGENCY_MODE", true)
            startActivity(intent)
        }
        
        // Voice Message (Long press SOS for quick access)
        binding.btnSOS.setOnLongClickListener {
            val intent = Intent(this, VoiceMessageActivity::class.java)
            intent.putExtra("EMERGENCY_MODE", true)
            startActivity(intent)
            true
        }
    }


    private fun handleSOSClick() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - sosLastClickTime > CLICK_TIME_INTERVAL) {
            sosClickCount = 0
        }

        sosClickCount++
        sosLastClickTime = currentTime

        if (sosClickCount == 3) {
            sosClickCount = 0 // Reset after successful trigger
            callEmergencyContact()
        }
    }

    private fun callEmergencyContact() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Call permission required for emergency calls", Toast.LENGTH_SHORT).show()
            // Optionally request permission here, but usually best to request in advance
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), Constants.PERMISSION_REQUEST_CODE)
            return
        }

        val contacts = sharedPref.getEmergencyContacts()
        if (contacts.isNotEmpty()) {
            val contact = contacts[0]
            val phoneNumber = contact.number

            if (phoneNumber.isNotEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_CALL)
                    intent.data = android.net.Uri.parse("tel:$phoneNumber")
                    startActivity(intent)
                    Toast.makeText(this, "Calling Emergency Contact: ${contact.name}", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to make call: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                 Toast.makeText(this, "Emergency contact has no phone number", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No emergency contacts found. Please add one.", Toast.LENGTH_LONG).show()
        }
    }

    private fun activateSOS() {
        if (sharedPref.getEmergencyContacts().isEmpty()) {
            Toast.makeText(this, "Please add emergency contacts first", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, SOSService::class.java)
        intent.action = Constants.ACTION_START_SOS

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        Toast.makeText(this, "SOS Activated! Help is on the way.", Toast.LENGTH_LONG).show()
    }

    private fun checkSOSStatus() {
        if (sharedPref.isSOSActive()) {
            binding.tvStatus.text = "SOS ACTIVE"
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.PERMISSION_REQUEST_CODE) {
            val deniedPermissions = mutableListOf<String>()
            for (i in grantResults.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i])
                }
            }

            if (deniedPermissions.isNotEmpty()) {
                Toast.makeText(this,
                    "Some permissions are required for app functionality",
                    Toast.LENGTH_LONG).show()
            }
        }
    }
}
