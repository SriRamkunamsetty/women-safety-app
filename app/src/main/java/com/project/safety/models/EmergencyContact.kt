package com.project.safety.models

data class EmergencyContact(
    var name: String = "",
    var number: String = "",
    var relation: String = "",
    var isPrimary: Boolean = false
)
