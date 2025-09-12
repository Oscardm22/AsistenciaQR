package com.example.asistenciaqr.domain.usecase

import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetUsersUseCase(private val userRepository: UserRepository) {
    suspend fun execute(): Result<List<User>> {
        return withContext(Dispatchers.IO) {
            userRepository.getUsers()
        }
    }
}