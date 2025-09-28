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
import com.yalantis.ucrop.UCrop
import java.io.ByteArrayOutputStream
import java.io.File
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
                // Guardar bitmap temporalmente y abrir UCrop
                val tempUri = saveBitmapToTempUri(it)
                startCropActivity(tempUri)
            }
        }
    }

    // Contract para seleccionar imagen de galería
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Abrir UCrop directamente con la URI de la imagen seleccionada
            startCropActivity(it)
        }
    }

    // Contract para el resultado de UCrop (usando StartActivityForResult)
    private val cropImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = result.data?.let { UCrop.getOutput(it) }
            resultUri?.let {
                processCroppedImage(it)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = result.data?.let { UCrop.getError(it) }
            showError("Error al recortar imagen: ${cropError?.message}")
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

        viewModel.loading.observe(this) { isLoading ->
            binding.btnSaveTeacher.isEnabled = !isLoading
            if (isLoading) {
                binding.btnSaveTeacher.text = getString(R.string.saving)
            } else {
                binding.btnSaveTeacher.text = getString(R.string.save_user)
            }
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

        // Botón para eliminar foto seleccionada
        binding.ivPhotoPreview.setOnClickListener {
            showRemovePhotoDialog()
        }
    }

    private fun showRemovePhotoDialog() {
        val builder = android.app.AlertDialog.Builder(
            this,
            R.style.ThemeOverlay_AsistenciaQR_AlertDialog_Delete
        )

        builder.setTitle("Eliminar foto")
            .setMessage("¿Quieres eliminar la foto seleccionada?")
            .setPositiveButton("Eliminar") { dialog, _ ->
                binding.ivPhotoPreview.setImageDrawable(null)
                binding.ivPhotoPreview.visibility = android.view.View.GONE
                photoBase64 = null
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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
        pickImageLauncher.launch("image/*")
    }

    private fun startCropActivity(sourceUri: Uri) {
        // Configurar opciones de UCrop
        val options = UCrop.Options()
        options.setToolbarColor(ContextCompat.getColor(this, R.color.purple_500))
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.purple_700))
        options.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.white))

        // Crear URI de destino para la imagen recortada
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))

        // Configurar UCrop
        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f) // Ratio cuadrado
            .withMaxResultSize(500, 500) // Tamaño máximo
            .withOptions(options)

        // Lanzar UCrop usando el contract
        cropImageLauncher.launch(uCrop.getIntent(this))
    }

    private fun processCroppedImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val imageBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)

            // Convertir a Base64
            photoBase64 = convertBitmapToBase64(imageBitmap)

            // Mostrar en ImageView
            binding.ivPhotoPreview.setImageBitmap(imageBitmap)
            binding.ivPhotoPreview.visibility = android.view.View.VISIBLE

            showSuccess("Imagen ajustada correctamente")

        } catch (e: Exception) {
            showError("Error al procesar imagen: ${e.message}")
        }
    }

    private fun saveBitmapToTempUri(bitmap: android.graphics.Bitmap): Uri {
        val file = File(cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
        val outputStream = java.io.FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.close()
        return Uri.fromFile(file)
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