package com.example.asistenciaqr.data.repository

import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl : UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    override suspend fun getUsers(): Result<List<User>> {
        return try {
            val users = usersCollection
                .whereEqualTo("isActive", true)
                .get()
                .await()
                .toObjects(User::class.java)

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTeachers(): Result<List<User>> {
        return try {
            val teachers = usersCollection
                .whereEqualTo("isActive", true)
                .whereEqualTo("isAdmin", false)
                .get()
                .await()
                .toObjects(User::class.java)

            Result.success(teachers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserById(userId: String): Result<User> {
        return try {
            val user = usersCollection.document(userId).get().await()
                .toObject(User::class.java)

            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Usuario no encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateQrForUser(userId: String): Result<String> {
        return try {
            val userResult = getUserById(userId)
            if (userResult.isSuccess) {
                val user = userResult.getOrThrow()
                val qrData = "USER_${userId}_${System.currentTimeMillis()}"
                val updatedUser = user.copy(qrCodeData = qrData)

                usersCollection.document(userId).set(updatedUser).await()
                Result.success(qrData)
            } else {
                Result.failure(userResult.exceptionOrNull() ?: Exception("Error al generar QR"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUser(user: User): Result<Boolean> {
        return try {
            usersCollection.document(user.uid).set(user).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}