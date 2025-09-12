package com.example.asistenciaqr.domain.usecase

import com.example.asistenciaqr.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GenerateQrUseCase(private val userRepository: UserRepository) {
    suspend fun execute(userId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            userRepository.generateQrForUser(userId)
        }
    }
}