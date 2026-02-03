package com.project.safety

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.project.safety.databinding.ActivityPhoneLoginBinding
import java.util.concurrent.TimeUnit

class PhoneLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhoneLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var verificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSendOTP.setOnClickListener {
            val phoneNumber = binding.etPhone.text.toString().trim()

            if (phoneNumber.isEmpty() || phoneNumber.length != 10) {
                Toast.makeText(this, "Enter valid 10-digit phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendOTP("+91$phoneNumber")
        }

        binding.btnVerifyOTP.setOnClickListener {
            val otp = binding.etOTP.text.toString().trim()

            if (otp.isEmpty() || otp.length != 6) {
                Toast.makeText(this, "Enter 6-digit OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyOTP(otp)
        }

        binding.tvResendOTP.setOnClickListener {
            val phoneNumber = binding.etPhone.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                sendOTP("+91$phoneNumber")
            }
        }

        binding.btnBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun sendOTP(phoneNumber: String) {
        binding.progressBar.visibility = android.view.View.VISIBLE

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(
                        this@PhoneLoginActivity,
                        "Verification failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onCodeSent(
                    id: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    binding.progressBar.visibility = android.view.View.GONE
                    verificationId = id
                    resendToken = token

                    binding.etOTPLayout.visibility = android.view.View.VISIBLE
                    binding.btnVerifyOTP.visibility = android.view.View.VISIBLE
                    binding.tvResendOTP.visibility = android.view.View.VISIBLE

                    Toast.makeText(
                        this@PhoneLoginActivity,
                        "OTP sent successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyOTP(otp: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        binding.progressBar.visibility = android.view.View.VISIBLE

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = android.view.View.GONE

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
