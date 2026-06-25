package com.project.safety.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class User(
    @PropertyName("id")
    val id: String = "",

    @PropertyName("name")
    val name: String = "",

    @PropertyName("email")
    val email: String = "",

    @PropertyName("phone")
    val phone: String = "",

    @PropertyName("bloodGroup")
    val bloodGroup: String = "",

    @PropertyName("city")
    val city: String = "",

    @PropertyName("age")
    val age: String = "",

    @PropertyName("nativeLocation")
    val nativeLocation: String = "",

    @PropertyName("fatherName")
    val fatherName: String = "",

    @PropertyName("motherName")
    val motherName: String = "",

    @PropertyName("occupation")
    val occupation: String = "",

    @PropertyName("medicalConditions")
    val medicalConditions: List<String> = emptyList(),

    @PropertyName("emergencyContacts")
    val emergencyContacts: List<EmergencyContact> = emptyList(),

    @PropertyName("profilePhotoUrl")
    val profilePhotoUrl: String? = null,

    @PropertyName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @PropertyName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),

    @PropertyName("isVerified")
    val isVerified: Boolean = false,

    @PropertyName("lastLocation")
    val lastLocation: String? = null,

    @PropertyName("sosCount")
    val sosCount: Int = 0
)
