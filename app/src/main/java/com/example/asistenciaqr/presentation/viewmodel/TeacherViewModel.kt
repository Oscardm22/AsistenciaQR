package com.example.asistenciaqr.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.usecase.AddTeacherUseCase
import com.example.asistenciaqr.domain.usecase.GetUsersUseCase
import com.example.asistenciaqr.domain.usecase.SoftDeleteTeacherUseCase
import com.example.asistenciaqr.domain.usecase.UpdateTeacherUseCase
import kotlinx.coroutines.launch

class TeacherViewModel(
    private val getTeachersUseCase: GetUsersUseCase,
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

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess

    private val _updateCompleted = MutableLiveData<Boolean>()
    val updateCompleted: LiveData<Boolean> = _updateCompleted

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
                loadTeachers()
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                        "El correo ya está en uso por otra cuenta."
                    is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                        "La contraseña es demasiado débil."
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                        "El correo ingresado no es válido."
                    else -> e.message ?: "Error al agregar profesor"
                }
                _error.value = errorMessage
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateTeacher(user: User) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val success = updateTeacherUseCase.execute(user)
                _updateSuccess.value = success
                if (success) {
                    _updateCompleted.value = true
                    loadTeachers() // Recargar lista si fue exitoso
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al actualizar profesor"
                _updateSuccess.value = false
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

    fun resetUpdateCompleted() {
        _updateCompleted.value = false
    }
}