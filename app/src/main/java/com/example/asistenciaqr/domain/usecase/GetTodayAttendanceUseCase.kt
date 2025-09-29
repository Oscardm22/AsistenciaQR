package com.example.asistenciaqr.domain.usecase

import com.example.asistenciaqr.data.model.AttendanceRecord
import com.example.asistenciaqr.domain.repository.AttendanceRepository

class GetTodayAttendanceUseCase(
    private val attendanceRepository: AttendanceRepository
) {
    suspend fun execute(userId: String): Result<List<AttendanceRecord>> {
        return attendanceRepository.getTodayAttendanceByUser(userId)
    }
}