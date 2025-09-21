package com.example.asistenciaqr.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.asistenciaqr.data.repository.UserRepositoryImpl
import com.example.asistenciaqr.domain.usecase.AddTeacherUseCase
import com.example.asistenciaqr.domain.usecase.GetUsersUseCase
import com.example.asistenciaqr.domain.usecase.SoftDeleteTeacherUseCase
import com.example.asistenciaqr.domain.usecase.UpdateTeacherUseCase
import com.example.asistenciaqr.presentation.viewmodel.TeacherViewModel

class TeacherViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherViewModel::class.java)) {
            val userRepository = UserRepositoryImpl()

            val getTeachersUseCase = GetUsersUseCase (userRepository)
            val addTeacherUseCase = AddTeacherUseCase(userRepository)
            val updateTeacherUseCase = UpdateTeacherUseCase(userRepository)
            val softDeleteTeacherUseCase = SoftDeleteTeacherUseCase(userRepository)

            return TeacherViewModel(
                getTeachersUseCase,
                addTeacherUseCase,
                updateTeacherUseCase,
                softDeleteTeacherUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}