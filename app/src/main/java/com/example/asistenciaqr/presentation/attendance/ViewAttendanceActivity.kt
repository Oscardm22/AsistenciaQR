package com.example.asistenciaqr.presentation.attendance

import android.app.DatePickerDialog
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
import com.google.firebase.Timestamp
import java.util.*
import androidx.core.view.isVisible

class ViewAttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewAttendanceBinding
    private val viewModel: AttendanceViewModel by viewModels { AttendanceViewModelFactory() }
    private lateinit var adapter: AttendanceAdapter
    private var userId: String? = null
    private var showAll: Boolean = false
    private var userName: String? = null
    private var startDate: Date? = null
    private var endDate: Date? = null
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setStatusBarColor()
        setupIntentExtras()
        setupToolbar()
        setupRecyclerView()
        setupFilters()
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
        showAll = intent.getBooleanExtra("SHOW_ALL", false)
        userId = intent.getStringExtra("USER_ID")
        userName = intent.getStringExtra("USER_NAME")

        if (showAll) {
            binding.toolbar.title = "Todas las Asistencias"
            binding.fabFilter.visibility = android.view.View.VISIBLE
        } else {
            binding.toolbar.title = "Mis Asistencias"
            userName?.let {
                binding.toolbar.subtitle = it
            }
            binding.fabFilter.visibility = android.view.View.GONE
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

    private fun setupFilters() {
        // Mostrar/ocultar card de filtros
        binding.fabFilter.setOnClickListener {
            val isVisible = binding.cardFilters.isVisible
            binding.cardFilters.visibility = if (isVisible) android.view.View.GONE else android.view.View.VISIBLE
        }

        // Selector de fecha inicio
        binding.btnStartDate.setOnClickListener {
            showDatePicker(true)
        }

        // Selector de fecha fin
        binding.btnEndDate.setOnClickListener {
            showDatePicker(false)
        }

        // Aplicar filtro
        binding.btnApplyFilter.setOnClickListener {
            applyDateFilter()
        }

        // Limpiar filtro
        binding.btnClearFilter.setOnClickListener {
            clearDateFilter()
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, if (isStartDate) 0 else 23)
                    set(Calendar.MINUTE, if (isStartDate) 0 else 59)
                    set(Calendar.SECOND, if (isStartDate) 0 else 59)
                }.time

                if (isStartDate) {
                    startDate = selectedDate
                    binding.btnStartDate.text = formatDate(selectedDate)
                } else {
                    endDate = selectedDate
                    binding.btnEndDate.text = formatDate(selectedDate)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun applyDateFilter() {
        if (startDate == null || endDate == null) {
            showError("Selecciona ambas fechas para filtrar")
            return
        }

        if (startDate!!.after(endDate)) {
            showError("La fecha inicio no puede ser mayor a la fecha fin")
            return
        }

        // Convertir a Timestamp de Firebase
        val startTimestamp = Timestamp(startDate!!)
        val endTimestamp = Timestamp(endDate!!)

        // Aplicar filtro
        viewModel.getAttendanceByDateRange(startTimestamp, endTimestamp)

        // Ocultar filtros después de aplicar
        binding.cardFilters.visibility = android.view.View.GONE

        // Actualizar título con rango de fechas
        binding.toolbar.title = "Asistencias: ${formatDate(startDate!!)} - ${formatDate(endDate!!)}"
    }

    private fun clearDateFilter() {
        startDate = null
        endDate = null
        binding.btnStartDate.text = "Fecha inicio"
        binding.btnEndDate.text = "Fecha fin"
        binding.cardFilters.visibility = android.view.View.GONE

        // Restaurar título original y cargar todas las asistencias
        if (showAll) {
            binding.toolbar.title = "Todas las Asistencias"
            viewModel.resetToAllAttendance()
        } else {
            binding.toolbar.title = "Mis Asistencias"
            userId?.let { viewModel.getAttendanceByUser(it) }
        }
    }

    private fun formatDate(date: Date): String {
        return android.text.format.DateFormat.format("dd/MM/yyyy", date).toString()
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
            viewModel.getAllAttendance()
        } else {
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
            if (startDate != null && endDate != null) {
                "No hay asistencias en el rango de fechas seleccionado"
            } else {
                "No hay registros de asistencia"
            }
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