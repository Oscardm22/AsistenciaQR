package com.example.asistenciaqr.presentation.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.asistenciaqr.R
import com.example.asistenciaqr.data.model.AttendanceRecord
import com.example.asistenciaqr.databinding.ItemAttendanceBinding

class AttendanceAdapter(
    private val showUserNames: Boolean = false
) : ListAdapter<AttendanceRecord, AttendanceAdapter.AttendanceViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val binding = ItemAttendanceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AttendanceViewHolder(binding, showUserNames)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val record = getItem(position)
        holder.bind(record)
    }

    class AttendanceViewHolder(
        private val binding: ItemAttendanceBinding,
        private val showUserNames: Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: AttendanceRecord) {
            if (showUserNames) {
                binding.tvUserName.visibility = android.view.View.VISIBLE
                binding.tvUserName.text = "${record.userNames} ${record.userLastnames}"
            } else {
                binding.tvUserName.visibility = android.view.View.GONE
            }

            binding.tvDateTime.text = record.getFormattedDate()
            binding.tvType.text = record.getTypeInSpanish()

            // MOSTRAR DIRECCIÓN EN LUGAR DE COORDENADAS
            binding.tvLocation.text = if (record.locationAddress.isNotEmpty()) {
                record.locationAddress
            } else {
                // Fallback a coordenadas si no hay dirección
                "Lat: ${String.format("%.4f", record.latitude)}, Lng: ${String.format("%.4f", record.longitude)}"
            }

            // Color diferente según el tipo de registro
            val context = binding.root.context
            val typeColor = when (record.type) {
                com.example.asistenciaqr.data.model.AttendanceType.ENTRY -> R.color.green_600
                com.example.asistenciaqr.data.model.AttendanceType.EXIT -> R.color.red_600
            }
            binding.tvType.setBackgroundColor(ContextCompat.getColor(context, typeColor))
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<AttendanceRecord>() {
        override fun areItemsTheSame(oldItem: AttendanceRecord, newItem: AttendanceRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AttendanceRecord, newItem: AttendanceRecord): Boolean {
            return oldItem == newItem
        }
    }
}