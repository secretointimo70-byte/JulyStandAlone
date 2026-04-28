package com.july.offline.ui.permission

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Composable que gestiona el permiso de RECORD_AUDIO en runtime.
 *
 * Uso en ConversationScreen:
 *   PermissionHandler(
 *       onPermissionGranted = { viewModel.onMicPressed() },
 *       onPermissionDenied = { viewModel.onPermissionDenied() }
 *   ) { requestPermission ->
 *       Button(onClick = requestPermission) { Text("Hablar") }
 *   }
 */
@Composable
fun PermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable (requestPermission: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            showRationale = true
            onPermissionDenied()
        }
    }

    val requestPermission: () -> Unit = {
        val currentStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        )
        if (currentStatus == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    content(requestPermission)

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Permiso de micrófono") },
            text = {
                Text(
                    "July necesita acceso al micrófono para escucharte. " +
                    "Sin este permiso no puede funcionar. " +
                    "Puedes habilitarlo en Configuración > Aplicaciones > July > Permisos."
                )
            },
            confirmButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}
