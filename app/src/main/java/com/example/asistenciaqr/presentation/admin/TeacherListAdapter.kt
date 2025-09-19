package com.example.asistenciaqr.presentation.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.asistenciaqr.R
import com.example.asistenciaqr.data.model.User

class TeacherListAdapter(
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : ListAdapter<User, TeacherListAdapter.TeacherViewHolder>(TeacherDiffCallback()) {

    inner class TeacherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvTeacherName)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvTeacherEmail)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvTeacherStatus)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(teacher: User) {
            val context = itemView.context

            tvName.text = context.getString(R.string.teacher_full_name, teacher.names, teacher.lastnames)
            tvEmail.text = teacher.email

            if (teacher.active) {
                tvStatus.text = context.getString(R.string.status_active)
                tvStatus.setBackgroundResource(R.drawable.bg_status_active)
            } else {
                tvStatus.text = context.getString(R.string.status_inactive)
                tvStatus.setBackgroundResource(R.drawable.bg_status_inactive)
            }

            btnEdit.setOnClickListener { onEditClick(teacher) }
            btnDelete.setOnClickListener { onDeleteClick(teacher) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_teacher, parent, false)
        return TeacherViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeacherViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Clase DiffUtil para comparar eficientemente las listas
    class TeacherDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}