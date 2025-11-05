package com.example.taller3icm.presentation.state

data class RegisterUiState(
    val nombre: String = "",
    val identificacion: String = "",
    val email: String = "",
    val password: String = "",
    val telefono: String = "",
    val showPassword: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegisterEnabled: Boolean = false
) {
    companion object {
        fun isValidForm(
            nombre: String,
            identificacion: String,
            email: String,
            password: String,
            telefono: String
        ): Boolean {
            return nombre.isNotBlank() &&
                    identificacion.isNotBlank() &&
                    email.contains("@") &&
                    password.length >= 8 &&
                    telefono.isNotBlank()
        }
    }
}
