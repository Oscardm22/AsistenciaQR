package com.example.asistenciaqr.data.repository

import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                val user = getUserFromFirestore(firebaseUser.uid)
                Result.success(user)
            } else {
                Result.failure(Exception("Error al iniciar sesión"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(user: User, password: String): Result<User> {
        return try {
            // Crear usuario en Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(user.email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Crear usuario en Firestore
                val newUser = user.copy(uid = firebaseUser.uid)
                val result = saveUserToFirestore(newUser)

                if (result.isSuccess) {
                    Result.success(newUser)
                } else {
                    // Rollback: eliminar usuario de Auth si falla Firestore
                    firebaseUser.delete().await()
                    Result.failure(Exception("Error al guardar datos del usuario"))
                }
            } else {
                Result.failure(Exception("Error al crear usuario"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getUserFromFirestore(uid: String): User {
        val document = usersCollection.document(uid).get().await()
        return document.toObject(User::class.java) ?: throw Exception("Usuario no encontrado")
    }

    override suspend fun saveUserToFirestore(user: User): Result<Boolean> {
        return try {
            usersCollection.document(user.uid).set(user).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? {
        return auth.currentUser?.uid?.let { uid ->
            try {
                getUserFromFirestore(uid)
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun logout() {
        auth.signOut()
    }
}