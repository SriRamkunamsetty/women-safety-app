package com.project.safety.models

data class Incident(
    val id: String = "",
    val type: String = "",
    val location: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val evidenceUrls: List<String> = emptyList(),
    val status: String = "reported",
    val reporterName: String? = null,
    val reporterPhone: String? = null,
    val isAnonymous: Boolean = false
)
