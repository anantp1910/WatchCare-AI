package com.example.watchcareai

data class Alert(
    val id: String = "",
    val location: String = "",
    val severity: Double = 0.0,
    val status: String = "active",
    val createdAtMillis: Long = 0L
)