package com.example.asistenciaqr.presentation.admin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.asistenciaqr.databinding.ActivityManageTeachersBinding

class ManageTeachersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageTeachersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageTeachersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Aquí implementarás la gestión de profesores
    }
}