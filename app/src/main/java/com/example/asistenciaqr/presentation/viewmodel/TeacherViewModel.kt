package com.example.asistenciaqr.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.usecase.AddTeacherUseCase
import com.example.asistenciaqr.domain.usecase.SoftDeleteTeacherUseCase
import com.example.asistenciaqr.domain.usecase.GetTeachersUseCase
import com.example.asistenciaqr.domain.usecase.UpdateTeacherUseCase
import kotlinx.coroutines.launch

class TeacherViewModel(
    private val getTeachersUseCase: GetTeachersUseCase,
    private val addTeacherUseCase: AddTeacherUseCase,
    private val updateTeacherUseCase: UpdateTeacherUseCase,
    private val softDeleteTeacherUseCase: SoftDeleteTeacherUseCase
) : ViewModel() {

    private val _teachers = MutableLiveData<List<User>>()
    val teachers: LiveData<List<User>> = _teachers

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadTeachers() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val teachersList = getTeachersUseCase.execute()
                _teachers.value = teachersList
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al cargar profesores"
            } finally {
                _loading.value = false
            }
        }
    }

    fun addTeacher(user: User, password: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                addTeacherUseCase.execute(user, password)
                loadTeachers() // Recargar la lista
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al agregar profesor"
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateTeacher(user: User) {
        viewModelScope.launch {
            _loading.value = true
            try {
                updateTeacherUseCase.execute(user)
                loadTeachers() // Recargar la lista
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al actualizar profesor"
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteTeacher(userId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                softDeleteTeacherUseCase.execute(userId)
                loadTeachers() // Recargar la lista
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al eliminar profesor"
            } finally {
                _loading.value = false
            }
        }
    }
}