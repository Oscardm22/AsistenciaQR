package com.example.asistenciaqr.domain.repository

import com.example.asistenciaqr.data.model.AttendanceRecord

interface AttendanceRepository {
    suspend fun registerAttendance(record: AttendanceRecord): Result<Boolean>
    suspend fun getAttendanceByUser(userId: String): Result<List<AttendanceRecord>>
    suspend fun getAllAttendance(): Result<List<AttendanceRecord>>
    suspend fun getTodayAttendanceByUser(userId: String): Result<List<AttendanceRecord>>
}