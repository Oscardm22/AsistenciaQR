package com.example.asistenciaqr.presentation.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.asistenciaqr.R
import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.data.repository.AuthRepositoryImpl
import com.example.asistenciaqr.databinding.ActivityRegisterBinding
import com.example.asistenciaqr.domain.usecase.LoginUseCase
import com.example.asistenciaqr.domain.usecase.RegisterUseCase
import com.example.asistenciaqr.presentation.viewmodel.AuthState
import com.example.asistenciaqr.presentation.viewmodel.AuthViewModel
import com.example.asistenciaqr.util.AuthViewModelFactory
import com.google.android.material.snackbar.Snackbar

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupObservers()
        setupListeners()
        setupToolbar()
        setupBackPressedHandler()
    }

    private fun setupViewModel() {
        val authRepository = AuthRepositoryImpl()
        val loginUseCase = LoginUseCase(authRepository)
        val registerUseCase = RegisterUseCase(authRepository)
        viewModel = ViewModelProvider(this, AuthViewModelFactory(loginUseCase, registerUseCase))[AuthViewModel::class.java]
    }

    private fun setupObservers() {
        viewModel.registerState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> showLoading(true)
                is AuthState.Success -> {
                    showLoading(false)
                    showSuccess(getString(R.string.register_success))
                    finish()
                }
                is AuthState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            val names = binding.etNames.text.toString().trim()
            val lastnames = binding.etLastnames.text.toString().trim()

            if (validateInputs(email, password, confirmPassword, names, lastnames)) {
                val user = User(
                    email = email,
                    names = names,
                    lastnames = lastnames,
                    isAdmin = false
                )
                viewModel.register(user, password)
            }
        }
    }

    private fun validateInputs(
        email: String,
        password: String,
        confirmPassword: String,
        names: String,
        lastnames: String
    ): Boolean {
        var isValid = true

        // Reset errors
        binding.etEmail.error = null
        binding.etPassword.error = null
        binding.etConfirmPassword.error = null
        binding.etNames.error = null
        binding.etLastnames.error = null

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        }

        if (password.isEmpty() || password.length < 6) {
            binding.etPassword.error = getString(R.string.error_short_password)
            isValid = false
        }

        if (password != confirmPassword) {
            binding.etConfirmPassword.error = getString(R.string.error_password_mismatch)
            isValid = false
        }

        if (names.isEmpty()) {
            binding.etNames.error = getString(R.string.error_empty_names)
            isValid = false
        }

        if (lastnames.isEmpty()) {
            binding.etLastnames.error = getString(R.string.error_empty_lastnames)
            isValid = false
        }

        return isValid
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !show
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = getString(R.string.title_register)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupBackPressedHandler() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        }

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}