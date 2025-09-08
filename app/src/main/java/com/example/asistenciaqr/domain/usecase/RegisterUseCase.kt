package com.example.asistenciaqr.domain.usecase

import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RegisterUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(user: User, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            authRepository.register(user, password)
        }
    }
}