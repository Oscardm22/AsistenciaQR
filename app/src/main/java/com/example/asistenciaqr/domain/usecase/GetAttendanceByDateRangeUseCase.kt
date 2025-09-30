package com.example.asistenciaqr.domain.usecase

import com.example.asistenciaqr.data.model.AttendanceRecord
import com.example.asistenciaqr.domain.repository.AttendanceRepository
import com.google.firebase.Timestamp

class GetAttendanceByDateRangeUseCase(
    private val attendanceRepository: AttendanceRepository
) {
    suspend fun execute(startDate: Timestamp, endDate: Timestamp): Result<List<AttendanceRecord>> {
        return attendanceRepository.getAttendanceByDateRange(startDate, endDate)
    }
}