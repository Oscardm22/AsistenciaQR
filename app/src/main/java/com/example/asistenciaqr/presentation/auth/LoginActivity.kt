package com.example.asistenciaqr.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.asistenciaqr.R
import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.data.repository.AuthRepositoryImpl
import com.example.asistenciaqr.databinding.ActivityLoginBinding
import com.example.asistenciaqr.presentation.main.MainActivity
import com.example.asistenciaqr.presentation.viewmodel.AuthState
import com.example.asistenciaqr.presentation.viewmodel.AuthViewModel
import com.example.asistenciaqr.util.AuthViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthViewModel
    private val authRepository = AuthRepositoryImpl()
    private var keepSplashOnScreen = true
    private lateinit var splashScreen: SplashScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        // Verificar usuario primero
        checkCurrentUser()
    }

    private fun initializeUI() {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupObservers()
        setupListeners()
        setupTextWatchers()
    }

    private fun setupViewModel() {
        val factory = AuthViewModelFactory(authRepository, applicationContext)
        viewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]
    }

    private fun setupObservers() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> showLoading(true)
                is AuthState.Success -> {
                    showLoading(false)
                    showSuccess(getString(R.string.login_success))
                    navigateToMainActivity(state.user)
                }
                is AuthState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
            }
        }

        viewModel.recoveryState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> showLoading(true)
                is AuthState.Success -> {
                    showLoading(false)
                    showSuccess(getString(R.string.recovery_email_sent))
                }
                is AuthState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
            }
        }
    }

    private fun navigateToMainActivity(user: User) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("USER", user)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (validateInputs(email, password)) {
                viewModel.login(email, password)
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            showRecoveryDialogFragment()
        }
    }

    private fun showRecoveryDialogFragment() {
        val recoveryDialog = RecoveryDialogFragment().apply {
            onEmailSubmitted = { email ->
                viewModel.sendPasswordResetEmail(email)
            }
        }
        recoveryDialog.show(supportFragmentManager, "RecoveryDialogFragment")
    }

    private fun setupTextWatchers() {
        // TextWatcher para el campo de email
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.emailInputLayout.error = null
            }
        })

        // TextWatcher para el campo de password
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.passwordInputLayout.error = null
            }
        })

        // FocusChangeListener para email
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.emailInputLayout.error = null
            }
        }

        // FocusChangeListener para password
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.passwordInputLayout.error = null
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        binding.emailInputLayout.error = null
        binding.passwordInputLayout.error = null

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = getString(R.string.error_invalid_email)
            isValid = false
        }

        if (password.isEmpty() || password.length < 6) {
            binding.passwordInputLayout.error = getString(R.string.error_short_password)
            isValid = false
        }

        return isValid
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.tvForgotPassword.isEnabled = !show
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun checkCurrentUser() {
        lifecycleScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                // Si ya hay un usuario logeado, navegar directamente sin mostrar el login
                navigateToMainActivity(currentUser)
            } else {
                // Si no hay usuario logeado, mostrar la interfaz de login
                keepSplashOnScreen = false // Quitar el splash screen
                initializeUI() // Mostrar la UI del login
            }
        }
    }
}