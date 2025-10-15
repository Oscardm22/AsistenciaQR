package com.example.asistenciaqr.presentation.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.asistenciaqr.R
import com.example.asistenciaqr.databinding.ActivityMainBinding
import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.data.repository.AuthRepositoryImpl
import com.example.asistenciaqr.presentation.admin.ManageTeachersActivity
import com.example.asistenciaqr.presentation.attendance.QrScannerActivity
import com.example.asistenciaqr.presentation.attendance.ViewAttendanceActivity
import com.example.asistenciaqr.presentation.auth.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var currentUser: User
    private val authRepository = AuthRepositoryImpl()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar primero si hay un usuario en los extras
        val userFromIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("USER", User::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("USER")
        }

        if (userFromIntent != null) {
            currentUser = userFromIntent
            initializeUI()
        } else {
            checkCurrentUser()
        }
    }

    private fun checkCurrentUser() {
        CoroutineScope(Dispatchers.Main).launch {
            authRepository.getCurrentUser()?.let { user ->
                currentUser = user
                initializeUI()
            } ?: run {
                // Si no hay usuario logeado, redirigir al login
                navigateToLogin()
            }
        }
    }

    private fun initializeUI() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        // Usar recursos de string con placeholders
        binding.tvWelcome.text = getString(R.string.welcome_message, currentUser.names)

        // Usar recursos para los roles
        binding.tvRole.text = if (currentUser.admin) {
            getString(R.string.role_administrator)
        } else {
            getString(R.string.role_teacher)
        }

        // Configurar textos de botones desde recursos
        binding.btnScanQr.text = getString(R.string.scan_qr)
        binding.btnViewMyAttendance.text = getString(R.string.view_my_attendance)
        binding.btnManageTeachers.text = getString(R.string.manage_teachers)
        binding.btnViewAllAttendance.text = getString(R.string.view_all_attendance)
        binding.btnLogout.text = getString(R.string.logout)
        binding.tvAdminSection.text = getString(R.string.admin_section)

        if (currentUser.admin) {
            binding.cardAdminSection.visibility = View.VISIBLE
        } else {
            binding.cardAdminSection.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.btnScanQr.setOnClickListener {
            val intent = Intent(this, QrScannerActivity::class.java)
            intent.putExtra("USER", currentUser)
            startActivity(intent)
        }

        binding.btnViewMyAttendance.setOnClickListener {
            val intent = Intent(this, ViewAttendanceActivity::class.java)
            intent.putExtra("USER_ID", currentUser.uid)
            intent.putExtra("USER_NAME", "${currentUser.names} ${currentUser.lastnames}")
            startActivity(intent)
        }

        if (currentUser.admin) {
            binding.btnManageTeachers.setOnClickListener {
                startActivity(Intent(this, ManageTeachersActivity::class.java))
            }

            // Eliminar este listener
            // binding.btnGenerateQr.setOnClickListener {
            //     startActivity(Intent(this, GenerateQrActivity::class.java))
            // }

            binding.btnViewAllAttendance.setOnClickListener {
                val intent = Intent(this, ViewAttendanceActivity::class.java)
                intent.putExtra("SHOW_ALL", true)
                startActivity(intent)
            }
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        authRepository.logout()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}