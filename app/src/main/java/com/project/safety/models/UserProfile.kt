package com.project.safety.models

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val bloodGroup: String = "",
    val city: String = "",
    val age: String = "",
    val nativeLocation: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val occupation: String = "",
    val medicalConditions: List<String> = emptyList(),
    val photoUrl: String? = null,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
