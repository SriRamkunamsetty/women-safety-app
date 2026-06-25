package com.project.safety

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.project.safety.databinding.ActivityLiveLocationBinding
import com.project.safety.services.LocationTrackingService
import com.project.safety.utils.Constants
import com.project.safety.utils.SharedPrefManager

class LiveLocationActivity : AppCompatActivity(), OnMapReadyCallback {


    private val SMS_REQUEST_CODE = 101
    private lateinit var binding: ActivityLiveLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sharedPref: SharedPrefManager
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPref = SharedPrefManager.getInstance(this)
        db = FirebaseFirestore.getInstance()

        setupMap()
        setupClickListeners()
        checkLocationPermission()
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            getCurrentLocation()
        }
    }

    private fun setupClickListeners() {
        binding.btnStartTracking.setOnClickListener {
            startLocationTracking()
        }

        binding.btnShareLocation.setOnClickListener {
            shareLiveLocation()
        }
        
        binding.btnSafePlaces.setOnClickListener {
            findNearbySafePlaces()
        }
        
        binding.btnOpenMaps.setOnClickListener {
            openInGoogleMaps()
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                Constants.LOCATION_REQUEST_CODE
            )
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        map.clear()
                        map.addMarker(
                            MarkerOptions()
                                .position(currentLatLng)
                                .title("My Current Location")
                        )
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                        // Save current location
                        sharedPref.setLastKnownLocation("${it.latitude},${it.longitude}")
                        
                        // Update status
                        binding.tvLocationStatus.text = "Location: ${String.format("%.6f", it.latitude)}, ${String.format("%.6f", it.longitude)}"
                    } ?: run {
                        Toast.makeText(this, "Unable to get location. Please enable GPS.", Toast.LENGTH_LONG).show()
                        binding.tvLocationStatus.text = "Location unavailable - Enable GPS"
                    }
                }
        }
    }

    private fun shareLiveLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val locationLink = "https://maps.google.com/?q=${it.latitude},${it.longitude}"
                    val timestamp = System.currentTimeMillis()
                    val formattedTime = java.text.DateFormat.getDateTimeInstance().format(timestamp)

                    // Create share message with Google Maps link
                    val message = "🚨 EMERGENCY - My Live Location:\n$locationLink\n\nTime: $formattedTime\n\nFrom Women Safety App"

                    // Save to Firestore for sharing (non-blocking)
                    val locationData = hashMapOf(
                        "latitude" to it.latitude,
                        "longitude" to it.longitude,
                        "timestamp" to timestamp,
                        "shareable" to true
                    )

                    db.collection("location_shares")
                        .add(locationData)
                        .addOnSuccessListener { documentReference ->
                            // Successfully saved to Firestore
                            android.util.Log.d("LiveLocationActivity", "Location saved to Firestore: ${documentReference.id}")
                        }
                        .addOnFailureListener { e ->
                            // Log but don't block sharing
                            android.util.Log.w("LiveLocationActivity", "Failed to save location to Firestore: ${e.message}")
                        }

                    // Try to send SMS to emergency contacts if permission is granted
                    val contacts = sharedPref.getEmergencyContacts()
                    if (contacts.isNotEmpty() && ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            val smsManager = android.telephony.SmsManager.getDefault()
                            var successCount = 0
                            var failCount = 0

                            for (contact in contacts) {
                                try {
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
                                    android.util.Log.e("LiveLocationActivity", "Failed to send SMS to ${contact.number}: ${e.message}")
                                }
                            }

                            if (successCount > 0) {
                                Toast.makeText(
                                    this,
                                    "✅ Location sent via SMS to $successCount contact(s)" +
                                    if (failCount > 0) "\n⚠️ Failed to send to $failCount contact(s)" else "",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("LiveLocationActivity", "Error sending SMS: ${e.message}")
                        }
                    }

                    // Create Intent to share location via any app (WhatsApp, Email, SMS, etc.)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "🚨 Emergency Location Share")
                        putExtra(Intent.EXTRA_TEXT, message)
                    }

                    // Also create a direct Google Maps intent
                    val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse(locationLink))

                    // Create chooser to let user select sharing method
                    val chooserIntent = Intent.createChooser(shareIntent, "Share Location Via")
                    
                    // Add Google Maps as an option
                    val extraIntents = arrayListOf<Intent>()
                    try {
                        mapsIntent.setPackage("com.google.android.apps.maps")
                        extraIntents.add(mapsIntent)
                    } catch (e: Exception) {
                        // Google Maps not available, skip
                    }
                    
                    if (extraIntents.isNotEmpty()) {
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toTypedArray())
                    }

                    try {
                        startActivity(chooserIntent)
                        Toast.makeText(this, "Select an app to share your location", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        // Fallback: try to open Google Maps directly
                        try {
                            startActivity(mapsIntent)
                        } catch (e2: Exception) {
                            // Last resort: copy to clipboard
                            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Location", message)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(this, "Location link copied to clipboard: $locationLink", Toast.LENGTH_LONG).show()
                        }
                    }
                } ?: run {
                    Toast.makeText(this, "Unable to get current location. Please enable GPS and try again.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error getting location: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun findNearbySafePlaces() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    // Search for nearby police stations, hospitals, and safe places
                    val lat = it.latitude
                    val lng = it.longitude
                    
                    // Open Google Maps with search for safe places
                    val uri = Uri.parse("geo:$lat,$lng?q=police+station|hospital|pharmacy")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.setPackage("com.google.android.apps.maps")
                    
                    try {
                        startActivity(intent)
                        Toast.makeText(this, "Showing nearby safe places", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        // If Google Maps not installed, open in browser
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/police+station+hospital/@$lat,$lng,15z")
                        )
                        startActivity(browserIntent)
                    }
                } ?: run {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun startLocationTracking() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
            checkLocationPermission()
            return
        }
        
        // Start continuous location tracking service
        val intent = Intent(this, LocationTrackingService::class.java)
        intent.action = Constants.ACTION_START_TRACKING
        startService(intent)

        binding.tvLocationStatus.text = "Live tracking active..."
        binding.btnStartTracking.isEnabled = false
        Toast.makeText(this, "Live location tracking started", Toast.LENGTH_SHORT).show()
        
        // Get current location immediately
        getCurrentLocation()
    }

    private fun stopLocationTracking() {
        val intent = Intent(this, LocationTrackingService::class.java)
        intent.action = Constants.ACTION_STOP_TRACKING
        stopService(intent)

        Toast.makeText(this, "Location tracking stopped", Toast.LENGTH_SHORT).show()
    }
    
    private fun openInGoogleMaps() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    // Open current location in Google Maps
                    val uri = Uri.parse("geo:${it.latitude},${it.longitude}?z=15")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.setPackage("com.google.android.apps.maps")
                    
                    try {
                        startActivity(intent)
                        Toast.makeText(this, "Opening in Google Maps", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        // If Google Maps not installed, open in browser
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/@${it.latitude},${it.longitude},15z")
                        )
                        startActivity(browserIntent)
                        Toast.makeText(this, "Opening in browser", Toast.LENGTH_SHORT).show()
                    }
                } ?: run {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
                if (::map.isInitialized) {
                    map.isMyLocationEnabled = true
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == SMS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // SMS permission granted, but we can still share via other methods
                shareLiveLocation()
            } else {
                // SMS permission denied, but still allow sharing via other apps
                Toast.makeText(this, "SMS permission denied. You can still share via other apps.", Toast.LENGTH_SHORT).show()
                shareLiveLocation()
            }
        }
    }
}
