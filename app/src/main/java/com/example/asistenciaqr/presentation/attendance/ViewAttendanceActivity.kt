package com.example.asistenciaqr.presentation.attendance

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.asistenciaqr.databinding.ActivityViewAttendanceBinding

class ViewAttendanceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewAttendanceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Implementación para ver asistencias
    }
}