package com.project.safety

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.project.safety.databinding.ActivitySettingsBinding
import com.project.safety.utils.Constants
import com.project.safety.utils.SharedPrefManager
import java.util.concurrent.Executor

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPref: SharedPrefManager
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = SharedPrefManager.getInstance(this)
        executor = ContextCompat.getMainExecutor(this)

        setupBiometricPrompt()
        loadSettings()
        setupClickListeners()
    }

    private fun setupBiometricPrompt() {
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@SettingsActivity,
                        "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    sharedPref.setBiometricEnabled(true)
                    binding.swBiometric.isChecked = true
                    Toast.makeText(this@SettingsActivity,
                        "Biometric authentication enabled", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@SettingsActivity,
                        "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadSettings() {
        binding.swSosShake.isChecked = sharedPref.isShakeSOSEnabled()
        binding.swSosPowerButton.isChecked = sharedPref.isPowerButtonSOSEnabled()
        binding.swAutoRecording.isChecked = sharedPref.isAutoRecordingEnabled()
        binding.swLocationSharing.isChecked = sharedPref.isLocationSharingEnabled()
        binding.swBiometric.isChecked = sharedPref.isBiometricEnabled()
        binding.swLowBatteryAlert.isChecked = sharedPref.isLowBatteryAlertEnabled()
        binding.swOfflineMode.isChecked = sharedPref.isOfflineModeEnabled()
        binding.swDisguiseMode.isChecked = sharedPref.isDisguiseModeEnabled()
    }

    private fun setupClickListeners() {
        binding.swSosShake.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.setShakeSOSEnabled(isChecked)
            if (isChecked) {
                checkSensorPermission()
            }
        }

        binding.swSosPowerButton.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.setPowerButtonSOSEnabled(isChecked)
        }

        binding.swAutoRecording.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.setAutoRecordingEnabled(isChecked)
            if (isChecked) {
                checkAudioPermission()
            }
        }

        binding.swLocationSharing.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.setLocationSharingEnabled(isChecked)
            if (isChecked) {
                checkLocationPermission()
            }
        }

        binding.swBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showBiometricPrompt()
            } else {
                sharedPref.setBiometricEnabled(false)
            }
        }

        binding.swLowBatteryAlert.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.setLowBatteryAlertEnabled(isChecked)
        }

        binding.swOfflineMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.setOfflineModeEnabled(isChecked)
        }

        binding.swDisguiseMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.setDisguiseModeEnabled(isChecked)
            if (isChecked) {
                showDisguiseModeDialog()
            }
        }

        binding.btnPermissions.setOnClickListener {
            openAppSettings()
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            openPrivacyPolicy()
        }

        binding.btnTerms.setOnClickListener {
            openTerms()
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }

        binding.btnDeleteAccount.setOnClickListener {
            deleteAccount()
        }
    }

    private fun checkSensorPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.BODY_SENSORS),
                1001
            )
        }
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1002
            )
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1003
            )
        }
    }

    private fun showBiometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable Biometric Login")
            .setSubtitle("Use your fingerprint or face to login")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showDisguiseModeDialog() {
        AlertDialog.Builder(this)
            .setTitle("Disguise Mode")
            .setMessage("App will appear as Calculator. To access safety features, enter PIN 9110 in calculator.")
            .setPositiveButton("OK") { _, _ ->
                // Save PIN
                sharedPref.setDisguisePin("9110")
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.swDisguiseMode.isChecked = false
            }
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    private fun openPrivacyPolicy() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(Constants.PRIVACY_POLICY_URL)
        startActivity(intent)
    }

    private fun openTerms() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(Constants.TERMS_URL)
        startActivity(intent)
    }

    private fun logout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                sharedPref.clearAll()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("This will permanently delete your account and all data. This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                // Delete from Firebase and clear local data
                sharedPref.clearAll()
                Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                1001 -> binding.swSosShake.isChecked = false
                1002 -> binding.swAutoRecording.isChecked = false
                1003 -> binding.swLocationSharing.isChecked = false
            }
        }
    }
}
