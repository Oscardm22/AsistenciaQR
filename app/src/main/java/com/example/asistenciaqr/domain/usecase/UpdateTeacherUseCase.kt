package com.example.asistenciaqr.domain.usecase

import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.repository.UserRepository

class UpdateTeacherUseCase(
    private val userRepository: UserRepository
) {
    suspend fun execute(user: User): Boolean {
        return userRepository.updateUser(user).getOrDefault(false)
    }
}