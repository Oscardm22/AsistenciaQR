package com.example.asistenciaqr.presentation.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.asistenciaqr.R
import com.example.asistenciaqr.data.model.User
import com.example.asistenciaqr.databinding.ActivityAddTeacherBinding
import com.example.asistenciaqr.presentation.viewmodel.TeacherViewModel
import com.example.asistenciaqr.util.TeacherViewModelFactory
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Date

class AddTeacherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTeacherBinding
    private var photoBase64: String? = null
    private lateinit var viewModel: TeacherViewModel

    // Contract para tomar foto
    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.let { data ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    data.getParcelableExtra("data", android.graphics.Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    data.getParcelableExtra("data")
                }
            }

            imageBitmap?.let {
                photoBase64 = convertBitmapToBase64(it)
                binding.ivPhotoPreview.setImageBitmap(it)
                binding.ivPhotoPreview.visibility = android.view.View.VISIBLE
            }
        }
    }

    // Contract para seleccionar imagen de galería
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(it)
                val imageBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                photoBase64 = convertBitmapToBase64(imageBitmap)
                binding.ivPhotoPreview.setImageBitmap(imageBitmap)
                binding.ivPhotoPreview.visibility = android.view.View.VISIBLE
            } catch (e: Exception) {
                showError("Error al cargar la imagen: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddTeacherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val factory = TeacherViewModelFactory()
        viewModel = ViewModelProvider(this, factory)[TeacherViewModel::class.java]

        setStatusBarColor()
        setupToolbar()
        setupListeners()
        setupObservers()
    }

    private fun setupObservers() {

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let { showError(it) }
        }

        viewModel.teachers.observe(this) {
            showSuccess("Profesor agregado correctamente")
            finish()
        }
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
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        binding.btnSaveTeacher.setOnClickListener {
            saveTeacher()
        }

        binding.btnTakePhoto.setOnClickListener {
            showPhotoOptionsDialog()
        }
    }

    private fun showPhotoOptionsDialog() {
        val options = arrayOf("Tomar Foto", "Elegir de Galería", "Cancelar")

        android.app.AlertDialog.Builder(this)
            .setTitle("Seleccionar Foto")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> pickImageFromGallery()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun takePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoLauncher.launch(takePictureIntent)
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent.toString())
    }

    private fun convertBitmapToBase64(bitmap: android.graphics.Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun saveTeacher() {
        val names = binding.etNames.text.toString().trim()
        val lastnames = binding.etLastnames.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val isAdmin = binding.switchAdmin.isChecked
        val isActive = binding.switchActive.isChecked

        if (!validateForm(names, lastnames, email, password, confirmPassword)) return

        val newTeacher = User(
            uid = "",
            email = email,
            names = names,
            lastnames = lastnames,
            admin = isAdmin,
            photoBase64 = photoBase64,
            qrCodeData = null,
            createdAt = Date(),
            active = isActive
        )

        viewModel.addTeacher(newTeacher, password)
    }

    private fun validateForm(
        names: String,
        lastnames: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        // Validar nombres
        if (names.isEmpty()) {
            binding.inputLayoutNames.error = "Los nombres son obligatorios"
            isValid = false
        } else {
            binding.inputLayoutNames.error = null
        }

        // Validar apellidos
        if (lastnames.isEmpty()) {
            binding.inputLayoutLastnames.error = "Los apellidos son obligatorios"
            isValid = false
        } else {
            binding.inputLayoutLastnames.error = null
        }

        // Validar email
        if (email.isEmpty()) {
            binding.inputLayoutEmail.error = "El email es obligatorio"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputLayoutEmail.error = "Email inválido"
            isValid = false
        } else {
            binding.inputLayoutEmail.error = null
        }

        // Validar contraseña
        if (password.isEmpty()) {
            binding.inputLayoutPassword.error = "La contraseña es obligatoria"
            isValid = false
        } else if (password.length < 6) {
            binding.inputLayoutPassword.error = "Mínimo 6 caracteres"
            isValid = false
        } else {
            binding.inputLayoutPassword.error = null
        }

        // Validar confirmación de contraseña
        if (confirmPassword.isEmpty()) {
            binding.inputLayoutConfirmPassword.error = "Confirma la contraseña"
            isValid = false
        } else if (password != confirmPassword) {
            binding.inputLayoutConfirmPassword.error = "Las contraseñas no coinciden"
            isValid = false
        } else {
            binding.inputLayoutConfirmPassword.error = null
        }

        return isValid
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}