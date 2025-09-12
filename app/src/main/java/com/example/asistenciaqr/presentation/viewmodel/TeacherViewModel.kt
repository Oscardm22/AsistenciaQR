package com.example.asistenciaqr.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.usecase.GenerateQrUseCase
import com.example.asistenciaqr.domain.usecase.GetTeacherUseCase
import kotlinx.coroutines.launch

class TeacherViewModel(
    private val getTeacherUseCase: GetTeacherUseCase,
    private val generateQrUseCase: GenerateQrUseCase
) : ViewModel() {

    private val _teacherState = MutableLiveData<TeacherState>()
    val teacherState: LiveData<TeacherState> = _teacherState

    private val _qrState = MutableLiveData<QrState>()
    val qrState: LiveData<QrState> = _qrState

    fun getTeacher(userId: String) {
        viewModelScope.launch {
            _teacherState.value = TeacherState.Loading
            val result = getTeacherUseCase.execute(userId)
            if (result.isSuccess) {
                _teacherState.value = TeacherState.Success(result.getOrNull()!!)
            } else {
                _teacherState.value = TeacherState.Error(
                    result.exceptionOrNull()?.message ?: "Error al obtener datos del profesor"
                )
            }
        }
    }

    fun generateQr(userId: String) {
        viewModelScope.launch {
            _qrState.value = QrState.Loading
            val result = generateQrUseCase.execute(userId)
            if (result.isSuccess) {
                _qrState.value = QrState.Success(result.getOrNull()!!)
            } else {
                _qrState.value = QrState.Error(
                    result.exceptionOrNull()?.message ?: "Error al generar QR"
                )
            }
        }
    }
}

sealed class TeacherState {
    object Loading : TeacherState()
    data class Success(val user: User) : TeacherState()
    data class Error(val message: String) : TeacherState()
}

sealed class QrState {
    object Loading : QrState()
    data class Success(val qrData: String) : QrState()
    data class Error(val message: String) : QrState()
}