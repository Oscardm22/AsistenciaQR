package com.example.asistenciaqr.domain.usecase

import com.example.asistenciaqr.data.model.AttendanceRecord
import com.example.asistenciaqr.domain.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RegisterAttendanceUseCase(private val attendanceRepository: AttendanceRepository) {
    suspend fun execute(record: AttendanceRecord): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            attendanceRepository.registerAttendance(record)
        }
    }
}