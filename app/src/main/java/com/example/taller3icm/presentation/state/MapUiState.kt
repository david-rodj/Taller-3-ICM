package com.example.taller3icm.presentation.state

import com.example.taller3icm.data.model.User

data class MapUiState(
    val currentUser: User? = null,
    val onlineUsers: List<User> = emptyList(),
    val isLocationEnabled: Boolean = false,
    val userPath: List<com.google.android.gms.maps.model.LatLng> = emptyList(),
    val otherUsersPaths: Map<String, List<com.google.android.gms.maps.model.LatLng>> = emptyMap(),
    val errorMessage: String? = null
)
