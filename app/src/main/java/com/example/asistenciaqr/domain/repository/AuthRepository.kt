package com.example.asistenciaqr.domain.repository

import com.example.asistenciaqr.data.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(user: User, password: String): Result<User>
    suspend fun getCurrentUser(): User?
    fun logout()
    suspend fun saveUserToFirestore(user: User): Result<Boolean>
    suspend fun sendPasswordResetEmail(email: String): Result<Boolean>
}