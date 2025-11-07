package com.example.taller3icm.presentation.viewmodel

import android.location.Location
import android.util.Log
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

    private fun observeOnlineUsers() {
        viewModelScope.launch {
            userRepository.getOnlineUsersFlow()
                .collect { users ->
                    val currentUid = authRepository.currentUser?.uid

                    // SOLUCIÓN: Filtro triple para asegurar que solo usuarios conectados aparezcan
                    val filteredUsers = users.filter { user ->
                        user.uid != currentUid &&  // No incluir al usuario actual
                                user.conectado &&           // Debe estar conectado
                                user.latitud != 0.0 &&      // Debe tener coordenadas válidas
                                user.longitud != 0.0
                    }

                    Log.d("MapViewModel", "Usuarios filtrados: ${filteredUsers.size}")

                    // Actualizar rutas de usuarios conectados
                    filteredUsers.forEach { user ->
                        val currentPos = LatLng(user.latitud, user.longitud)
                        val previousPos = _previousPositions[user.uid]

                        if (previousPos == null || previousPos != currentPos) {
                            if (!_otherUsersPaths.containsKey(user.uid)) {
                                _otherUsersPaths[user.uid] = mutableListOf()
                            }
                            _otherUsersPaths[user.uid]?.add(currentPos)
                            _previousPositions[user.uid] = currentPos
                        }
                    }

                    // CRÍTICO: Limpiar usuarios que ya no están conectados
                    val onlineUids = filteredUsers.map { it.uid }.toSet()
                    val disconnectedUsers = _otherUsersPaths.keys - onlineUids

                    Log.d("MapViewModel", "Usuarios desconectados a limpiar: ${disconnectedUsers.size}")

                    disconnectedUsers.forEach { uid ->
                        Log.d("MapViewModel", "Limpiando usuario desconectado: $uid")
                        _otherUsersPaths.remove(uid)
                        _previousPositions.remove(uid)
                    }

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

            // Actualizar en Firestore
            userRepository.updateUserLocation(uid, location.latitude, location.longitude)

            // Agregar a la ruta local
            _userPath.add(latLng)

            // SOLUCIÓN: Actualizar currentUser con las nuevas coordenadas
            _uiState.update { state ->
                state.copy(
                    currentUser = state.currentUser?.copy(
                        latitud = location.latitude,
                        longitud = location.longitude
                    ),
                    userPath = _userPath.toList()
                )
            }
        }
    }

    fun onLogout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid
            if (uid != null) {
                userRepository.updateConnectionStatus(uid, false)
            }
            authRepository.logout()
            onSuccess()
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid
            if (uid != null) {
                userRepository.updateConnectionStatus(uid, false)
            }
        }
    }
}