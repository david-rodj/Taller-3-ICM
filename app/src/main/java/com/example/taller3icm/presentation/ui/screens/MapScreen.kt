package com.example.taller3icm.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taller3icm.presentation.viewmodel.MapViewModel
import com.example.taller3icm.utils.LocationService
import com.example.taller3icm.utils.MarkerUtils
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    onNavigateToProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Servicio de ubicación
    val locationService = remember { LocationService(context) }

    // Permisos de ubicación
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Estado del menú
    var showMenu by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Posición inicial del mapa (Bogotá)
    val defaultPosition = LatLng(4.7110, -74.0721)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 12f)
    }

    // Observar cambios de ubicación
    LaunchedEffect(uiState.isLocationEnabled) {
        if (uiState.isLocationEnabled) {
            if (locationPermissions.allPermissionsGranted) {
                locationService.requestLocationUpdates().collect { location ->
                    viewModel.onLocationUpdate(location)
                    // Mover cámara a la ubicación actual
                    val newPosition = LatLng(location.latitude, location.longitude)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(newPosition, 15f)
                }
            } else {
                // Si no hay permisos, desactivar el switch
                viewModel.onLocationToggle(false)
                showPermissionDialog = true
            }
        } else {
            locationService.stopLocationUpdates()
        }
    }

    // Diálogo de permisos
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permisos de Ubicación Requeridos") },
            text = {
                Text(
                    "Esta aplicación necesita acceso a tu ubicación para " +
                            "mostrar tu posición en tiempo real y ver otros usuarios."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        locationPermissions.launchMultiplePermissionRequest()
                        showPermissionDialog = false
                    }
                ) {
                    Text("Conceder Permisos")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa de Usuarios") },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar Perfil") },
                            onClick = {
                                showMenu = false
                                onNavigateToProfile()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Cerrar Sesión") },
                            onClick = {
                                showMenu = false
                                viewModel.onLogout(onSuccess = onLogout)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ExitToApp, contentDescription = null)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mapa
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false)
            ) {
                // Marcador del usuario actual
                uiState.currentUser?.let { user ->
                    if (user.latitud != 0.0 && user.longitud != 0.0) {
                        val position = LatLng(user.latitud, user.longitud)

                        Marker(
                            state = MarkerState(position = position),
                            title = "Yo",
                            snippet = user.nombre,
                            icon = MarkerUtils.createCustomMarker(
                                context = context,
                                color = android.graphics.Color.BLUE
                            )
                        )

                        // Polyline del usuario actual
                        if (uiState.userPath.isNotEmpty()) {
                            Polyline(
                                points = uiState.userPath,
                                color = androidx.compose.ui.graphics.Color.Blue,
                                width = 8f
                            )
                        }
                    }
                }

                // Marcadores de otros usuarios en línea
                uiState.onlineUsers.forEach { user ->
                    if (user.latitud != 0.0 && user.longitud != 0.0) {
                        val position = LatLng(user.latitud, user.longitud)

                        Marker(
                            state = MarkerState(position = position),
                            title = user.nombre,
                            snippet = "En línea",
                            icon = MarkerUtils.createTextMarker(
                                context = context,
                                text = user.nombre,
                                backgroundColor = android.graphics.Color.RED
                            )
                        )

                        // Polyline de otros usuarios
                        uiState.otherUsersPaths[user.uid]?.let { path ->
                            if (path.isNotEmpty()) {
                                Polyline(
                                    points = path,
                                    color = androidx.compose.ui.graphics.Color.Red,
                                    width = 5f
                                )
                            }
                        }
                    }
                }
            }

            // Switch de ubicación (flotante)
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.isLocationEnabled) "Conectado" else "Desconectado",
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = uiState.isLocationEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !locationPermissions.allPermissionsGranted) {
                                showPermissionDialog = true
                            } else {
                                viewModel.onLocationToggle(enabled)
                            }
                        }
                    )
                }
            }

            // Contador de usuarios en línea
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Text(
                    text = "Usuarios en línea: ${uiState.onlineUsers.size}",
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}