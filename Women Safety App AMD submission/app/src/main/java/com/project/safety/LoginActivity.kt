package com.project.safety

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.project.safety.databinding.ActivityLoginBinding
import com.project.safety.utils.SharedPrefManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import android.text.InputType
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPref: SharedPrefManager
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        sharedPref = SharedPrefManager.getInstance(this)
        
        setupBiometric()
        setupClickListeners()

        // Check if user is already logged in
        if (sharedPref.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Demo mode: Use demo@test.com / demo123 for offline testing
            if (email == "demo@test.com" && password == "demo123") {
                loginDemoUser()
            } else {
                loginUser(email, password)
            }
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnPhoneLogin.setOnClickListener {
            startActivity(Intent(this, PhoneLoginActivity::class.java))
        }

        binding.btnBiometricLogin.setOnClickListener {
            startBiometricLogin()
        }
        
        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun loginUser(email: String, password: String) {
        binding.progressBar.visibility = android.view.View.VISIBLE

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = android.view.View.GONE

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        sharedPref.setLoggedIn(true)
                        sharedPref.saveAuthToken(user.uid)
                        sharedPref.saveUserEmail(email) // Save email for biometric login

                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                } else {
                    val errorMsg = task.exception?.message ?: "Unknown error"
                    Toast.makeText(
                        this,
                        "Login failed: $errorMsg\n\nTry demo login:\nEmail: demo@test.com\nPassword: demo123",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    
    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@LoginActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Auto-login with saved credentials
                    val savedEmail = sharedPref.getUserEmail()
                    if (savedEmail != null) {
                        sharedPref.setLoggedIn(true)
                        Toast.makeText(this@LoginActivity, "Biometric login successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "No saved credentials found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@LoginActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()
    }

    private fun startBiometricLogin() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                biometricPrompt.authenticate(promptInfo)
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                Toast.makeText(this, "No biometric hardware available", Toast.LENGTH_SHORT).show()
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                Toast.makeText(this, "Biometric hardware unavailable", Toast.LENGTH_SHORT).show()
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                Toast.makeText(this, "No biometric credentials enrolled", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showForgotPasswordDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        val input = android.widget.EditText(this)
        input.hint = "Enter your email"
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        
        builder.setTitle("Reset Password")
            .setMessage("Enter your email to receive password reset link")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty()) {
                    sendPasswordResetEmail(email)
                } else {
                    Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun loginDemoUser() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        // Simulate network delay
        binding.root.postDelayed({
            binding.progressBar.visibility = android.view.View.GONE
            
            // Save demo user session
            sharedPref.setLoggedIn(true)
            sharedPref.saveAuthToken("demo_user_123")
            sharedPref.saveUserEmail("demo@test.com")
            
            Toast.makeText(this, "Demo Login Successful!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1000)
    }
}
