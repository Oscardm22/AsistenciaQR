package com.example.asistenciaqr.domain.usecase

import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            authRepository.login(email, password)
        }
    }
}