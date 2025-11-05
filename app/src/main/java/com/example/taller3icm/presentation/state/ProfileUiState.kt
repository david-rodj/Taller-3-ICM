package com.example.taller3icm.presentation.state

import com.example.taller3icm.data.model.User

data class ProfileUiState(
    val user: User? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
