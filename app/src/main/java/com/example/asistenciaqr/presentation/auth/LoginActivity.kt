package com.example.asistenciaqr.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.asistenciaqr.MainActivity
import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.data.repository.AuthRepositoryImpl
import com.example.asistenciaqr.databinding.ActivityLoginBinding
import com.example.asistenciaqr.domain.usecase.LoginUseCase
import com.example.asistenciaqr.domain.usecase.RegisterUseCase
import com.example.asistenciaqr.presentation.viewmodel.AuthState
import com.example.asistenciaqr.presentation.viewmodel.AuthViewModel
import com.example.asistenciaqr.util.AuthViewModelFactory
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupObservers()
        setupListeners()
        checkCurrentUser()
    }

    private fun setupViewModel() {
        val authRepository = AuthRepositoryImpl()
        val loginUseCase = LoginUseCase(authRepository)
        val registerUseCase = RegisterUseCase(authRepository)
        viewModel = ViewModelProvider(this, AuthViewModelFactory(loginUseCase, registerUseCase))[AuthViewModel::class.java]
    }

    private fun setupObservers() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> showLoading(true)
                is AuthState.Success -> {
                    showLoading(false)
                    navigateToMainActivity(state.user)
                }
                is AuthState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (validateInputs(email, password)) {
                viewModel.login(email, password)
            }
        }

        binding.tvRegister.setOnClickListener {
            navigateToRegister()
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Ingrese un correo válido"
            isValid = false
        }

        if (password.isEmpty() || password.length < 6) {
            binding.etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
        }

        return isValid
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun navigateToMainActivity(user: User) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("USER_ID", user.uid)
            putExtra("USER_NAMES", user.names)
            putExtra("USER_LASTNAMES", user.lastnames)
            putExtra("IS_ADMIN", user.isAdmin)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun checkCurrentUser() {
        lifecycleScope.launch {
            val authRepository = AuthRepositoryImpl()
            val currentUser = authRepository.getCurrentUser()
            currentUser?.let {
                navigateToMainActivity(it)
            }
        }
    }
}