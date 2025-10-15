package com.example.asistenciaqr.presentation.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.asistenciaqr.R
import com.example.asistenciaqr.data.model.AttendanceRecord
import com.example.asistenciaqr.data.model.AttendanceType
import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.databinding.ActivityQrScannerBinding
import com.example.asistenciaqr.presentation.viewmodel.AttendanceViewModel
import com.example.asistenciaqr.util.AttendanceViewModelFactory
import com.example.asistenciaqr.util.LocationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QrScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private lateinit var viewModel: AttendanceViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationHelper: LocationHelper
    private var currentUser: User? = null
    private var currentLocation: Location? = null
    private var currentQrData: String? = null // Para guardar el QR data temporalmente

    // Contract para escanear QR
    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            handleQrResult(result.contents)
        } else {
            showError(getString(R.string.scan_cancelled))
        }
    }

    // Contract para permisos de ubicación
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            getCurrentLocation()
        } else {
            showError(getString(R.string.location_permission_required))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setStatusBarColor()
        setupViewModel()
        getCurrentUser()
        setupUI()
        checkPermissionsAndSetup()
        setupToolbar()
        locationHelper = LocationHelper(this)
    }

    private fun setStatusBarColor() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_500)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = getString(R.string.scan_qr_attendance_title)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupViewModel() {
        val factory = AttendanceViewModelFactory()
        viewModel = ViewModelProvider(this, factory)[AttendanceViewModel::class.java]

        // Observar el estado del registro de asistencia
        viewModel.attendanceState.observe(this) { state ->
            when (state) {
                is com.example.asistenciaqr.presentation.viewmodel.AttendanceState.Loading -> {
                    showLoading(true)
                }
                is com.example.asistenciaqr.presentation.viewmodel.AttendanceState.Success -> {
                    showLoading(false)
                    showSuccess(getString(R.string.attendance_registered_success))
                    finish()
                }
                is com.example.asistenciaqr.presentation.viewmodel.AttendanceState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
            }
        }

        // NUEVO: Observer para los registros del día
        viewModel.attendanceListState.observe(this) { state ->
            when (state) {
                is com.example.asistenciaqr.presentation.viewmodel.AttendanceListState.Success -> {
                    // Cuando obtenemos los registros, proceder a registrar la asistencia
                    handleAttendanceRegistration(state.records)
                }
                is com.example.asistenciaqr.presentation.viewmodel.AttendanceListState.Error -> {
                    showLoading(false)
                    showError(getString(R.string.error_getting_records, state.message))
                }
                is com.example.asistenciaqr.presentation.viewmodel.AttendanceListState.Loading -> {
                    showLoading(true)
                }
            }
        }
    }

    private fun getCurrentUser() {
        currentUser = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("USER", User::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("USER")
        }

        if (currentUser == null) {
            showError(getString(R.string.user_info_error))
            finish()
        }
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnScanQr.setOnClickListener { launchQrScanner() }
    }

    private fun checkPermissionsAndSetup() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val hasAllPermissions = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (hasAllPermissions) {
            getCurrentLocation()
        } else {
            locationPermissionLauncher.launch(requiredPermissions)
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    currentLocation = location
                    if (location != null) {
                        binding.tvLocationStatus.text = getString(R.string.location_obtained)
                    } else {
                        binding.tvLocationStatus.text = getString(R.string.location_not_available)
                        showError(getString(R.string.location_not_available_error))
                    }
                }
                .addOnFailureListener { e ->
                    binding.tvLocationStatus.text = getString(R.string.location_error)
                    showError(getString(R.string.error_getting_location, e.message ?: "Error desconocido"))
                }
        }
    }

    private fun launchQrScanner() {
        if (currentLocation == null) {
            showError(getString(R.string.waiting_location))
            getCurrentLocation()
            return
        }

        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt(getString(R.string.scan_prompt))
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(true)
            setCaptureActivity(CustomCaptureActivity::class.java)
        }

        qrScannerLauncher.launch(options)
    }

    private fun handleQrResult(qrData: String) {
        val user = currentUser
        val location = currentLocation

        if (user == null) {
            showError(getString(R.string.user_not_available))
            return
        }

        if (location == null) {
            showError(getString(R.string.location_not_available_error))
            return
        }

        if (!isValidUserQr(qrData, user)) {
            showError(getString(R.string.invalid_qr_user))
            return
        }

        // Guardar el QR data para usarlo después
        currentQrData = qrData
        showLoading(true)

        // PRIMERO: Obtener registros del día
        viewModel.getTodayAttendanceByUser(user.uid)
    }

    private fun handleAttendanceRegistration(todayRecords: List<AttendanceRecord>) {
        val user = currentUser
        val location = currentLocation
        val qrData = currentQrData

        if (user == null || location == null || qrData == null) {
            showError(getString(R.string.incomplete_data))
            showLoading(false)
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Determinar el tipo basado en los registros existentes
                val attendanceType = determineAttendanceTypeBasedOnRecords(todayRecords)

                // Obtener la dirección
                val address = locationHelper.getDetailedAddressFromLocation(
                    location.latitude,
                    location.longitude
                )

                // Crear y registrar el registro de asistencia
                val attendanceRecord = AttendanceRecord(
                    userId = user.uid,
                    userNames = user.names,
                    userLastnames = user.lastnames,
                    type = attendanceType,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    locationAddress = address,
                    qrData = qrData
                )

                // Registrar la asistencia
                viewModel.registerAttendance(attendanceRecord)

            } catch (e: Exception) {
                showError(getString(R.string.error_general, e.message ?: "Error desconocido"))
                showLoading(false)
            }
        }
    }

    private fun determineAttendanceTypeBasedOnRecords(todayRecords: List<AttendanceRecord>): AttendanceType {
        // Si no hay registros hoy → ENTRY (primera vez del día)
        if (todayRecords.isEmpty()) {
            return AttendanceType.ENTRY
        }

        // Obtener el último registro del día
        val lastRecord = todayRecords.maxByOrNull { it.timestamp }

        // Si el último registro fue ENTRY → ahora es EXIT
        // Si el último registro fue EXIT → ahora es ENTRY (nuevo ciclo)
        return when (lastRecord?.type) {
            AttendanceType.ENTRY -> AttendanceType.EXIT
            AttendanceType.EXIT -> AttendanceType.ENTRY
            else -> AttendanceType.ENTRY // Por defecto
        }
    }

    private fun isValidUserQr(qrData: String, user: User): Boolean {
        return qrData.contains(user.uid) || qrData.contains(user.email)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnScanQr.isEnabled = !show
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}