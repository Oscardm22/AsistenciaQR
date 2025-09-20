package com.example.asistenciaqr.domain.usecase

import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.repository.UserRepository

class AddTeacherUseCase(
    private val userRepository: UserRepository
) {
    suspend fun execute(user: User, password: String): Boolean {
        return userRepository.addTeacher(user, password).getOrThrow()
    }
}