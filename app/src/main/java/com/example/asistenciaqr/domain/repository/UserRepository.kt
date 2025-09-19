package com.example.asistenciaqr.domain.repository

import com.example.asistenciaqr.data.model.User

interface UserRepository {
    suspend fun getUsers(): Result<List<User>>
    suspend fun getTeachers(): Result<List<User>>
    suspend fun getActiveTeachers(): Result<List<User>>
    suspend fun getUserById(userId: String): Result<User>
    suspend fun generateQrForUser(userId: String): Result<String>
    suspend fun updateUser(user: User): Result<Boolean>
    suspend fun addTeacher(user: User, password: String): Result<Boolean>
    suspend fun softDeleteTeacher(userId: String): Result<Boolean>
}