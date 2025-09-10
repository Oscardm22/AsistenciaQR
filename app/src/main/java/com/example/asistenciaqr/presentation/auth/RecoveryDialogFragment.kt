package com.example.asistenciaqr.presentation.auth

import android.app.Dialog
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.asistenciaqr.R
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText

class RecoveryDialogFragment : DialogFragment() {

    var onEmailSubmitted: ((String) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_recovery, null)

        val tilEmail = view.findViewById<TextInputLayout>(R.id.tilRecoveryEmail)
        val etEmail = view.findViewById<TextInputEditText>(R.id.etRecoveryEmail)

        val dialog = AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_AsistenciaQR_AlertDialog)
            .setTitle(R.string.recover_password)
            .setMessage(R.string.hint_enter_email_recovery)
            .setView(view)
            .setPositiveButton(R.string.send, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setOnClickListener {
                val email = etEmail.text.toString().trim()
                if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    tilEmail.error = null
                    onEmailSubmitted?.invoke(email)
                    dialog.dismiss()
                } else {
                    tilEmail.error = getString(R.string.error_invalid_email)
                }
            }

            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        return dialog
    }
}