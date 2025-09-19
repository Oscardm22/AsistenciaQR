package com.example.asistenciaqr.domain.usecase

import com.example.asistenciaqr.domain.repository.UserRepository

class SoftDeleteTeacherUseCase(
    private val userRepository: UserRepository
) {
    suspend fun execute(userId: String): Boolean {
        val result = userRepository.softDeleteTeacher(userId)
        return if (result.isSuccess) {
            result.getOrThrow()
        } else {
            throw Exception(result.exceptionOrNull()?.message ?: "Error al eliminar profesor")
        }
    }
}