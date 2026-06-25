package com.project.safety

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.project.safety.databinding.ActivityProfileSetupBinding
import com.project.safety.models.User
import com.project.safety.models.UserProfile
import com.project.safety.utils.Constants
import com.project.safety.utils.SharedPrefManager

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPref: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()
        sharedPref = SharedPrefManager.getInstance(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        binding.btnSkip.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun saveProfile() {
        val bloodGroup = binding.spBloodGroup.text.toString().trim().ifEmpty { "Not Specified" }
        val city = binding.etCity.text.toString().trim()
        val medicalConditions = binding.etMedicalConditions.text.toString().trim()

        if (city.isEmpty()) {
            Toast.makeText(this, "Please enter your city", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE

        val user = auth.currentUser
        user?.let {
            val medicalList = if (medicalConditions.isNotEmpty()) {
                medicalConditions.split(",").map { it.trim() }
            } else {
                emptyList()
            }

            val userData = hashMapOf(
                "bloodGroup" to bloodGroup,
                "city" to city,
                "medicalConditions" to medicalList,
                "profileCompleted" to true
            )

            db.collection(Constants.COLLECTION_USERS)
                .document(it.uid)
                .update(userData as Map<String, Any>)
                .addOnSuccessListener {
                    binding.progressBar.visibility = android.view.View.GONE

                    // Save to shared preferences
                    val profile = com.project.safety.models.UserProfile(
                        name = user.displayName ?: "",
                        email = user.email ?: "",
                        phone = user.phoneNumber ?: "",
                        bloodGroup = bloodGroup,
                        city = city,
                        medicalConditions = medicalList
                    )
                    sharedPref.saveUserProfile(profile)

                    Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Failed to save profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
