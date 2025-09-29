package com.example.asistenciaqr.presentation.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.asistenciaqr.data.model.AttendanceRecord
import com.example.asistenciaqr.data.model.AttendanceType
import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.databinding.ActivityQrScannerBinding
import com.example.asistenciaqr.presentation.viewmodel.AttendanceViewModel
import com.example.asistenciaqr.util.AttendanceViewModelFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class QrScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private lateinit var viewModel: AttendanceViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentUser: User? = null
    private var currentLocation: Location? = null

    // Contract para escanear QR
    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            handleQrResult(result.contents)
        } else {
            showError("Escaneo cancelado")
        }
    }

    // Contract para permisos de ubicación
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            getCurrentLocation()
        } else {
            showError("Se necesitan permisos de ubicación para registrar asistencia")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        getCurrentUser()
        setupUI()
        checkPermissionsAndSetup()
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
                    showSuccess("Asistencia registrada exitosamente")
                    finish()
                }
                is com.example.asistenciaqr.presentation.viewmodel.AttendanceState.Error -> {
                    showLoading(false)
                    showError(state.message)
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
            showError("No se pudo obtener información del usuario")
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
                        binding.tvLocationStatus.text = "Ubicación obtenida"
                    } else {
                        binding.tvLocationStatus.text = "Ubicación no disponible"
                        showError("No se pudo obtener la ubicación actual")
                    }
                }
                .addOnFailureListener { e ->
                    binding.tvLocationStatus.text = "Error obteniendo ubicación"
                    showError("Error al obtener ubicación: ${e.message}")
                }
        }
    }

    private fun launchQrScanner() {
        if (currentLocation == null) {
            showError("Esperando ubicación...")
            getCurrentLocation()
            return
        }

        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Escanea tu código QR de asistencia")
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(true)
        }

        qrScannerLauncher.launch(options)
    }

    private fun handleQrResult(qrData: String) {
        val user = currentUser
        val location = currentLocation

        if (user == null) {
            showError("Error: Usuario no disponible")
            return
        }

        if (location == null) {
            showError("Error: Ubicación no disponible")
            return
        }

        // Validar que el QR escaneado pertenezca al usuario actual
        if (!isValidUserQr(qrData, user)) {
            showError("El código QR no corresponde a tu usuario")
            return
        }

        // Crear registro de asistencia
        val attendanceRecord = AttendanceRecord(
            userId = user.uid,
            userNames = user.names,
            userLastnames = user.lastnames,
            type = AttendanceType.ENTRY,
            latitude = location.latitude,
            longitude = location.longitude,
            qrData = qrData
        )

        // Registrar asistencia
        viewModel.registerAttendance(attendanceRecord)
    }

    private fun isValidUserQr(qrData: String, user: User): Boolean {
        // El QR debería contener algo como: "TEACHER:${user.uid}:${user.email}"
        // Como vimos en GenerateQrActivity
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