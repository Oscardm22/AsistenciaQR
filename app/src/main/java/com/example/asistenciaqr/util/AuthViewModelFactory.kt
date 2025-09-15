package com.example.asistenciaqr.util

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.asistenciaqr.domain.repository.AuthRepository
import com.example.asistenciaqr.domain.usecase.LoginUseCase
import com.example.asistenciaqr.presentation.viewmodel.AuthViewModel

class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            val loginUseCase = LoginUseCase(authRepository, context)
            return AuthViewModel(loginUseCase, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}