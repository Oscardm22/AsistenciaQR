package com.example.asistenciaqr.presentation.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.asistenciaqr.R
import com.example.asistenciaqr.data.repository.AuthRepositoryImpl
import com.example.asistenciaqr.databinding.ActivityLoginBinding
import com.example.asistenciaqr.domain.usecase.LoginUseCase
import com.example.asistenciaqr.presentation.viewmodel.AuthState
import com.example.asistenciaqr.presentation.viewmodel.AuthViewModel
import com.example.asistenciaqr.util.AuthViewModelFactory
import com.google.android.material.snackbar.Snackbar
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
        setupTextWatchers()
        checkCurrentUser()
    }

    private fun setupViewModel() {
        val authRepository = AuthRepositoryImpl()
        val loginUseCase = LoginUseCase(authRepository)
        viewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(loginUseCase, authRepository)
        )[AuthViewModel::class.java]
    }

    private fun setupObservers() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> showLoading(true)
                is AuthState.Success -> {
                    showLoading(false)
                    showSuccess(getString(R.string.login_success))
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
            val authRepository = AuthRepositoryImpl()
            val currentUser = authRepository.getCurrentUser()
            currentUser?.let {
            }
        }
    }
}