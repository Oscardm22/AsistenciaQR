package com.example.asistenciaqr.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.usecase.LoginUseCase
import com.example.asistenciaqr.domain.usecase.RegisterUseCase
import kotlinx.coroutines.launch

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _loginState = MutableLiveData<AuthState>()
    val loginState: LiveData<AuthState> = _loginState

    private val _registerState = MutableLiveData<AuthState>()
    val registerState: LiveData<AuthState> = _registerState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = AuthState.Loading
            val result = loginUseCase.execute(email, password)
            if (result.isSuccess) {
                _loginState.value = AuthState.Success(result.getOrNull()!!)
            } else {
                _loginState.value =
                    AuthState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun register(user: User, password: String) {
        viewModelScope.launch {
            _registerState.value = AuthState.Loading
            val result = registerUseCase.execute(user, password)
            if (result.isSuccess) {
                _registerState.value = AuthState.Success(result.getOrNull()!!)
            } else {
                _registerState.value =
                    AuthState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }
}

// Estados de autenticación
sealed class AuthState {
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}