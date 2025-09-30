package com.example.asistenciaqr.presentation.attendance

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.asistenciaqr.R
import com.example.asistenciaqr.data.model.AttendanceRecord
import com.example.asistenciaqr.databinding.ActivityViewAttendanceBinding
import com.example.asistenciaqr.presentation.viewmodel.AttendanceViewModel
import com.example.asistenciaqr.util.AttendanceViewModelFactory

class ViewAttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewAttendanceBinding
    private val viewModel: AttendanceViewModel by viewModels { AttendanceViewModelFactory() }
    private lateinit var adapter: AttendanceAdapter

    // Variables para determinar qué datos mostrar
    private var userId: String? = null
    private var showAll: Boolean = false
    private var userName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setStatusBarColor()
        setupIntentExtras()
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        loadAttendanceData()
    }

    private fun setStatusBarColor() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_500)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }
    }

    private fun setupIntentExtras() {
        // Determinar si mostrar todas las asistencias o solo las del usuario
        showAll = intent.getBooleanExtra("SHOW_ALL", false)
        userId = intent.getStringExtra("USER_ID")
        userName = intent.getStringExtra("USER_NAME")

        if (showAll) {
            // Modo administrador - ver todas las asistencias
            binding.toolbar.title = "Todas las Asistencias"
        } else {
            // Modo usuario - ver sus propias asistencias
            binding.toolbar.title = "Mis Asistencias"
            userName?.let {
                binding.toolbar.subtitle = it
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = AttendanceAdapter(showUserNames = showAll)
        binding.rvAttendance.layoutManager = LinearLayoutManager(this)
        binding.rvAttendance.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.attendanceListState.observe(this) { state ->
            when (state) {
                is com.example.asistenciaqr.presentation.viewmodel.AttendanceListState.Loading -> {
                    showLoading(true)
                    hideEmptyState()
                }
                is com.example.asistenciaqr.presentation.viewmodel.AttendanceListState.Success -> {
                    showLoading(false)
                    if (state.records.isNotEmpty()) {
                        showAttendanceList(state.records)
                        hideEmptyState()
                    } else {
                        showEmptyState()
                    }
                }
                is com.example.asistenciaqr.presentation.viewmodel.AttendanceListState.Error -> {
                    showLoading(false)
                    showError(state.message)
                    showEmptyState()
                }
            }
        }
    }

    private fun loadAttendanceData() {
        if (showAll) {
            // Administrador: cargar todas las asistencias
            viewModel.getAllAttendance()
        } else {
            // Usuario: cargar solo sus asistencias
            userId?.let {
                viewModel.getAttendanceByUser(it)
            } ?: run {
                showError("No se pudo identificar al usuario")
                finish()
            }
        }
    }

    private fun showAttendanceList(records: List<AttendanceRecord>) {
        adapter.submitList(records)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun showEmptyState() {
        binding.tvEmptyState.visibility = android.view.View.VISIBLE
        binding.rvAttendance.visibility = android.view.View.GONE

        val emptyText = if (showAll) {
            "No hay registros de asistencia"
        } else {
            "No tienes registros de asistencia"
        }
        binding.tvEmptyState.text = emptyText
    }

    private fun hideEmptyState() {
        binding.tvEmptyState.visibility = android.view.View.GONE
        binding.rvAttendance.visibility = android.view.View.VISIBLE
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
    }
}