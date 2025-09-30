package com.example.asistenciaqr.data.model

import com.google.firebase.Timestamp

enum class AttendanceType {
    ENTRY,
    EXIT,
}

data class AttendanceRecord(
    val id: String = "",
    val userId: String = "",
    val userNames: String = "",
    val userLastnames: String = "",
    val type: AttendanceType = AttendanceType.ENTRY,
    val timestamp: Timestamp = Timestamp.now(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val qrData: String = ""
) {
    fun getFormattedDate(): String {
        val date = timestamp.toDate()
        return android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", date).toString()
    }

    fun getTypeInSpanish(): String {
        return when (type) {
            AttendanceType.ENTRY -> "Entrada"
            AttendanceType.EXIT -> "Salida"
        }
    }
}