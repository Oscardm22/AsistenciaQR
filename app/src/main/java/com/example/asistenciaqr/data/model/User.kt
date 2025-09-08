package com.example.asistenciaqr.data.model

import java.util.Date

data class User(
    val uid: String = "",
    val email: String = "",
    val names: String = "",
    val lastnames: String = "",
    val isAdmin: Boolean = false,
    val createdAt: Date = Date()
)