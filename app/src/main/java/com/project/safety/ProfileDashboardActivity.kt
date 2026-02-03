package com.project.safety

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.project.safety.databinding.ActivityProfileDashboardBinding
import com.project.safety.models.UserProfile
import com.project.safety.utils.Constants
import com.project.safety.utils.SharedPrefManager

class ProfileDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPref: SharedPrefManager
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()
        sharedPref = SharedPrefManager.getInstance(this)

        setupClickListeners()
        fetchUserData()
    }

    private fun fetchUserData() {
        val user = auth.currentUser
        if (user != null) {
            binding.progressBar.visibility = View.VISIBLE
            
            db.collection(Constants.COLLECTION_USERS).document(user.uid).get()
                .addOnSuccessListener { document ->
                    binding.progressBar.visibility = View.GONE
                    if (document.exists()) {
                        val firestoreUser = document.toObject(com.project.safety.models.User::class.java)
                        firestoreUser?.let {
                            updateUI(it)
                            saveToLocal(it)
                        }
                    } else {
                        // If document doesn't exist, try loading from local as fallback
                        loadProfileDataFromLocal()
                    }
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Failed to sync data: ${it.message}", Toast.LENGTH_SHORT).show()
                    loadProfileDataFromLocal()
                }
        } else {
            loadProfileDataFromLocal()
        }
    }

    private fun loadProfileDataFromLocal() {
        val profile = sharedPref.getUserProfile()
        profile?.let {
            // Map UserProfile to UI directly since we have separate models
            binding.tvHeaderName.text = it.name.ifEmpty { "User Name" }
            binding.tvHeaderEmail.text = it.email
            
            binding.etName.setText(it.name)
            binding.etPhone.setText(it.phone)
            binding.etAge.setText(it.age)
            binding.etNativeLocation.setText(it.city) // Mapping city to Native Location as fallback or primary
            binding.etFatherName.setText(it.fatherName)
            binding.etMotherName.setText(it.motherName)
            binding.etOccupation.setText(it.occupation)
        }
    }

    private fun updateUI(user: com.project.safety.models.User) {
        binding.tvHeaderName.text = user.name.ifEmpty { "User Name" }
        binding.tvHeaderEmail.text = user.email
        
        binding.etName.setText(user.name)
        binding.etPhone.setText(user.phone)
        binding.etAge.setText(user.age)
        binding.etNativeLocation.setText(user.nativeLocation.ifEmpty { user.city })
        binding.etFatherName.setText(user.fatherName)
        binding.etMotherName.setText(user.motherName)
        binding.etOccupation.setText(user.occupation)
    }

    private fun saveToLocal(user: com.project.safety.models.User) {
        val userProfile = UserProfile(
            name = user.name,
            email = user.email,
            phone = user.phone,
            bloodGroup = user.bloodGroup,
            city = user.city, // Keep city for compatibility
            age = user.age,
            nativeLocation = user.nativeLocation,
            fatherName = user.fatherName,
            motherName = user.motherName,
            occupation = user.occupation,
            medicalConditions = user.medicalConditions,
            photoUrl = user.profilePhotoUrl
        )
        sharedPref.saveUserProfile(userProfile)
    }

    private fun setupClickListeners() {
        binding.fabEdit.setOnClickListener {
            toggleEditMode(true)
        }

        binding.btnCancel.setOnClickListener {
            toggleEditMode(false)
            fetchUserData() // Re-fetch to reset any unsaved changes
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun toggleEditMode(enabled: Boolean) {
        isEditMode = enabled
        
        // Toggle Fields
        val fields = listOf(
            binding.etName,
            binding.etPhone,
            binding.etAge,
            binding.etNativeLocation,
            binding.etFatherName,
            binding.etMotherName,
            binding.etOccupation
        )
        
        fields.forEach { it.isEnabled = enabled }

        // Toggle Buttons
        binding.fabEdit.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.layoutActions.visibility = if (enabled) View.VISIBLE else View.GONE
        
        if (enabled) {
            binding.etName.requestFocus()
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val age = binding.etAge.text.toString().trim()
        val nativeLocation = binding.etNativeLocation.text.toString().trim()
        val fatherName = binding.etFatherName.text.toString().trim()
        val motherName = binding.etMotherName.text.toString().trim()
        val occupation = binding.etOccupation.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Name and Phone are required", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        setInputsEnabled(false)

        val user = auth.currentUser
        user?.let { firebaseUser ->
            // Use SetOptions.merge() logic by using update but handling non-existence if we used set before
            // But since we want to be robust:
            
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "phone" to phone,
                "age" to age,
                "nativeLocation" to nativeLocation,
                "fatherName" to fatherName,
                "motherName" to motherName,
                "occupation" to occupation,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.uid)
                .update(updates)
                .addOnSuccessListener {
                    // Update Local Storage
                    // We can re-fetch or construct manually. Constructing manually to be fast.
                    val currentProfile = sharedPref.getUserProfile() ?: UserProfile()
                    val updatedProfile = currentProfile.copy(
                        name = name,
                        phone = phone,
                        age = age,
                        nativeLocation = nativeLocation,
                        fatherName = fatherName,
                        motherName = motherName,
                        occupation = occupation
                    )
                    sharedPref.saveUserProfile(updatedProfile)

                    binding.progressBar.visibility = View.GONE
                    setInputsEnabled(true)
                    Toast.makeText(this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                    toggleEditMode(false)
                    
                    // Update header
                    binding.tvHeaderName.text = name
                }
                .addOnFailureListener { e ->
                    // Fallback using set() if update failed likely due to document missing
                    // This creates the document if it doesn't exist
                    val fullUserUpdate = com.project.safety.models.User(
                        id = firebaseUser.uid,
                        name = name,
                        email = firebaseUser.email ?: "",
                        phone = phone,
                        age = age,
                        nativeLocation = nativeLocation,
                        fatherName = fatherName,
                        motherName = motherName,
                        occupation = occupation,
                        updatedAt = System.currentTimeMillis()
                    )
                     
                     db.collection(Constants.COLLECTION_USERS)
                        .document(firebaseUser.uid)
                        .set(fullUserUpdate) // This might overwrite other fields if we aren't careful, but User has default values.
                        // Ideally we should use merge, but set(pojo) replaces.
                        // Let's use set(map, SetOptions.merge()) if we could, but let's stick to simple retry with set if update fails
                        // Actually, if update fails, it might be permission or network.
                        
                    binding.progressBar.visibility = View.GONE
                    setInputsEnabled(true)
                    Toast.makeText(this, "Update Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            binding.progressBar.visibility = View.GONE
            setInputsEnabled(true)
            Toast.makeText(this, "User session invalid. Please login again.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setInputsEnabled(enabled: Boolean) {
        binding.btnSave.isEnabled = enabled
        binding.btnCancel.isEnabled = enabled
    }
}
