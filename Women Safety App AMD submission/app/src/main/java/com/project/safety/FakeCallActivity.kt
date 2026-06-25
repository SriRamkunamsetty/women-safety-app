package com.project.safety

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.project.safety.databinding.ActivityFakeCallBinding
import com.project.safety.utils.Constants
import java.util.*

class FakeCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFakeCallBinding
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator
    private lateinit var handler: Handler
    private var isCallActive = false

    private val callers = listOf(
        "Mom" to "Mom",
        "Dad" to "Dad",
        "Police" to "Police",
        "Best Friend" to "Friend",
        "Brother" to "Brother",
        "Sister" to "Sister"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFakeCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        handler = Handler(Looper.getMainLooper())

        setupClickListeners()
        setupSpinner()
    }

    private fun setupSpinner() {
        val callerNames = callers.map { it.first }
        val adapter = android.widget.ArrayAdapter(
            this,
            R.layout.spinner_item,
            callerNames
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spCaller.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnStartFakeCall.setOnClickListener {
            startFakeCall()
        }

        binding.btnStopFakeCall.setOnClickListener {
            stopFakeCall()
        }

        binding.btnAnswerCall.setOnClickListener {
            answerCall()
        }

        binding.btnRejectCall.setOnClickListener {
            rejectCall()
        }

        binding.btnSetTimer.setOnClickListener {
            setCallTimer()
        }
    }

    private fun startFakeCall() {
        if (isCallActive) return

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.VIBRATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.VIBRATE),
                Constants.PERMISSION_REQUEST_CODE
            )
            return
        }

        val selectedCaller = binding.spCaller.selectedItem as String
        val callerType = callers.find { it.first == selectedCaller }?.second ?: "Unknown"

        binding.tvCallerName.text = selectedCaller
        binding.tvCallerType.text = "Incoming call - $callerType"

        // Start ringtone
        playRingtone()

        // Start vibration
        startVibration()

        // Show call screen
        binding.layoutCallScreen.visibility = android.view.View.VISIBLE

        isCallActive = true

        // Auto answer after delay if enabled
        if (binding.swAutoAnswer.isChecked) {
            handler.postDelayed({
                answerCall()
            }, 3000)
        }

        // Auto end after timer if set
        val timerMinutes = binding.etTimer.text.toString().toIntOrNull() ?: 0
        if (timerMinutes > 0) {
            handler.postDelayed({
                stopFakeCall()
            }, (timerMinutes * 60 * 1000).toLong())
        }
    }

    private fun playRingtone() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build()
                )
                setDataSource(this@FakeCallActivity, ringtoneUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error playing ringtone", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(1000, 1000),
                    0
                )
            )
        } else {
            vibrator.vibrate(longArrayOf(1000, 1000), 0)
        }
    }

    private fun answerCall() {
        if (!isCallActive) return

        // Stop ringtone and vibration
        stopRingtone()
        stopVibration()

        // Show fake conversation
        binding.layoutCallScreen.visibility = android.view.View.GONE
        binding.layoutConversation.visibility = android.view.View.VISIBLE

        // Start fake conversation
        startFakeConversation()
    }

    private fun rejectCall() {
        stopFakeCall()
        Toast.makeText(this, "Call rejected", Toast.LENGTH_SHORT).show()
    }

    private fun startFakeConversation() {
        val messages = listOf(
            "Hello? Are you there?",
            "Where are you? I'm waiting.",
            "Can you come pick me up?",
            "I'm in an emergency situation.",
            "Please hurry up!",
            "Okay, I'll be there in 5 minutes.",
            "Thank you! See you soon."
        )

        val conversation = binding.tvConversation
        conversation.text = ""

        var messageIndex = 0
        handler.post(object : Runnable {
            override fun run() {
                if (messageIndex < messages.size) {
                    val currentText = conversation.text.toString()
                    conversation.text = "$currentText\n\n${messages[messageIndex]}"
                    messageIndex++
                    handler.postDelayed(this, 2000)
                }
            }
        })
    }

    private fun stopRingtone() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
    }

    private fun stopVibration() {
        vibrator.cancel()
    }

    private fun stopFakeCall() {
        if (!isCallActive) return

        stopRingtone()
        stopVibration()

        binding.layoutCallScreen.visibility = android.view.View.GONE
        binding.layoutConversation.visibility = android.view.View.GONE

        isCallActive = false

        handler.removeCallbacksAndMessages(null)
    }

    private fun setCallTimer() {
        val timerMinutes = binding.etTimer.text.toString().toIntOrNull() ?: 0
        if (timerMinutes > 0) {
            Toast.makeText(
                this,
                "Call will auto-end in $timerMinutes minutes",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        stopFakeCall()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startFakeCall()
            }
        }
    }
}
