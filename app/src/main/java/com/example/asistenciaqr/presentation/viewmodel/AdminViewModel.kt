package com.example.asistenciaqr.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.usecase.GenerateQrUseCase
import com.example.asistenciaqr.domain.usecase.GetUsersUseCase
import com.example.asistenciaqr.domain.usecase.RegisterUserUseCase
import kotlinx.coroutines.launch

class AdminViewModel(
    private val getUsersUseCase: GetUsersUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val generateQrUseCase: GenerateQrUseCase
) : ViewModel() {

    private val _usersState = MutableLiveData<AdminUsersState>()
    val usersState: LiveData<AdminUsersState> = _usersState

    private val _registerState = MutableLiveData<AdminRegisterState>()
    val registerState: LiveData<AdminRegisterState> = _registerState

    private val _qrState = MutableLiveData<AdminQrState>()
    val qrState: LiveData<AdminQrState> = _qrState

    fun getUsers() {
        viewModelScope.launch {
            _usersState.value = AdminUsersState.Loading
            try {
                // getUsersUseCase.execute() ya devuelve List<User> directamente
                val users = getUsersUseCase.execute()
                _usersState.value = AdminUsersState.Success(users)
            } catch (e: Exception) {
                _usersState.value = AdminUsersState.Error(
                    e.message ?: "Error al obtener usuarios"
                )
            }
        }
    }

    fun registerUser(user: User, password: String) {
        viewModelScope.launch {
            _registerState.value = AdminRegisterState.Loading
            val result = registerUserUseCase.execute(user, password)
            if (result.isSuccess) {
                _registerState.value = AdminRegisterState.Success(result.getOrNull()!!)
            } else {
                _registerState.value = AdminRegisterState.Error(
                    result.exceptionOrNull()?.message ?: "Error al registrar usuario"
                )
            }
        }
    }

    fun generateQrForUser(userId: String) {
        viewModelScope.launch {
            _qrState.value = AdminQrState.Loading
            val result = generateQrUseCase.execute(userId)
            if (result.isSuccess) {
                _qrState.value = AdminQrState.Success(result.getOrNull()!!)
            } else {
                _qrState.value = AdminQrState.Error(
                    result.exceptionOrNull()?.message ?: "Error al generar QR"
                )
            }
        }
    }
}

// Estados para la gestión de usuarios (Admin)
sealed class AdminUsersState {
    object Loading : AdminUsersState()
    data class Success(val users: List<User>) : AdminUsersState()
    data class Error(val message: String) : AdminUsersState()
}

// Estados para el registro de usuarios (Admin)
sealed class AdminRegisterState {
    object Loading : AdminRegisterState()
    data class Success(val user: User) : AdminRegisterState()
    data class Error(val message: String) : AdminRegisterState()
}

// Estados para la generación de QR (Admin)
sealed class AdminQrState {
    object Loading : AdminQrState()
    data class Success(val qrData: String) : AdminQrState()
    data class Error(val message: String) : AdminQrState()
}