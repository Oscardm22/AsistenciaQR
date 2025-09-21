package com.example.asistenciaqr.presentation.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.asistenciaqr.databinding.ActivityManageTeachersBinding
import com.example.asistenciaqr.presentation.viewmodel.TeacherViewModel
import com.example.asistenciaqr.util.TeacherViewModelFactory
import com.example.asistenciaqr.R
import com.example.asistenciaqr.data.model.User

class ManageTeachersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageTeachersBinding
    private lateinit var viewModel: TeacherViewModel
    private lateinit var adapter: TeacherListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageTeachersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setStatusBarColor()
        setupToolbar()
        setupViewModel()
        setupRecyclerView()
        setupObservers()
        setupListeners()

        viewModel.loadTeachers()
    }

    private fun setStatusBarColor() {
        // Cambiar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_500)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupViewModel() {
        val factory = TeacherViewModelFactory()
        viewModel = ViewModelProvider(this, factory)[TeacherViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = TeacherListAdapter(
            onEditClick = { teacher ->
                openEditTeacher(teacher)
            },
            onDeleteClick = { teacher ->
                showDeleteConfirmation(teacher)
            }
        )

        binding.rvTeachers.layoutManager = LinearLayoutManager(this)
        binding.rvTeachers.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.teachers.observe(this) { teachers ->
            adapter.submitList(teachers)
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                showError(errorMessage)
            }
        }
    }

    private fun setupListeners() {
        binding.fabAddTeacher.setOnClickListener {
            val intent = Intent(this, AddTeacherActivity::class.java)
            startActivity(intent)
        }
    }

    private fun openEditTeacher(teacher: User) {
        val intent = Intent(this, EditTeacherActivity::class.java).apply {
            putExtra("teacher", teacher)
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmation(teacher: User) {
        // No permitir eliminar administradores
        if (teacher.admin) {
            showError("No se puede eliminar un administrador")
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this, R.style.ThemeOverlay_AsistenciaQR_AlertDialog_Delete)
            .setTitle("Confirmar desactivación")
            .setMessage("¿Estás seguro de que quieres desactivar a ${teacher.names} ${teacher.lastnames}? El usuario no podrá iniciar sesión.")
            .setPositiveButton("Desactivar") { dialog, which ->
                viewModel.deleteTeacher(teacher.uid)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadTeachers() // Recargar datos al volver a la actividad
    }
}