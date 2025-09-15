package com.example.asistenciaqr.domain.usecase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.asistenciaqr.R
import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.UnknownHostException

class LoginUseCase(
    private val authRepository: AuthRepository,
    private val context: Context
) {
    suspend fun execute(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                // Primero verificar conexión a internet
                if (!isNetworkAvailable()) {
                    return@withContext Result.failure(Exception(context.getString(R.string.error_no_internet)))
                }

                // Intentar login
                val result = authRepository.login(email, password)

                if (result.isSuccess) {
                    result
                } else {
                    // Obtener la excepción original del Result
                    val originalException = result.exceptionOrNull()

                    // Traducir la excepción
                    val errorMessage = when (originalException) {
                        is FirebaseAuthInvalidCredentialsException ->
                            context.getString(R.string.error_invalid_credentials)
                        is FirebaseAuthInvalidUserException ->
                            context.getString(R.string.error_user_not_found)
                        is UnknownHostException ->
                            context.getString(R.string.error_no_internet)
                        else ->
                            context.getString(R.string.error_login_general, originalException?.message ?: "Error desconocido")
                    }
                    Result.failure(Exception(errorMessage))
                }

            } catch (e: Exception) {
                // Manejar cualquier otra excepción
                Result.failure(Exception(context.getString(R.string.error_login_general, e.message ?: "Error desconocido")))
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }
}