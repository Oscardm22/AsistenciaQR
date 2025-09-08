package com.example.asistenciaqr.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.asistenciaqr.domain.usecase.LoginUseCase
import com.example.asistenciaqr.domain.usecase.RegisterUseCase
import com.example.asistenciaqr.presentation.viewmodel.AuthViewModel

class AuthViewModelFactory(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(loginUseCase, registerUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}