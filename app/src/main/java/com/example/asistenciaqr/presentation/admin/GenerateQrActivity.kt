package com.example.asistenciaqr.presentation.admin

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.asistenciaqr.R
import com.example.asistenciaqr.databinding.ActivityGenerateQrBinding
import com.example.asistenciaqr.data.model.User
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.core.graphics.scale

class GenerateQrActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGenerateQrBinding
    private lateinit var teacher: User
    private var qrBitmap: Bitmap? = null
    private var userPhotoBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenerateQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        getTeacherData()
        setupUI()
        generateQRCode()
        loadUserPhotoFromBase64()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Generar Carnet"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun getTeacherData() {
        teacher = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("teacher", User::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("teacher")!!
        }
    }

    private fun setupUI() {
        binding.tvTeacherName.text = "${teacher.names} ${teacher.lastnames}"
        binding.tvTeacherEmail.text = teacher.email
        binding.tvTeacherId.text = "ID: ${teacher.uid.substring(0, 8).uppercase()}"
    }

    private fun generateQRCode() {
        val qrData = "TEACHER:${teacher.uid}:${teacher.email}"
        qrBitmap = generateQRBitmap(qrData, 400)
        binding.ivQrCode.setImageBitmap(qrBitmap)
    }

    private fun generateQRBitmap(text: String, size: Int): Bitmap {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size
            )
            val bitmap = createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            bitmap
        } catch (e: Exception) {
            createErrorBitmap(size)
        }
    }

    private fun loadUserPhotoFromBase64() {
        if (!teacher.photoBase64.isNullOrEmpty()) {
            try {
                // Decodificar Base64 a Bitmap
                userPhotoBitmap = decodeBase64ToBitmap(teacher.photoBase64!!)

                if (userPhotoBitmap == null) {
                    showError("No se pudo decodificar la foto Base64")
                    userPhotoBitmap = createUserPhotoPlaceholder()
                }
                // Eliminamos el showSuccess que mostraba "Foto cargada desde base64"
            } catch (e: Exception) {
                showError("Error decodificando Base64: ${e.message}")
                userPhotoBitmap = createUserPhotoPlaceholder()
            }
        } else {
            // Si no hay foto en Base64, usar placeholder con iniciales
            showError("No hay foto registrada para este usuario")
            userPhotoBitmap = createUserPhotoPlaceholder()
        }
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            // Limpiar el string Base64 (remover prefix si existe)
            val cleanBase64 = if (base64String.contains(",")) {
                base64String.substring(base64String.indexOf(",") + 1)
            } else {
                base64String
            }

            // Decodificar Base64
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)

            // Convertir a Bitmap
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun createUserPhotoPlaceholder(): Bitmap {
        val size = 300
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Fondo circular con color basado en el nombre
        val color = generateColorFromName(teacher.names + teacher.lastnames)
        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Iniciales del usuario
        val initials = getInitials(teacher.names, teacher.lastnames)
        paint.color = Color.WHITE
        paint.textSize = size / 3f
        paint.textAlign = Paint.Align.CENTER

        val textBounds = Paint()
        textBounds.textSize = paint.textSize
        val yPos = size / 2f - (textBounds.descent() + textBounds.ascent()) / 2f

        canvas.drawText(initials, size / 2f, yPos, paint)

        return bitmap
    }

    private fun generateColorFromName(name: String): Int {
        val hash = name.hashCode()
        return Color.HSVToColor(floatArrayOf(
            (hash % 360).toFloat(),
            0.7f,
            0.8f
        ))
    }

    private fun getInitials(names: String, lastnames: String): String {
        val nameInitial = names.takeIf { it.isNotEmpty() }?.first()?.uppercase() ?: "U"
        val lastnameInitial = lastnames.takeIf { it.isNotEmpty() }?.first()?.uppercase() ?: "S"
        return "$nameInitial$lastnameInitial"
    }

    private fun createErrorBitmap(size: Int): Bitmap {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.RED)
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ERROR", size / 2f, size / 2f, paint)
        return bitmap
    }

    private fun setupListeners() {
        binding.btnGeneratePdf.setOnClickListener {
            if (userPhotoBitmap == null) {
                showError("La foto no está disponible")
                return@setOnClickListener
            }
            generatePdfCarnet()
        }

        binding.btnSharePdf.setOnClickListener {
            qrBitmap?.let { bitmap ->
                shareQRCode(bitmap)
            } ?: showError("Primero genera el código QR")
        }
    }

    private fun generatePdfCarnet() {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            // Página 1 - Frente del carnet
            val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page1 = pdfDocument.startPage(pageInfo1)
            drawFrontPage(page1.canvas, pageWidth, pageHeight)
            pdfDocument.finishPage(page1)

            // Página 2 - Reverso del carnet
            val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
            val page2 = pdfDocument.startPage(pageInfo2)
            drawBackPage(page2.canvas, pageWidth, pageHeight)
            pdfDocument.finishPage(page2)

            // Guardar el PDF
            val file = createPdfFile()
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            showSuccess("Carnet generado exitosamente")
            sharePdfFile(file)

        } catch (e: Exception) {
            showError("Error al generar PDF: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun drawFrontPage(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Fondo blanco
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Header con color institucional
        paint.color = ContextCompat.getColor(this, R.color.purple_500)
        canvas.drawRect(0f, 0f, width.toFloat(), 120f, paint)

        // Título
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("CARNET DOCENTE", width / 2f, 70f, paint)

        // Logo o icono
        paint.textSize = 40f
        canvas.drawText("🏫", width / 2f, 40f, paint)

        // Foto del profesor (DESDE BASE64)
        userPhotoBitmap?.let { photoBitmap ->
            val photoSize = 120f
            val photoLeft = (width - photoSize) / 2f
            val photoTop = 150f

            // Crear bitmap circular para la foto
            val circularPhoto = createCircularBitmap(photoBitmap, photoSize.toInt())
            canvas.drawBitmap(circularPhoto, photoLeft, photoTop, paint)
        }

        // Información del profesor
        paint.color = Color.BLACK
        paint.textSize = 14f
        paint.textAlign = Paint.Align.LEFT

        val infoStartY = 300f
        var currentY = infoStartY

        val maxWidth = width - 100f

        // Dibujar texto con ajuste de línea
        drawTextWithWrap(canvas, "Nombre: ${teacher.names} ${teacher.lastnames}",
            50f, currentY, maxWidth, paint)
        currentY += 22f

        drawTextWithWrap(canvas, "Email: ${teacher.email}", 50f, currentY, maxWidth, paint)
        currentY += 22f

        drawTextWithWrap(canvas, "ID: ${teacher.uid.substring(0, 8).uppercase()}",
            50f, currentY, maxWidth, paint)
        currentY += 22f

        drawTextWithWrap(canvas, "Tipo: ${if (teacher.admin) "Administrador" else "Docente"}",
            50f, currentY, maxWidth, paint)

        // QR Code
        qrBitmap?.let { bitmap ->
            val qrSize = 100f
            val qrLeft = (width - qrSize) / 2f
            val qrTop = height - 180f

            val scaledBitmap = bitmap.scale(qrSize.toInt(), qrSize.toInt(), false)
            canvas.drawBitmap(scaledBitmap, qrLeft, qrTop, paint)
        }

        // Texto bajo el QR
        paint.textSize = 10f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Escanear para verificar", width / 2f, height - 70f, paint)
    }

    private fun drawTextWithWrap(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint) {
        val words = text.split(" ")
        var line = ""

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            val testWidth = paint.measureText(testLine)

            if (testWidth <= maxWidth) {
                line = testLine
            } else {
                canvas.drawText(line, x, y, paint)
                line = word
            }
        }
        canvas.drawText(line, x, y, paint)
    }

    private fun createCircularBitmap(bitmap: Bitmap, diameter: Int): Bitmap {
        val output = createBitmap(diameter, diameter)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Dibujar círculo de fondo blanco
        paint.color = Color.WHITE
        canvas.drawCircle(diameter / 2f, diameter / 2f, diameter / 2f, paint)

        // Usar el bitmap original como máscara
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)

        // Escalar el bitmap al tamaño del círculo
        val scaledBitmap = bitmap.scale(diameter, diameter, false)
        canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)

        return output
    }

    private fun drawBackPage(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Fondo blanco
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Header
        paint.color = ContextCompat.getColor(this, R.color.purple_500)
        canvas.drawRect(0f, 0f, width.toFloat(), 80f, paint)

        // Título reverso
        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("INFORMACIÓN ADICIONAL", width / 2f, 50f, paint)

        // Contenido informativo
        paint.color = Color.BLACK
        paint.textSize = 12f
        paint.textAlign = Paint.Align.LEFT

        var currentY = 150f
        val margin = 40f
        val lineSpacing = 20f

        val instructions = listOf(
            "• Este carnet identifica al docente ante la institución",
            "• Presentar al ingresar a las instalaciones",
            "• El código QR contiene información de verificación",
            "• En caso de pérdida, reportar inmediatamente",
            "• Válido durante el año académico vigente",
            "",
            "Fecha de emisión: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}",
            "",
            "Firma del director: ___________________"
        )

        instructions.forEach { line ->
            canvas.drawText(line, margin, currentY, paint)
            currentY += lineSpacing
        }

        // Código de barras numérico (simulado)
        paint.textSize = 10f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "CÓDIGO: ${teacher.uid.replace("-", "").take(16).uppercase()}",
            width / 2f, height - 100f, paint
        )

        // Línea para firma del docente
        paint.textSize = 10f
        canvas.drawText("Firma del docente: ___________________", width / 2f, height - 50f, paint)
    }

    private fun createPdfFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "carnet_${teacher.names.replace(" ", "_")}_$timeStamp.pdf"

        val storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        return File(storageDir, fileName)
    }

    private fun sharePdfFile(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Carnet Docente - ${teacher.names} ${teacher.lastnames}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Compartir Carnet"))
    }

    private fun shareQRCode(bitmap: Bitmap) {
        val file = File(cacheDir, "qr_${teacher.uid}.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "QR Code - ${teacher.names}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Compartir QR Code"))
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Liberar recursos
        qrBitmap?.recycle()
        userPhotoBitmap?.recycle()
    }
}