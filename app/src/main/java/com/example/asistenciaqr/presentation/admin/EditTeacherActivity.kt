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
import com.example.asistenciaqr.databinding.ActivityEditTeacherBinding
import com.example.asistenciaqr.presentation.viewmodel.TeacherViewModel
import com.example.asistenciaqr.util.TeacherViewModelFactory
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.UCrop
import java.io.ByteArrayOutputStream
import java.io.File

class EditTeacherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditTeacherBinding
    private var photoBase64: String? = null
    private lateinit var viewModel: TeacherViewModel
    private lateinit var currentTeacher: User

    // Contracts para manejar fotos
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
                val tempUri = saveBitmapToTempUri(it)
                startCropActivity(tempUri)
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            startCropActivity(it)
        }
    }

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

        binding = ActivityEditTeacherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar ViewModel
        val factory = TeacherViewModelFactory()
        viewModel = ViewModelProvider(this, factory)[TeacherViewModel::class.java]

        // Obtener el profesor de los extras
        currentTeacher = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("teacher", User::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("teacher")
        } ?: run {
            showError("Error: No se encontró información del profesor")
            finish()
            return
        }

        setStatusBarColor()
        setupToolbar()
        setupUI()
        setupObservers()
        setupListeners()
    }

    private fun setupUI() {
        // Rellenar los campos con los datos actuales del profesor
        binding.etNames.setText(currentTeacher.names)
        binding.etLastnames.setText(currentTeacher.lastnames)
        binding.switchAdmin.isChecked = currentTeacher.admin
        binding.switchActive.isChecked = currentTeacher.active

        // Cargar foto existente si existe
        currentTeacher.photoBase64?.let { base64 ->
            photoBase64 = base64
            loadBase64Image(base64)
        }
    }

    private fun loadBase64Image(base64: String) {
        try {
            val imageBytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            binding.ivPhotoPreview.setImageBitmap(bitmap)
        } catch (e: Exception) {
            showError("Error al cargar la imagen: ${e.message}")
            // Mostrar placeholder si hay error
            binding.ivPhotoPreview.setImageResource(R.drawable.ic_person)
        }
    }

    private fun setupObservers() {
        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let { showError(it) }
        }

        viewModel.updateCompleted.observe(this) { completed ->
            if (completed) {
                showSuccess("Profesor actualizado correctamente")
                binding.root.postDelayed({
                    setResult(RESULT_OK)
                    finish()
                }, 1000)
            }
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.btnSave.isEnabled = !isLoading

            updatePhotoButtonAppearance(isLoading)

            if (isLoading) {
                binding.btnSave.text = getString(R.string.saving)
            } else {
                binding.btnSave.text = getString(R.string.save_changes)
            }
        }
    }

    private fun updatePhotoButtonAppearance(isLoading: Boolean) {
        if (isLoading) {
            binding.btnEditPhoto.alpha = 0.6f
            binding.btnEditPhoto.isClickable = false
        } else {
            // Restaurar apariencia normal
            binding.btnEditPhoto.alpha = 1.0f
            binding.btnEditPhoto.isClickable = true
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
        binding.btnSave.setOnClickListener {
            if (validateForm()) {
                updateTeacher()
            }
        }

        binding.btnEditPhoto.setOnClickListener {
            if (viewModel.loading.value != true) {
                showPhotoOptionsDialog()
            }
        }

        binding.ivPhotoPreview.setOnClickListener {
            if (viewModel.loading.value != true) {
                showRemovePhotoDialog()
            }
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
                binding.ivPhotoPreview.setImageResource(R.drawable.ic_person)
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
        val options = UCrop.Options()
        options.setToolbarColor(ContextCompat.getColor(this, R.color.purple_500))
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.purple_700))
        options.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.white))

        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))

        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(500, 500)
            .withOptions(options)

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

    private fun validateForm(): Boolean {
        val names = binding.etNames.text.toString().trim()
        val lastnames = binding.etLastnames.text.toString().trim()

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

        return isValid
    }

    private fun updateTeacher() {
        val names = binding.etNames.text.toString().trim()
        val lastnames = binding.etLastnames.text.toString().trim()
        val isAdmin = binding.switchAdmin.isChecked
        val isActive = binding.switchActive.isChecked

        val updatedTeacher = currentTeacher.copy(
            names = names,
            lastnames = lastnames,
            admin = isAdmin,
            active = isActive,
            photoBase64 = photoBase64 ?: currentTeacher.photoBase64 // Mantener foto actual si no se cambió
        )

        viewModel.updateTeacher(updatedTeacher)
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.resetUpdateCompleted()
    }
}