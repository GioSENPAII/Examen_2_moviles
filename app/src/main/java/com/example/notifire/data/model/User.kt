package com.example.notifire.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "user" // o "admin"
)
