package com.example.asistenciaqr.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asistenciaqr.data.model.AttendanceRecord
import com.example.asistenciaqr.domain.usecase.GetAllAttendanceUseCase
import com.example.asistenciaqr.domain.usecase.GetAttendanceUseCase
import com.example.asistenciaqr.domain.usecase.GetTodayAttendanceUseCase
import com.example.asistenciaqr.domain.usecase.RegisterAttendanceUseCase
import kotlinx.coroutines.launch

class AttendanceViewModel(
    private val registerAttendanceUseCase: RegisterAttendanceUseCase,
    private val getAttendanceUseCase: GetAttendanceUseCase,
    private val getTodayAttendanceUseCase: GetTodayAttendanceUseCase,
    private val getAllAttendanceUseCase: GetAllAttendanceUseCase
) : ViewModel() {

    private val _attendanceState = MutableLiveData<AttendanceState>()
    val attendanceState: LiveData<AttendanceState> = _attendanceState

    private val _attendanceListState = MutableLiveData<AttendanceListState>()
    val attendanceListState: LiveData<AttendanceListState> = _attendanceListState

    fun registerAttendance(record: AttendanceRecord) {
        viewModelScope.launch {
            _attendanceState.value = AttendanceState.Loading
            val result = registerAttendanceUseCase.execute(record)
            if (result.isSuccess) {
                _attendanceState.value = AttendanceState.Success
            } else {
                _attendanceState.value = AttendanceState.Error(
                    result.exceptionOrNull()?.message ?: "Error al registrar asistencia"
                )
            }
        }
    }

    fun getAttendanceByUser(userId: String) {
        viewModelScope.launch {
            _attendanceListState.value = AttendanceListState.Loading
            val result = getAttendanceUseCase.execute(userId)
            if (result.isSuccess) {
                _attendanceListState.value = AttendanceListState.Success(result.getOrNull() ?: emptyList())
            } else {
                _attendanceListState.value = AttendanceListState.Error(
                    result.exceptionOrNull()?.message ?: "Error al obtener asistencias"
                )
            }
        }
    }

    fun getTodayAttendanceByUser(userId: String) {
        viewModelScope.launch {
            _attendanceListState.value = AttendanceListState.Loading
            val result = getTodayAttendanceUseCase.execute(userId)
            if (result.isSuccess) {
                _attendanceListState.value = AttendanceListState.Success(result.getOrNull() ?: emptyList())
            } else {
                _attendanceListState.value = AttendanceListState.Error(
                    result.exceptionOrNull()?.message ?: "Error al obtener asistencias de hoy"
                )
            }
        }
    }

    fun getAllAttendance() {
        viewModelScope.launch {
            _attendanceListState.value = AttendanceListState.Loading
            val result = getAllAttendanceUseCase.execute()
            if (result.isSuccess) {
                _attendanceListState.value = AttendanceListState.Success(result.getOrNull() ?: emptyList())
            } else {
                _attendanceListState.value = AttendanceListState.Error(
                    result.exceptionOrNull()?.message ?: "Error al obtener todas las asistencias"
                )
            }
        }
    }
}

sealed class AttendanceState {
    object Loading : AttendanceState()
    object Success : AttendanceState()
    data class Error(val message: String) : AttendanceState()
}

sealed class AttendanceListState {
    object Loading : AttendanceListState()
    data class Success(val records: List<AttendanceRecord>) : AttendanceListState()
    data class Error(val message: String) : AttendanceListState()
}