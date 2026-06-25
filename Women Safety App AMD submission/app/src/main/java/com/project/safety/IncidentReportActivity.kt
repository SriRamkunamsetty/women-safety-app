package com.project.safety

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.project.safety.databinding.ActivityIncidentReportBinding
import com.project.safety.utils.Constants
import com.project.safety.utils.SharedPrefManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class IncidentReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncidentReportBinding
    private lateinit var sharedPref: SharedPrefManager
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var selectedImageUri: Uri? = null
    private var selectedAudioUri: Uri? = null
    private var selectedVideoUri: Uri? = null
    private var currentPhotoUri: Uri? = null

    private val CAMERA_REQUEST = 1001
    private val GALLERY_REQUEST = 1002
    private val AUDIO_REQUEST = 1003
    private val VIDEO_REQUEST = 1004

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncidentReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = SharedPrefManager.getInstance(this)
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupSpinner()
        setupClickListeners()
    }

    private fun setupSpinner() {
        val incidentTypes = resources.getStringArray(R.array.incident_types)
        val adapter = android.widget.ArrayAdapter(
            this,
            R.layout.spinner_item,
            incidentTypes
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spIncidentType.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnTakePhoto.setOnClickListener {
            openCamera()
        }

        binding.btnSelectPhoto.setOnClickListener {
            openGallery()
        }

        binding.btnRecordAudio.setOnClickListener {
            recordAudio()
        }

        binding.btnRecordVideo.setOnClickListener {
            recordVideo()
        }

        binding.btnSubmitReport.setOnClickListener {
            submitIncidentReport()
        }

        binding.swAnonymous.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutPersonalInfo.visibility = if (isChecked) android.view.View.GONE else android.view.View.VISIBLE
        }
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQUEST
            )
        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val photoFile = createImageFile()
            val photoURI = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                photoFile
            )
            currentPhotoUri = photoURI
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(intent, CAMERA_REQUEST)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST)
    }

    private fun recordAudio() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_REQUEST
            )
        } else {
            val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
            startActivityForResult(intent, AUDIO_REQUEST)
        }
    }

    private fun recordVideo() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                VIDEO_REQUEST
            )
        } else {
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60) // 1 minute max
            startActivityForResult(intent, VIDEO_REQUEST)
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun submitIncidentReport() {
        val incidentType = binding.spIncidentType.selectedItem.toString()
        val location = binding.etLocation.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val isAnonymous = binding.swAnonymous.isChecked

        if (location.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill location and description", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnSubmitReport.isEnabled = false

        // Get current location
        val lastLocation = sharedPref.getLastKnownLocation()
        val latitude = lastLocation?.split(",")?.get(0)?.toDoubleOrNull()
        val longitude = lastLocation?.split(",")?.get(1)?.toDoubleOrNull()

        val incidentData = hashMapOf<String, Any>(
            "type" to incidentType,
            "location" to location,
            "description" to description,
            "latitude" to (latitude ?: ""),
            "longitude" to (longitude ?: ""),
            "anonymous" to isAnonymous,
            "timestamp" to System.currentTimeMillis(),
            "status" to "reported"
        )

        // Add reporter info if not anonymous
        if (!isAnonymous) {
            val reporterName = binding.etReporterName.text.toString().trim()
            val reporterPhone = binding.etReporterPhone.text.toString().trim()

            incidentData["reporterName"] = reporterName
            incidentData["reporterPhone"] = reporterPhone
        }

        // Upload evidence files
        val uploadTasks = mutableListOf<com.google.android.gms.tasks.Task<Uri>>()

        selectedImageUri?.let { uri ->
            val imageRef = storage.reference.child("incidents/images/${System.currentTimeMillis()}.jpg")
            uploadTasks.add(imageRef.putFile(uri).continueWithTask { task ->
                if (task.isSuccessful) {
                    task.result?.storage?.downloadUrl
                } else {
                    throw task.exception ?: Exception("Upload failed")
                }
            })
        }

        selectedAudioUri?.let { uri ->
            val audioRef = storage.reference.child("incidents/audio/${System.currentTimeMillis()}.mp3")
            uploadTasks.add(audioRef.putFile(uri).continueWithTask { task ->
                if (task.isSuccessful) {
                    task.result?.storage?.downloadUrl
                } else {
                    throw task.exception ?: Exception("Upload failed")
                }
            })
        }

        selectedVideoUri?.let { uri ->
            val videoRef = storage.reference.child("incidents/video/${System.currentTimeMillis()}.mp4")
            uploadTasks.add(videoRef.putFile(uri).continueWithTask { task ->
                if (task.isSuccessful) {
                    task.result?.storage?.downloadUrl
                } else {
                    throw task.exception ?: Exception("Upload failed")
                }
            })
        }

        if (uploadTasks.isNotEmpty()) {
            Toast.makeText(this, "Uploading evidence files...", Toast.LENGTH_SHORT).show()
            com.google.android.gms.tasks.Tasks.whenAllSuccess<Uri>(uploadTasks)
                .addOnSuccessListener { uris ->
                    val evidenceUrls = ArrayList(uris.map { it.toString() })
                    incidentData["evidenceUrls"] = evidenceUrls

                    saveIncidentToFirestore(incidentData)
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnSubmitReport.isEnabled = true
                    Toast.makeText(this, "Failed to upload evidence: ${e.message}\n\nSubmitting without evidence...", Toast.LENGTH_LONG).show()
                    // Try to submit without evidence
                    saveIncidentToFirestore(incidentData)
                }
        } else {
            saveIncidentToFirestore(incidentData)
        }
    }

    private fun saveIncidentToFirestore(incidentData: HashMap<String, Any>) {
        db.collection(Constants.COLLECTION_INCIDENTS)
            .add(incidentData)
            .addOnSuccessListener {
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnSubmitReport.isEnabled = true
                Toast.makeText(this, "Incident reported successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnSubmitReport.isEnabled = true
                Toast.makeText(this, "Failed to report incident: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST -> {
                    currentPhotoUri?.let { uri ->
                        selectedImageUri = uri
                        binding.ivPreview.setImageURI(uri)
                    }
                }
                GALLERY_REQUEST -> {
                    data?.data?.let { uri ->
                        selectedImageUri = uri
                        binding.ivPreview.setImageURI(uri)
                    }
                }
                AUDIO_REQUEST -> {
                    data?.data?.let { uri ->
                        selectedAudioUri = uri
                        binding.tvAudioStatus.text = "Audio recorded ✓"
                    }
                }
                VIDEO_REQUEST -> {
                    data?.data?.let { uri ->
                        selectedVideoUri = uri
                        binding.tvVideoStatus.text = "Video recorded ✓"
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                CAMERA_REQUEST -> openCamera()
                AUDIO_REQUEST -> recordAudio()
                VIDEO_REQUEST -> recordVideo()
            }
        }
    }
}
