package com.example.asistenciaqr.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.usecase.LoginUseCase
import kotlinx.coroutines.launch

class AuthViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _loginState = MutableLiveData<AuthState>()
    val loginState: LiveData<AuthState> = _loginState

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
}

// Estados de autenticación
sealed class AuthState {
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}