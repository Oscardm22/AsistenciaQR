package com.example.asistenciaqr.domain.usecase

import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RegisterUserUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(user: User, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                // Registrar en Authentication
                val authResult = authRepository.register(user, password)
                if (authResult.isSuccess) {
                    val registeredUser = authResult.getOrThrow()

                    // Guardar en Firestore
                    val saveResult = authRepository.saveUserToFirestore(registeredUser)
                    if (saveResult.isSuccess) {
                        Result.success(registeredUser)
                    } else {
                        Result.failure(Exception("Error al guardar usuario en Firestore"))
                    }
                } else {
                    Result.failure(authResult.exceptionOrNull() ?: Exception("Error al registrar usuario"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}