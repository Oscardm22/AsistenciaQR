package com.example.asistenciaqr.data.model

import java.util.*

data class AttendanceRecord(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val userNames: String = "",
    val userLastnames: String = "",
    val type: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)