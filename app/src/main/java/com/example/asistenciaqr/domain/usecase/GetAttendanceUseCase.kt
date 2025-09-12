package com.example.asistenciaqr.domain.usecase

import com.example.asistenciaqr.data.model.AttendanceRecord
import com.example.asistenciaqr.domain.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAttendanceUseCase(private val attendanceRepository: AttendanceRepository) {
    suspend fun execute(userId: String): Result<List<AttendanceRecord>> {
        return withContext(Dispatchers.IO) {
            attendanceRepository.getAttendanceByUser(userId)
        }
    }
}