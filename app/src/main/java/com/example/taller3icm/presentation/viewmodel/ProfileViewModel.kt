package com.example.taller3icm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taller3icm.data.repository.AuthRepository
import com.example.taller3icm.data.repository.UserRepository
import com.example.taller3icm.presentation.state.ProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = authRepository.getCurrentUser()
            _uiState.update { it.copy(user = user, isLoading = false) }
        }
    }

    fun onEditToggle() {
        _uiState.update { it.copy(isEditing = !it.isEditing) }
    }

    fun onSaveProfile(nombre: String, identificacion: String, telefono: String) {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid ?: return@launch
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = userRepository.updateUserProfile(uid, nombre, identificacion, telefono)

            result.fold(
                onSuccess = {
                    loadUserProfile()
                    _uiState.update { it.copy(
                        isLoading = false,
                        isEditing = false,
                        successMessage = "Perfil actualizado"
                    )}
                },
                onFailure = { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al actualizar"
                    )}
                }
            )
        }
    }
}