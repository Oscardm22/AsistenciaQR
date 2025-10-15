package com.example.asistenciaqr.presentation.admin

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
import com.google.zxing.EncodeHintType
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
import androidx.core.view.WindowCompat

class GenerateQrActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGenerateQrBinding
    private lateinit var teacher: User
    private var userPhotoBitmap: Bitmap? = null
    private var qrBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenerateQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        getTeacherData()
        setupUI()
        setupListeners()
        loadPhotoFromBase64()
        generateQRCode()
        displayQRPreview()
        setStatusBarColor()
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
        // Previsualización del carnet FRONTAL - CON ETIQUETAS
        binding.tvCarnetNames.text = teacher.names
        binding.tvCarnetLastnames.text = teacher.lastnames

        val currentDate = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date())
        binding.tvIssueDate.text = getString(R.string.issued_date_format, currentDate)

        // Información adicional
        binding.tvTeacherName.text = getString(R.string.full_name_format, teacher.names, teacher.lastnames)
        binding.tvTeacherEmail.text = getString(R.string.email_format, teacher.email)
        binding.tvTeacherId.text = getString(R.string.id_format, teacher.uid.substring(0, 8).uppercase())

        binding.btnGeneratePdf.text = getString(R.string.generate_pdf_id_card)
    }

    private fun setupListeners() {
        binding.btnGeneratePdf.setOnClickListener {
            if (userPhotoBitmap == null) {
                showError("No hay foto disponible para generar el carnet")
                return@setOnClickListener
            }
            generateCarnetPdf()
        }
    }

    private fun setStatusBarColor() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_500)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }
    }

    private fun generateQRCode() {
        val qrData = "TEACHER:${teacher.uid}:${teacher.email}"
        qrBitmap = generateQRBitmap(qrData, 300)
    }

    private fun displayQRPreview() {
        qrBitmap?.let { bitmap ->
            binding.ivQrCode.setImageBitmap(bitmap)
        } ?: run {
            showError("No se pudo generar el código QR")
        }
    }

    private fun generateQRBitmap(text: String, size: Int): Bitmap {
        return try {
            // Configurar para generar QR sin márgenes
            val hints = mutableMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 0 // Sin márgenes

            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
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

    private fun loadPhotoFromBase64() {
        if (!teacher.photoBase64.isNullOrEmpty()) {
            try {
                userPhotoBitmap = decodeBase64ToBitmap(teacher.photoBase64!!)

                if (userPhotoBitmap != null) {
                    val targetSize = 800
                    userPhotoBitmap = scaleBitmapProportionally(userPhotoBitmap!!, targetSize)

                    // Hacer la foto CIRCULAR para la previsualización
                    val circularBitmap = getCircularBitmap(userPhotoBitmap!!, 300)
                    binding.ivUserPhoto.setImageBitmap(circularBitmap)
                } else {
                    userPhotoBitmap = createUserPhotoPlaceholder()
                    binding.ivUserPhoto.setImageBitmap(userPhotoBitmap)
                }
            } catch (e: Exception) {
                userPhotoBitmap = createUserPhotoPlaceholder()
                binding.ivUserPhoto.setImageBitmap(userPhotoBitmap)
            }
        } else {
            userPhotoBitmap = createUserPhotoPlaceholder()
            binding.ivUserPhoto.setImageBitmap(userPhotoBitmap)
        }
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val cleanBase64 = if (base64String.contains(",")) {
                base64String.substring(base64String.indexOf(",") + 1)
            } else {
                base64String
            }
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleBitmapProportionally(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val scaleFactor = if (width > height) {
            maxSize.toFloat() / width
        } else {
            maxSize.toFloat() / height
        }

        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()

        return bitmap.scale(newWidth, newHeight)
    }

    private fun createUserPhotoPlaceholder(): Bitmap {
        val size = 300
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = true }

        paint.color = ContextCompat.getColor(this, R.color.purple_500)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        paint.color = Color.WHITE
        paint.textSize = size / 3f  // Reducir tamaño del emoji
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("👤", size / 2f, size / 1.7f, paint)

        return bitmap
    }

    private fun createErrorBitmap(size: Int): Bitmap {
        val bitmap = createBitmap(size, size, Bitmap.Config.RGB_565)
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

    private fun generateCarnetPdf() {
        try {
            val pdfDocument = PdfDocument()

            // Tamaño carnet: 5.1cm x 8.4cm ≈ 145x238 puntos
            val cardWidth = 145f
            val cardHeight = 238f
            val cornerRadius = 12f // Bordes redondeados

            // Página carta
            val pageWidth = 612
            val pageHeight = 792

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            drawCarnetPage(canvas, pageWidth, pageHeight, cardWidth, cardHeight, cornerRadius)
            pdfDocument.finishPage(page)

            val file = createPdfFile()
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            showSuccess("Carnet generado exitosamente")
            sharePdfFile(file)

        } catch (e: Exception) {
            showError("Error al generar carnet: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun drawCarnetPage(canvas: Canvas, pageWidth: Int, pageHeight: Int, cardWidth: Float, cardHeight: Float, cornerRadius: Float) {
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        // Fondo blanco
        canvas.drawColor(Color.WHITE)

        // REDUCIR margen entre carnets (más juntos)
        val margin = 20f // Antes era 72f

        // Calcular posición - más centrados y juntos
        val totalWidth = cardWidth * 2 + margin
        val startX = (pageWidth - totalWidth) / 2f
        val startY = (pageHeight - cardHeight) / 2f

        // Carnet FRONTAL (izquierda) con bordes redondeados
        drawCarnetFront(canvas, startX, startY, cardWidth, cardHeight, cornerRadius, paint)

        // Carnet REVERSO (derecha) con bordes redondeados
        drawCarnetBack(canvas, startX + cardWidth + margin, startY, cardWidth, cardHeight, cornerRadius, paint)

        // Instrucciones de corte para doblar (no recortar separado)
        paint.color = Color.LTGRAY
        paint.textSize = 8f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Recortar y doblar por el centro", pageWidth / 2f, pageHeight - 30f, paint)

        // Línea punteada central para doblar
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 5f), 0f)
        val centerX = startX + cardWidth + margin / 2
        canvas.drawLine(centerX, startY - 10f, centerX, startY + cardHeight + 10f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawCarnetFront(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, cornerRadius: Float, paint: Paint) {
        // Fondo del carnet con bordes redondeados
        val rect = RectF(x, y, x + width, y + height)
        paint.color = Color.WHITE
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        // Borde redondeado
        paint.color = ContextCompat.getColor(this, R.color.purple_500)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        paint.style = Paint.Style.FILL

        // Header con bordes redondeados superiores (más pequeño)
        paint.color = ContextCompat.getColor(this, R.color.purple_500)
        val headerRect = RectF(x, y, x + width, y + height * 0.12f) // Reducido de 0.15f a 0.12f
        canvas.drawRoundRect(
            RectF(headerRect.left, headerRect.top, headerRect.right, headerRect.bottom + cornerRadius),
            cornerRadius,
            cornerRadius,
            paint
        )

        // Título MÁS GRANDE
        paint.color = Color.WHITE
        paint.textSize = 8f // Aumentado de 7f a 8f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD // Título en negrita
        canvas.drawText("DOCENTE", x + width / 2, y + height * 0.07f, paint)

        // FOTO CIRCULAR con más espacio superior
        userPhotoBitmap?.let { photoBitmap ->
            val photoDiameter = height * 0.5f // Diámetro del círculo
            val photoX = x + (width - photoDiameter) / 2
            val photoY = y + height * 0.20f // MISMA POSICIÓN QUE EL QR (0.20f)

            // Crear un bitmap circular
            val circularBitmap = getCircularBitmap(photoBitmap, photoDiameter.toInt())
            canvas.drawBitmap(circularBitmap, photoX, photoY, paint)
        }

        // Información con texto MÁS GRANDE y NEGRITAS
        paint.color = Color.BLACK
        paint.textSize = 6.5f // Aumentado de 5.5f a 6.5f
        paint.textAlign = Paint.Align.CENTER

        var currentY = y + height * 0.78f

        // NOMBRES Y APELLIDOS CON ETIQUETAS EN NEGRITA
        val boldPaint = Paint(paint).apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD // NEGRITA
        }
        val regularPaint = Paint(paint).apply {
            typeface = android.graphics.Typeface.DEFAULT // NORMAL
        }

        // Nombres con etiqueta en negrita
        val namesLabel = "Nombres: "

        // Medir ancho del texto para centrado preciso
        val namesLabelWidth = boldPaint.measureText(namesLabel)
        val namesValueWidth = regularPaint.measureText(teacher.names)
        val totalNamesWidth = namesLabelWidth + namesValueWidth

        if (totalNamesWidth <= width - 20f) {
            // Dibujar en una línea: etiqueta en negrita + valor normal
            val startX = x + (width - totalNamesWidth) / 2

            canvas.drawText(namesLabel, startX + namesLabelWidth / 2, currentY, boldPaint)
            canvas.drawText(teacher.names, startX + namesLabelWidth + namesValueWidth / 2, currentY, regularPaint)
            currentY += 7f // Aumentado de 6f a 7f
        } else {
            // Múltiples líneas
            canvas.drawText(namesLabel, x + width / 2, currentY, boldPaint)
            currentY += 7f

            val namesLines = splitTextIntoLines(teacher.names, width - 20f, regularPaint)
            namesLines.forEach { line ->
                canvas.drawText(line, x + width / 2, currentY, regularPaint)
                currentY += 7f
            }
        }

        currentY += 3f // Espacio entre nombres y apellidos

        // Apellidos con etiqueta en negrita
        val lastnamesLabel = "Apellidos: "

        val lastnamesLabelWidth = boldPaint.measureText(lastnamesLabel)
        val lastnamesValueWidth = regularPaint.measureText(teacher.lastnames)
        val totalLastnamesWidth = lastnamesLabelWidth + lastnamesValueWidth

        if (totalLastnamesWidth <= width - 20f) {
            val startX = x + (width - totalLastnamesWidth) / 2

            canvas.drawText(lastnamesLabel, startX + lastnamesLabelWidth / 2, currentY, boldPaint)
            canvas.drawText(teacher.lastnames, startX + lastnamesLabelWidth + lastnamesValueWidth / 2, currentY, regularPaint)
        } else {
            canvas.drawText(lastnamesLabel, x + width / 2, currentY, boldPaint)
            currentY += 7f

            val lastnamesLines = splitTextIntoLines(teacher.lastnames, width - 20f, regularPaint)
            lastnamesLines.forEach { line ->
                canvas.drawText(line, x + width / 2, currentY, regularPaint)
                currentY += 7f
            }
        }
    }

    private fun drawCarnetBack(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, cornerRadius: Float, paint: Paint) {
        // Fondo y borde redondeados
        val rect = RectF(x, y, x + width, y + height)
        paint.color = Color.WHITE
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        paint.color = ContextCompat.getColor(this, R.color.purple_500)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        paint.style = Paint.Style.FILL

        // Header redondeado (más pequeño)
        paint.color = ContextCompat.getColor(this, R.color.purple_500)
        val headerRect = RectF(x, y, x + width, y + height * 0.10f) // Reducido de 0.12f a 0.10f
        canvas.drawRoundRect(
            RectF(headerRect.left, headerRect.top, headerRect.right, headerRect.bottom + cornerRadius),
            cornerRadius,
            cornerRadius,
            paint
        )

        // Título reverso MÁS GRANDE
        paint.color = Color.WHITE
        paint.textSize = 7f // Aumentado de 6f a 7f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD // Título en negrita
        canvas.drawText("INFORMACIÓN", x + width / 2, y + height * 0.06f, paint)

        // QR CODE con más espacio superior
        qrBitmap?.let { bitmap ->
            val qrSize = height * 0.5f
            val qrX = x + (width - qrSize) / 2
            val qrY = y + height * 0.20f // MISMA POSICIÓN QUE LA FOTO (0.20f)

            val scaledQr = bitmap.scale(qrSize.toInt(), qrSize.toInt())
            canvas.drawBitmap(scaledQr, qrX, qrY, paint)
        }

        // FECHA MÁS GRANDE
        paint.color = Color.BLACK
        paint.textSize = 6f // Aumentado de 5f a 6f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD // Fecha en negrita

        val currentY = y + height * 0.75f
        canvas.drawText(
            "Emitido: ${SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date())}",
            x + width / 2,
            currentY,
            paint
        )
    }

    private fun getCircularBitmap(bitmap: Bitmap, diameter: Int): Bitmap {
        // Escalar el bitmap al diámetro deseado
        val scaledBitmap = bitmap.scale(diameter, diameter, false)

        // Crear un bitmap circular
        val output = createBitmap(diameter, diameter)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        // Dibujar un círculo y usar como máscara
        val circlePath = Path().apply {
            addCircle(diameter / 2f, diameter / 2f, diameter / 2f, Path.Direction.CW)
        }

        canvas.drawPath(circlePath, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)

        return output
    }

    private fun splitTextIntoLines(text: String, maxWidth: Float, paint: Paint): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }

    private fun createPdfFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "carnet_${teacher.names.replace(" ", "_")}_$timeStamp.pdf"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        return File(storageDir, fileName)
    }

    private fun sharePdfFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Carnet - ${teacher.names} ${teacher.lastnames}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir Carnet"))
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        userPhotoBitmap?.recycle()
        qrBitmap?.recycle()
    }
}