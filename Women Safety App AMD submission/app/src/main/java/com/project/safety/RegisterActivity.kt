package com.project.safety

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.project.safety.databinding.ActivityRegisterBinding
import com.project.safety.models.User
import com.project.safety.utils.Constants
import com.project.safety.utils.SharedPrefManager

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPref: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()
        sharedPref = SharedPrefManager.getInstance(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnRegister.isEnabled = false

        // Create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        // Send verification email (non-blocking - don't wait for it)
                        try {
                            firebaseUser.sendEmailVerification()
                                .addOnCompleteListener { verificationTask ->
                                    if (!verificationTask.isSuccessful) {
                                        // Log but don't block registration
                                        android.util.Log.w("RegisterActivity", "Email verification failed: ${verificationTask.exception?.message}")
                                    }
                                }
                                .addOnFailureListener { e ->
                                    // Log but don't block registration
                                    android.util.Log.w("RegisterActivity", "Email verification error: ${e.message}")
                                }
                        } catch (e: Exception) {
                            // If verification fails, continue anyway
                            android.util.Log.w("RegisterActivity", "Could not send verification email: ${e.message}")
                        }
                        
                        // Save user to Firestore immediately (don't wait for email verification)
                        saveUserToFirestore(firebaseUser.uid, name, email, phone)
                    } else {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.btnRegister.isEnabled = true
                        Toast.makeText(this, "Registration Error: User not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnRegister.isEnabled = true
                    
                    // If Firebase is not configured, use demo mode
                    val errorMsg = task.exception?.message ?: ""
                    if (errorMsg.contains("CONFIGURATION") || errorMsg.contains("API key")) {
                        registerDemoUser(name, email, phone)
                    } else {
                        Toast.makeText(
                            this,
                            "Registration failed: $errorMsg",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnRegister.isEnabled = true
                Toast.makeText(
                    this,
                    "Registration failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun saveUserToFirestore(uid: String, name: String, email: String, phone: String) {
        val user = User(
            id = uid,
            name = name,
            email = email,
            phone = phone,
            bloodGroup = "",
            city = "",
            medicalConditions = emptyList(),
            emergencyContacts = emptyList(),
            createdAt = System.currentTimeMillis()
        )

        // Add a safety check - verify user exists after a short delay
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var callbackFired = false
        
        val verifyUserExists = Runnable {
            if (!callbackFired) {
                // Check if user was actually saved
                db.collection(Constants.COLLECTION_USERS)
                    .document(uid)
                    .get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful && task.result.exists() && !callbackFired) {
                            callbackFired = true
                            completeRegistration(uid, email)
                        }
                    }
            }
        }
        
        // Check after 1 second if callback hasn't fired (in case data is saved but callback is delayed)
        handler.postDelayed(verifyUserExists, 1000)
        // Also check after 3 seconds as backup
        handler.postDelayed(verifyUserExists, 3000)

        db.collection(Constants.COLLECTION_USERS)
            .document(uid)
            .set(user)
            .addOnSuccessListener {
                if (!callbackFired) {
                    callbackFired = true
                    handler.removeCallbacks(verifyUserExists)
                    completeRegistration(uid, email)
                }
            }
            .addOnFailureListener { e ->
                if (!callbackFired) {
                    callbackFired = true
                    handler.removeCallbacks(verifyUserExists)
                    
                    runOnUiThread {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.btnRegister.isEnabled = true
                        
                        android.util.Log.e("RegisterActivity", "Firestore save failed: ${e.message}")
                        Toast.makeText(this, "Failed to save profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }
    
    private fun completeRegistration(uid: String, email: String) {
        runOnUiThread {
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnRegister.isEnabled = true

            // Save to shared preferences
            sharedPref.setLoggedIn(true)
            sharedPref.saveAuthToken(uid)
            // Save email for biometric login
            sharedPref.saveUserEmail(email)

            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()

            // Go to profile setup
            startActivity(Intent(this, ProfileSetupActivity::class.java))
            finish()
        }
    }
    
    private fun registerDemoUser(name: String, email: String, phone: String) {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnRegister.isEnabled = false
        
        // Simulate registration delay
        binding.root.postDelayed({
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnRegister.isEnabled = true
            
            // Save demo user
            sharedPref.setLoggedIn(true)
            sharedPref.saveAuthToken("demo_user_${System.currentTimeMillis()}")
            sharedPref.saveUserEmail(email)
            
            Toast.makeText(
                this,
                "Demo Registration Successful!\n\nYou can now login with:\nEmail: demo@test.com\nPassword: demo123",
                Toast.LENGTH_LONG
            ).show()
            
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 1000)
    }
}
