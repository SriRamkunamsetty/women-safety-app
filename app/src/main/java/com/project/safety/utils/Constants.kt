package com.project.safety.utils

object Constants {
    // Shared Preferences
    const val PREF_NAME = "SafetyAppPrefs"
    const val KEY_USER_PROFILE = "user_profile"
    const val KEY_EMERGENCY_CONTACTS = "emergency_contacts"
    const val KEY_SOS_ACTIVE = "sos_active"
    const val KEY_CURRENT_EMERGENCY_ID = "current_emergency_id"
    const val KEY_RECORDING_PATH = "recording_path"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_FIREBASE_USER_ID = "firebase_user_id"
    const val KEY_EMERGENCY_TRIGGER_COUNT = "emergency_trigger_count"
    const val KEY_LAST_RECORDING_TIME = "last_recording_time"
    const val KEY_LAST_KNOWN_LOCATION = "last_known_location"
    const val KEY_TRACKING_START_TIME = "tracking_start_time"
    const val KEY_ROUTE_HISTORY_PREFIX = "route_history_"
    const val KEY_SHAKE_SOS_ENABLED = "shake_sos_enabled"
    const val KEY_POWER_BUTTON_SOS_ENABLED = "power_button_sos_enabled"
    const val KEY_AUTO_RECORDING_ENABLED = "auto_recording_enabled"
    const val KEY_LOCATION_SHARING_ENABLED = "location_sharing_enabled"
    const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    const val KEY_LOW_BATTERY_ALERT_ENABLED = "low_battery_alert_enabled"
    const val KEY_OFFLINE_MODE_ENABLED = "offline_mode_enabled"
    const val KEY_DISGUISE_MODE_ENABLED = "disguise_mode_enabled"
    const val KEY_DISGUISE_PIN = "disguise_pin"
    const val KEY_FIRST_LAUNCH = "first_launch"
    const val KEY_APP_LANGUAGE = "app_language"
    const val KEY_THEME_MODE = "theme_mode"
    const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    const val KEY_SOUND_ENABLED = "sound_enabled"
    const val KEY_LAST_SYNC_TIME = "last_sync_time"
    const val KEY_DATA_BACKUP_ENABLED = "data_backup_enabled"
    const val KEY_APP_VERSION = "app_version"
    const val KEY_LAST_CRASH_REPORT_TIME = "last_crash_report_time"

    // Request Codes
    const val PERMISSION_REQUEST_CODE = 100
    const val LOCATION_REQUEST_CODE = 101
    const val CAMERA_REQUEST_CODE = 102
    const val AUDIO_REQUEST_CODE = 103

    // Notification
    const val SOS_CHANNEL_ID = "sos_channel"
    const val SOS_NOTIFICATION_ID = 1
    const val LOCATION_CHANNEL_ID = "location_channel"
    const val LOCATION_NOTIFICATION_ID = 2
    const val GENERAL_CHANNEL_ID = "general_channel"
    const val GENERAL_NOTIFICATION_ID = 3

    // Service Actions
    const val ACTION_START_SOS = "action_start_sos"
    const val ACTION_STOP_SOS = "action_stop_sos"
    const val ACTION_START_TRACKING = "action_start_tracking"
    const val ACTION_STOP_TRACKING = "action_stop_tracking"

    // Firebase Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_EMERGENCIES = "emergencies"
    const val COLLECTION_INCIDENTS = "incidents"
    const val COLLECTION_EVIDENCES = "evidences"

    // API Keys (Should be in local.properties in production)
    const val MAPS_API_KEY = "YOUR_GOOGLE_MAPS_API_KEY"

    // Emergency Numbers (India)
    const val POLICE_NUMBER = "100"
    const val AMBULANCE_NUMBER = "102"
    const val WOMEN_HELPLINE = "181"
    const val NATIONAL_EMERGENCY = "112"

    // Time Intervals
    const val LOCATION_UPDATE_INTERVAL = 10000L // 10 seconds
    const val SOS_DURATION = 300000L // 5 minutes
    const val SHAKE_THRESHOLD = 3 // Number of shakes to trigger SOS

    // URLs
    const val PRIVACY_POLICY_URL = "https://yoursafetyapp.com/privacy"
    const val TERMS_URL = "https://yoursafetyapp.com/terms"
}
