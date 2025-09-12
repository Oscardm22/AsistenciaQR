package com.example.asistenciaqr.presentation.admin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.asistenciaqr.databinding.ActivityGenerateQrBinding

class GenerateQrActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGenerateQrBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenerateQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Aquí implementarás la generación de QR
    }
}