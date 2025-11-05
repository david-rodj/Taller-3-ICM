package com.example.taller3icm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taller3icm.data.repository.AuthRepository
import com.example.taller3icm.presentation.state.RegisterUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNombreChanged(value: String) {
        _uiState.update { it.copy(
            nombre = value,
            errorMessage = null,
            isRegisterEnabled = RegisterUiState.isValidForm(
                value, it.identificacion, it.email, it.password, it.telefono
            )
        )}
    }

    fun onIdentificacionChanged(value: String) {
        _uiState.update { it.copy(
            identificacion = value,
            errorMessage = null,
            isRegisterEnabled = RegisterUiState.isValidForm(
                it.nombre, value, it.email, it.password, it.telefono
            )
        )}
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(
            email = value,
            errorMessage = null,
            isRegisterEnabled = RegisterUiState.isValidForm(
                it.nombre, it.identificacion, value, it.password, it.telefono
            )
        )}
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(
            password = value,
            errorMessage = null,
            isRegisterEnabled = RegisterUiState.isValidForm(
                it.nombre, it.identificacion, it.email, value, it.telefono
            )
        )}
    }

    fun onTelefonoChanged(value: String) {
        _uiState.update { it.copy(
            telefono = value,
            errorMessage = null,
            isRegisterEnabled = RegisterUiState.isValidForm(
                it.nombre, it.identificacion, it.email, it.password, value
            )
        )}
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(showPassword = !it.showPassword) }
    }

    fun onRegisterClick(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.register(
                nombre = state.nombre,
                identificacion = state.identificacion,
                email = state.email,
                password = state.password,
                telefono = state.telefono
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al registrar"
                    )}
                }
            )
        }
    }
}