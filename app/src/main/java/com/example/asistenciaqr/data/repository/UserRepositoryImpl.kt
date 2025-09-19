package com.example.asistenciaqr.data.repository

import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class UserRepositoryImpl : UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")

    override suspend fun getUsers(): Result<List<User>> {
        return try {
            val users = usersCollection
                .whereEqualTo("active", true)
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
                .whereEqualTo("active", true)
                .get()
                .await()
                .toObjects(User::class.java)

            teachers.forEach {
                println("DEBUG: ${it.names} - admin: ${it.admin}, active: ${it.active}")
            }

            Result.success(teachers)
        } catch (e: Exception) {
            println("DEBUG: Error en getTeachers: ${e.message}")
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

    override suspend fun getActiveTeachers(): Result<List<User>> {
        return try {
            val teachers = usersCollection
                .whereEqualTo("active", true)
                .get()
                .await()
                .toObjects(User::class.java)

            Result.success(teachers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addTeacher(user: User, password: String): Result<Boolean> {
        return try {
            // 1. Crear el nuevo usuario
            val authResult = auth.createUserWithEmailAndPassword(user.email, password).await()

            // 2. Actualizar perfil si es necesario
            val profileUpdates = userProfileChangeRequest {
                displayName = "${user.names} ${user.lastnames}"
            }
            authResult.user?.updateProfile(profileUpdates)?.await()

            // 3. Guardar datos adicionales en Firestore
            val userWithId = user.copy(
                uid = authResult.user?.uid ?: "",
                createdAt = Date()
            )
            usersCollection.document(userWithId.uid).set(userWithId).await()

            // 4. Cerrar sesión del nuevo usuario
            auth.signOut()

            Result.success(true)
        } catch (e: Exception) {
            auth.signOut()
            Result.failure(e)
        }
    }

    override suspend fun softDeleteTeacher(userId: String): Result<Boolean> {
        return try {
            // 1. Marcar como inactivo en Firestore
            val updates = hashMapOf<String, Any>(
                "active" to false,
                "deletedAt" to FieldValue.serverTimestamp()
            )

            usersCollection.document(userId).update(updates).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}