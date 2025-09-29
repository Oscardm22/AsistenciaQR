package com.example.asistenciaqr.domain.usecase

import com.example.asistenciaqr.data.model.AttendanceRecord
import com.example.asistenciaqr.domain.repository.AttendanceRepository

class GetAllAttendanceUseCase(
    private val attendanceRepository: AttendanceRepository
) {
    suspend fun execute(): Result<List<AttendanceRecord>> {
        return attendanceRepository.getAllAttendance()
    }
}