package com.example.asistenciaqr.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.asistenciaqr.domain.repository.AuthRepository
import com.example.asistenciaqr.domain.repository.AttendanceRepository
import com.example.asistenciaqr.domain.repository.UserRepository
import com.example.asistenciaqr.domain.usecase.*
import com.example.asistenciaqr.presentation.viewmodel.AdminViewModel
import com.example.asistenciaqr.presentation.viewmodel.AttendanceViewModel
import com.example.asistenciaqr.presentation.viewmodel.AuthViewModel
import com.example.asistenciaqr.presentation.viewmodel.TeacherViewModel

class ViewModelFactory(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                val loginUseCase = LoginUseCase(authRepository)
                AuthViewModel(loginUseCase, authRepository) as T
            }
            modelClass.isAssignableFrom(TeacherViewModel::class.java) -> {
                val getTeacherUseCase = GetTeacherUseCase(userRepository)
                val generateQrUseCase = GenerateQrUseCase(userRepository)
                TeacherViewModel(getTeacherUseCase, generateQrUseCase) as T
            }
            modelClass.isAssignableFrom(AdminViewModel::class.java) -> {
                val getUsersUseCase = GetUsersUseCase(userRepository)
                val registerUserUseCase = RegisterUserUseCase(authRepository)
                val generateQrUseCase = GenerateQrUseCase(userRepository)
                AdminViewModel(getUsersUseCase, registerUserUseCase, generateQrUseCase) as T
            }
            modelClass.isAssignableFrom(AttendanceViewModel::class.java) -> {
                val registerAttendanceUseCase = RegisterAttendanceUseCase(attendanceRepository)
                val getAttendanceUseCase = GetAttendanceUseCase(attendanceRepository)
                AttendanceViewModel(registerAttendanceUseCase, getAttendanceUseCase) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}