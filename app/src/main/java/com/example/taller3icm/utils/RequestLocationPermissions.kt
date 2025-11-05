package com.example.taller3icm.utils

import androidx.compose.runtime.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import android.Manifest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestLocationPermissions(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }

    if (!locationPermissionsState.allPermissionsGranted) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Permisos de Ubicación Requeridos") },
            text = {
                Text(
                    "Esta aplicación necesita acceso a tu ubicación para " +
                            "mostrar tu posición en tiempo real y ver otros usuarios."
                )
            },
            confirmButton = {
                Button(onClick = { locationPermissionsState.launchMultiplePermissionRequest() }) {
                    Text("Conceder Permisos")
                }
            },
            dismissButton = {
                TextButton(onClick = onPermissionDenied) {
                    Text("Cancelar")
                }
            }
        )
    }
}