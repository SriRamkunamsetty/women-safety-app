package com.project.safety.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.project.safety.models.EmergencyContact
import com.project.safety.models.UserProfile

class SharedPrefManager private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: SharedPrefManager? = null

        fun getInstance(context: Context): SharedPrefManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SharedPrefManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // ==================== AUTHENTICATION ====================
    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_IS_LOGGED_IN, false)
    }

    fun saveAuthToken(token: String) {
        sharedPreferences.edit().putString(Constants.KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString(Constants.KEY_AUTH_TOKEN, null)
    }

    fun saveFirebaseUserId(userId: String) {
        sharedPreferences.edit().putString(Constants.KEY_FIREBASE_USER_ID, userId).apply()
    }

    fun getFirebaseUserId(): String? {
        return sharedPreferences.getString(Constants.KEY_FIREBASE_USER_ID, null)
    }
    
    fun saveUserEmail(email: String) {
        sharedPreferences.edit().putString("KEY_USER_EMAIL", email).apply()
    }
    
    fun getUserEmail(): String? {
        return sharedPreferences.getString("KEY_USER_EMAIL", null)
    }

    // ==================== USER PROFILE ====================
    fun saveUserProfile(profile: UserProfile) {
        val json = gson.toJson(profile)
        sharedPreferences.edit().putString(Constants.KEY_USER_PROFILE, json).apply()
    }

    fun getUserProfile(): UserProfile? {
        val json = sharedPreferences.getString(Constants.KEY_USER_PROFILE, null)
        return if (json != null) gson.fromJson(json, UserProfile::class.java) else null
    }

    // ==================== EMERGENCY CONTACTS ====================
    fun saveEmergencyContacts(contacts: List<EmergencyContact>) {
        val json = gson.toJson(contacts)
        sharedPreferences.edit().putString(Constants.KEY_EMERGENCY_CONTACTS, json).apply()
    }

    fun getEmergencyContacts(): List<EmergencyContact> {
        val json = sharedPreferences.getString(Constants.KEY_EMERGENCY_CONTACTS, null)
        return if (json != null) {
            val type = object : TypeToken<List<EmergencyContact>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    // ==================== SOS & EMERGENCY ====================
    fun setSOSActive(isActive: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_SOS_ACTIVE, isActive).apply()
    }

    fun isSOSActive(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_SOS_ACTIVE, false)
    }

    fun setCurrentEmergencyId(id: String) {
        sharedPreferences.edit().putString(Constants.KEY_CURRENT_EMERGENCY_ID, id).apply()
    }

    fun getCurrentEmergencyId(): String? {
        return sharedPreferences.getString(Constants.KEY_CURRENT_EMERGENCY_ID, null)
    }

    fun setEmergencyTriggerCount(count: Int) {
        sharedPreferences.edit().putInt(Constants.KEY_EMERGENCY_TRIGGER_COUNT, count).apply()
    }

    fun getEmergencyTriggerCount(): Int {
        return sharedPreferences.getInt(Constants.KEY_EMERGENCY_TRIGGER_COUNT, 0)
    }

    // ==================== EVIDENCE & RECORDING ====================
    fun setRecordingPath(path: String) {
        sharedPreferences.edit().putString(Constants.KEY_RECORDING_PATH, path).apply()
    }

    fun getRecordingPath(): String? {
        return sharedPreferences.getString(Constants.KEY_RECORDING_PATH, null)
    }

    fun setLastRecordingTime(time: Long) {
        sharedPreferences.edit().putLong(Constants.KEY_LAST_RECORDING_TIME, time).apply()
    }

    fun getLastRecordingTime(): Long {
        return sharedPreferences.getLong(Constants.KEY_LAST_RECORDING_TIME, 0)
    }

    // ==================== LOCATION TRACKING ====================
    fun setLastKnownLocation(location: String) {
        sharedPreferences.edit().putString(Constants.KEY_LAST_KNOWN_LOCATION, location).apply()
    }

    fun getLastKnownLocation(): String? {
        return sharedPreferences.getString(Constants.KEY_LAST_KNOWN_LOCATION, null)
    }

    fun setTrackingStartTime(time: Long) {
        sharedPreferences.edit().putLong(Constants.KEY_TRACKING_START_TIME, time).apply()
    }

    fun getTrackingStartTime(): Long {
        return sharedPreferences.getLong(Constants.KEY_TRACKING_START_TIME, 0)
    }

    fun saveRouteHistory(date: String, route: MutableList<HashMap<String, Any>>) {
        val json = gson.toJson(route)
        sharedPreferences.edit().putString("${Constants.KEY_ROUTE_HISTORY_PREFIX}$date", json).apply()
    }

    fun getRouteHistory(date: String): MutableList<HashMap<String, Any>>? {
        val json = sharedPreferences.getString("${Constants.KEY_ROUTE_HISTORY_PREFIX}$date", null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<HashMap<String, Any>>>() {}.type
            gson.fromJson(json, type)
        } else {
            null
        }
    }

    // ==================== APP SETTINGS ====================
    fun setShakeSOSEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_SHAKE_SOS_ENABLED, enabled).apply()
    }

    fun isShakeSOSEnabled(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_SHAKE_SOS_ENABLED, true)
    }

    fun setPowerButtonSOSEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_POWER_BUTTON_SOS_ENABLED, enabled).apply()
    }

    fun isPowerButtonSOSEnabled(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_POWER_BUTTON_SOS_ENABLED, true)
    }

    fun setAutoRecordingEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_AUTO_RECORDING_ENABLED, enabled).apply()
    }

    fun isAutoRecordingEnabled(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_AUTO_RECORDING_ENABLED, true)
    }

    fun setLocationSharingEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_LOCATION_SHARING_ENABLED, enabled).apply()
    }

    fun isLocationSharingEnabled(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_LOCATION_SHARING_ENABLED, true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_BIOMETRIC_ENABLED, false)
    }

    fun setLowBatteryAlertEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_LOW_BATTERY_ALERT_ENABLED, enabled).apply()
    }

    fun isLowBatteryAlertEnabled(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_LOW_BATTERY_ALERT_ENABLED, true)
    }

    fun setOfflineModeEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_OFFLINE_MODE_ENABLED, enabled).apply()
    }

    fun isOfflineModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_OFFLINE_MODE_ENABLED, true)
    }

    fun setDisguiseModeEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_DISGUISE_MODE_ENABLED, enabled).apply()
    }

    fun isDisguiseModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_DISGUISE_MODE_ENABLED, false)
    }

    fun setDisguisePin(pin: String) {
        sharedPreferences.edit().putString(Constants.KEY_DISGUISE_PIN, pin).apply()
    }

    fun getDisguisePin(): String? {
        return sharedPreferences.getString(Constants.KEY_DISGUISE_PIN, "9110") // Default PIN
    }

    // ==================== APP CONFIGURATION ====================
    fun setFirstLaunch(isFirstLaunch: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_FIRST_LAUNCH, isFirstLaunch).apply()
    }

    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_FIRST_LAUNCH, true)
    }

    fun setAppLanguage(language: String) {
        sharedPreferences.edit().putString(Constants.KEY_APP_LANGUAGE, language).apply()
    }

    fun getAppLanguage(): String {
        return sharedPreferences.getString(Constants.KEY_APP_LANGUAGE, "en") ?: "en"
    }

    fun setThemeMode(mode: String) {
        sharedPreferences.edit().putString(Constants.KEY_THEME_MODE, mode).apply()
    }

    fun getThemeMode(): String {
        return sharedPreferences.getString(Constants.KEY_THEME_MODE, "light") ?: "light"
    }

    // ==================== NOTIFICATIONS ====================
    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun isNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_VIBRATION_ENABLED, enabled).apply()
    }

    fun isVibrationEnabled(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_VIBRATION_ENABLED, true)
    }

    fun setSoundEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_SOUND_ENABLED, enabled).apply()
    }

    fun isSoundEnabled(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_SOUND_ENABLED, true)
    }

    // ==================== DATA MANAGEMENT ====================
    fun setLastSyncTime(time: Long) {
        sharedPreferences.edit().putLong(Constants.KEY_LAST_SYNC_TIME, time).apply()
    }

    fun getLastSyncTime(): Long {
        return sharedPreferences.getLong(Constants.KEY_LAST_SYNC_TIME, 0)
    }

    fun setDataBackupEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_DATA_BACKUP_ENABLED, enabled).apply()
    }

    fun isDataBackupEnabled(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_DATA_BACKUP_ENABLED, true)
    }

    // ==================== MISCELLANEOUS ====================
    fun setAppVersion(version: String) {
        sharedPreferences.edit().putString(Constants.KEY_APP_VERSION, version).apply()
    }

    fun getAppVersion(): String? {
        return sharedPreferences.getString(Constants.KEY_APP_VERSION, null)
    }

    fun setLastCrashReportTime(time: Long) {
        sharedPreferences.edit().putLong(Constants.KEY_LAST_CRASH_REPORT_TIME, time).apply()
    }

    fun getLastCrashReportTime(): Long {
        return sharedPreferences.getLong(Constants.KEY_LAST_CRASH_REPORT_TIME, 0)
    }

    // ==================== UTILITY METHODS ====================
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    fun clearUserData() {
        with(sharedPreferences.edit()) {
            remove(Constants.KEY_USER_PROFILE)
            remove(Constants.KEY_AUTH_TOKEN)
            remove(Constants.KEY_FIREBASE_USER_ID)
            remove(Constants.KEY_IS_LOGGED_IN)
            remove(Constants.KEY_EMERGENCY_CONTACTS)
            apply()
        }
    }

    fun clearEmergencyData() {
        with(sharedPreferences.edit()) {
            remove(Constants.KEY_SOS_ACTIVE)
            remove(Constants.KEY_CURRENT_EMERGENCY_ID)
            remove(Constants.KEY_RECORDING_PATH)
            remove(Constants.KEY_LAST_RECORDING_TIME)
            remove(Constants.KEY_EMERGENCY_TRIGGER_COUNT)
            apply()
        }
    }

    fun clearLocationData() {
        with(sharedPreferences.edit()) {
            remove(Constants.KEY_LAST_KNOWN_LOCATION)
            remove(Constants.KEY_TRACKING_START_TIME)
            // Clear all route history
            val allEntries = sharedPreferences.all
            for ((key, _) in allEntries) {
                if (key.startsWith(Constants.KEY_ROUTE_HISTORY_PREFIX)) {
                    remove(key)
                }
            }
            apply()
        }
    }
}


