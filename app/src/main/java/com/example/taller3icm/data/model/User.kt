package com.example.taller3icm.data.model

data class User(
    val uid: String = "",
    val nombre: String = "",
    val identificacion: String = "",
    val email: String = "",
    val telefono: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val conectado: Boolean = false
)