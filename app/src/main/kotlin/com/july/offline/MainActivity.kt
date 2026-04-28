package com.july.offline

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.compose.rememberNavController
import com.july.offline.navigation.JulyNavGraph
import com.july.offline.ui.theme.JulyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Pide RECORD_AUDIO al arrancar para que los componentes de audio
    // (wake word, AudioRecord) encuentren el permiso ya concedido.
    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* el resultado lo gestiona PermissionHandler en cada acción concreta */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            JulyTheme {
                val navController = rememberNavController()
                JulyNavGraph(navController = navController)
            }
        }
    }
}
