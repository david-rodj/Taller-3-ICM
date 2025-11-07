package com.example.taller3icm.presentation.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taller3icm.data.repository.AuthRepository
import com.example.taller3icm.data.repository.UserRepository
import com.example.taller3icm.presentation.state.MapUiState
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _userPath = mutableListOf<LatLng>()
    private val _otherUsersPaths = mutableMapOf<String, MutableList<LatLng>>()

    //  NUEVO: Guardamos las posiciones anteriores para detectar movimientos
    private val _previousPositions = mutableMapOf<String, LatLng>()

    init {
        loadCurrentUser()
        observeOnlineUsers()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _uiState.update { it.copy(currentUser = user) }
        }
    }

    // MODIFICADO: Ahora detecta movimientos y actualiza polylines
    private fun observeOnlineUsers() {
        viewModelScope.launch {
            userRepository.getOnlineUsersFlow()
                .collect { users ->
                    val currentUid = authRepository.currentUser?.uid
                    val filteredUsers = users.filter { it.uid != currentUid }

                    // Procesar cada usuario en línea
                    filteredUsers.forEach { user ->
                        val currentPos = LatLng(user.latitud, user.longitud)

                        // Solo actualizar si la posición cambió (evita polylines duplicadas)
                        if (user.latitud != 0.0 && user.longitud != 0.0) {
                            val previousPos = _previousPositions[user.uid]

                            // Si es una nueva posición o cambió
                            if (previousPos == null || previousPos != currentPos) {
                                // Crear lista si no existe
                                if (!_otherUsersPaths.containsKey(user.uid)) {
                                    _otherUsersPaths[user.uid] = mutableListOf()
                                }

                                // Agregar nuevo punto a la ruta
                                _otherUsersPaths[user.uid]?.add(currentPos)

                                // Guardar posición actual como anterior
                                _previousPositions[user.uid] = currentPos
                            }
                        }
                    }

                    // NUEVO: Limpiar rutas de usuarios desconectados
                    val onlineUids = filteredUsers.map { it.uid }.toSet()
                    val disconnectedUsers = _otherUsersPaths.keys - onlineUids

                    disconnectedUsers.forEach { uid ->
                        _otherUsersPaths.remove(uid)
                        _previousPositions.remove(uid)
                    }

                    // Actualizar estado con usuarios y rutas
                    _uiState.update { state ->
                        state.copy(
                            onlineUsers = filteredUsers,
                            otherUsersPaths = _otherUsersPaths.mapValues { it.value.toList() }
                        )
                    }
                }
        }
    }

    fun onLocationToggle(enabled: Boolean) {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid ?: return@launch

            if (!enabled) {
                // Limpiar la ruta al desconectar
                _userPath.clear()
                _uiState.update { it.copy(userPath = emptyList()) }
            }

            userRepository.updateConnectionStatus(uid, enabled)
            _uiState.update { it.copy(isLocationEnabled = enabled) }
        }
    }

    fun onLocationUpdate(location: Location) {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid ?: return@launch
            val latLng = LatLng(location.latitude, location.longitude)

            // Actualizar en Firestore (otros usuarios lo verán)
            userRepository.updateUserLocation(uid, location.latitude, location.longitude)

            // Agregar a la ruta local
            _userPath.add(latLng)
            _uiState.update { it.copy(userPath = _userPath.toList()) }
        }
    }

    fun onLogout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid
            if (uid != null) {
                // Desconectar antes de cerrar sesión
                userRepository.updateConnectionStatus(uid, false)
            }
            authRepository.logout()
            onSuccess()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Limpiar recursos
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid
            if (uid != null) {
                userRepository.updateConnectionStatus(uid, false)
            }
        }
    }
}