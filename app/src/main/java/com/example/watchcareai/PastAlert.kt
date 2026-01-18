package com.example.watchcareai

data class PastAlert(
    val id: String,
    val location: String,
    val claimedBy: String,
    val timestamp: Long,
    val isClaimedByMe: Boolean
)
