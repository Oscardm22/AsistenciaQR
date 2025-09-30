package com.example.asistenciaqr.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.asistenciaqr.data.repository.AttendanceRepositoryImpl
import com.example.asistenciaqr.domain.usecase.*
import com.example.asistenciaqr.presentation.viewmodel.AttendanceViewModel

class AttendanceViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            val repository = AttendanceRepositoryImpl()
            val registerAttendanceUseCase = RegisterAttendanceUseCase(repository)
            val getAttendanceUseCase = GetAttendanceUseCase(repository)
            val getTodayAttendanceUseCase = GetTodayAttendanceUseCase(repository)
            val getAllAttendanceUseCase = GetAllAttendanceUseCase(repository)
            val getAttendanceByDateRangeUseCase = GetAttendanceByDateRangeUseCase(repository)

            return AttendanceViewModel(
                registerAttendanceUseCase,
                getAttendanceUseCase,
                getTodayAttendanceUseCase,
                getAllAttendanceUseCase,
                getAttendanceByDateRangeUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}