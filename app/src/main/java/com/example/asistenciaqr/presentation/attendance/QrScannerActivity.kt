package com.example.asistenciaqr.presentation.attendance

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.asistenciaqr.databinding.ActivityQrScannerBinding

class QrScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQrScannerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Implementación del escáner QR
    }
}