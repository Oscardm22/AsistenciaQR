package com.example.asistenciaqr.presentation.attendance

import com.google.android.material.datepicker.MaterialDatePicker
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
            binding.toolbar.title = getString(R.string.all_attendance)
            binding.fabFilter.visibility = android.view.View.VISIBLE
        } else {
            binding.toolbar.title = getString(R.string.my_attendance)
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
        val title = if (isStartDate) {
            getString(R.string.select_start_date)
        } else {
            getString(R.string.select_end_date)
        }

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setTheme(R.style.LightDatePicker)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            // Crear calendario en UTC para obtener solo la fecha sin hora
            val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            utcCalendar.timeInMillis = selection

            // Extraer año, mes y día en UTC
            val year = utcCalendar.get(Calendar.YEAR)
            val month = utcCalendar.get(Calendar.MONTH)
            val day = utcCalendar.get(Calendar.DAY_OF_MONTH)

            // Crear calendario local con la fecha UTC pero en zona horaria local
            val localCalendar = Calendar.getInstance()
            localCalendar.set(Calendar.YEAR, year)
            localCalendar.set(Calendar.MONTH, month)
            localCalendar.set(Calendar.DAY_OF_MONTH, day)

            // Ajustes de hora según tipo de fecha
            if (isStartDate) {
                localCalendar.set(Calendar.HOUR_OF_DAY, 0)
                localCalendar.set(Calendar.MINUTE, 0)
                localCalendar.set(Calendar.SECOND, 0)
                localCalendar.set(Calendar.MILLISECOND, 0)
            } else {
                localCalendar.set(Calendar.HOUR_OF_DAY, 23)
                localCalendar.set(Calendar.MINUTE, 59)
                localCalendar.set(Calendar.SECOND, 59)
                localCalendar.set(Calendar.MILLISECOND, 999)
            }

            val selectedDate = localCalendar.time

            if (isStartDate) {
                startDate = selectedDate
                binding.btnStartDate.text = formatDate(selectedDate)
            } else {
                endDate = selectedDate
                binding.btnEndDate.text = formatDate(selectedDate)
            }

            println("Fecha seleccionada: ${formatDate(selectedDate)}")
            println("Timestamp: ${selectedDate.time}")
        }
        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun applyDateFilter() {
        if (startDate == null || endDate == null) {
            showError(getString(R.string.select_both_dates))
            return
        }

        if (startDate!!.after(endDate)) {
            showError(getString(R.string.invalid_date_range))
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
        val dateRangeText = getString(
            R.string.attendance_range,
            formatDate(startDate!!),
            formatDate(endDate!!)
        )
        binding.toolbar.title = dateRangeText
    }

    private fun clearDateFilter() {
        startDate = null
        endDate = null
        binding.btnStartDate.text = getString(R.string.start_date)
        binding.btnEndDate.text = getString(R.string.end_date)
        binding.cardFilters.visibility = android.view.View.GONE

        // Restaurar título original y cargar todas las asistencias
        if (showAll) {
            binding.toolbar.title = getString(R.string.all_attendance)
            viewModel.resetToAllAttendance()
        } else {
            binding.toolbar.title = getString(R.string.my_attendance)
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
                showError(getString(R.string.user_identification_error))
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
                getString(R.string.no_attendance_in_range)
            } else {
                getString(R.string.no_attendance_records)
            }
        } else {
            getString(R.string.no_personal_attendance)
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