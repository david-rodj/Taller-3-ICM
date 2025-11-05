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
import com.google.firebase.firestore.ListenerRegistration

class MapViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _userPath = mutableListOf<LatLng>()
    private val _otherUsersPaths = mutableMapOf<String, MutableList<LatLng>>()

    private var usersListener: ListenerRegistration? = null

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
                    val filteredUsers = users.filter { it.uid != currentUid }

                    // Actualizar rutas de otros usuarios
                    filteredUsers.forEach { user ->
                        if (!_otherUsersPaths.containsKey(user.uid)) {
                            _otherUsersPaths[user.uid] = mutableListOf()
                        }

                        val latLng = LatLng(user.latitud, user.longitud)
                        if (user.latitud != 0.0 && user.longitud != 0.0) {
                            _otherUsersPaths[user.uid]?.add(latLng)
                        }
                    }

                    // Limpiar rutas de usuarios desconectados
                    val onlineUids = filteredUsers.map { it.uid }
                    _otherUsersPaths.keys.retainAll(onlineUids.toSet())

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

            // Actualizar en Firestore
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
                userRepository.updateConnectionStatus(uid, false)
            }
            authRepository.logout()
            usersListener?.remove()
            onSuccess()
        }
    }

    override fun onCleared() {
        super.onCleared()
        usersListener?.remove()
    }
}