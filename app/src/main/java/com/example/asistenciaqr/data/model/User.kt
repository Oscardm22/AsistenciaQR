package com.example.asistenciaqr.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class User(
    val uid: String = "",
    val email: String = "",
    val names: String = "",
    val lastnames: String = "",
    val admin: Boolean = false,
    val photoBase64: String? = null,
    val createdAt: Date = Date(),
    val active: Boolean = true
) : Parcelable