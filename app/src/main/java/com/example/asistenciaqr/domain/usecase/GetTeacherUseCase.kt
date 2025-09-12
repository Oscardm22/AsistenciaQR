package com.example.asistenciaqr.domain.usecase

import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetTeacherUseCase(private val userRepository: UserRepository) {
    suspend fun execute(userId: String): Result<User> {
        return withContext(Dispatchers.IO) {
            userRepository.getUserById(userId)
        }
    }
}