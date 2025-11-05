package com.example.taller3icm.presentation.state

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginEnabled: Boolean = false
) {
    companion object {
        fun isValidForm(email: String, password: String): Boolean {
            return email.contains("@") && password.length >= 8
        }
    }
}
