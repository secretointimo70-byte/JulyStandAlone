package com.july.offline.security.audit

import android.content.Context
import android.content.pm.PackageManager
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.security.report.SecurityFinding
import com.july.offline.security.report.SecurityFinding.Severity.*
import com.july.offline.security.report.SecurityFinding.FindingCategory.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class AppAuditor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: DiagnosticsLogger
) {

    suspend fun audit(): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()
        logger.logInfo("AppAuditor", "Starting app audit")
        findings += checkPermissions()
        findings += checkStorage()
        findings += checkBuildConfig()
        findings += checkJniLibraries()
        findings += checkNetworkSecurity()
        logger.logInfo("AppAuditor", "App audit complete: ${findings.size} findings")
        return findings
    }

    private fun checkPermissions(): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()
        try {
            val info = context.packageManager.getPackageInfo(
                context.packageName, PackageManager.GET_PERMISSIONS
            )
            val dangerous = (info.requestedPermissions ?: emptyArray())
                .filter { it in DANGEROUS_PERMISSIONS }

            if (dangerous.isNotEmpty()) {
                findings.add(SecurityFinding(
                    id = "APP_PERM_001", severity = INFO, category = PERMISSION,
                    title = "Permisos peligrosos declarados",
                    description = "La app declara ${dangerous.size} permisos peligrosos: " +
                        dangerous.joinToString(", ") { it.substringAfterLast(".") },
                    recommendation = "Verificar que cada permiso es estrictamente necesario."
                ))
            }

            val micGranted = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (micGranted) {
                findings.add(SecurityFinding(
                    id = "APP_PERM_002", severity = INFO, category = PERMISSION,
                    title = "RECORD_AUDIO concedido",
                    description = "El micrófono está habilitado.",
                    recommendation = "Verificar que solo se activa cuando el usuario lo inicia.",
                    rawData = "RECORD_AUDIO: GRANTED"
                ))
            }
        } catch (e: Exception) {
            logger.logError("AppAuditor", "Permission check failed", e)
        }
        return findings
    }

    private fun checkStorage(): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()

        val largeFiles = (context.filesDir.listFiles() ?: emptyArray())
            .filter { it.length() > 50 * 1024 * 1024 }
        if (largeFiles.isNotEmpty()) {
            findings.add(SecurityFinding(
                id = "APP_STOR_001", severity = INFO, category = STORAGE,
                title = "Archivos de modelo grandes en filesDir",
                description = "${largeFiles.size} archivos grandes: " +
                    largeFiles.joinToString(", ") { "${it.name} (${it.length() / 1024 / 1024}MB)" },
                recommendation = "Verificar que los modelos JNI no son accesibles desde otras apps.",
                rawData = largeFiles.joinToString("\n") { "${it.name}: ${it.length()} bytes" }
            ))
        }

        val cacheSize = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        if (cacheSize > 100 * 1024 * 1024) {
            findings.add(SecurityFinding(
                id = "APP_STOR_002", severity = LOW, category = STORAGE,
                title = "Cache grande detectado",
                description = "El cache ocupa ${cacheSize / 1024 / 1024}MB.",
                recommendation = "Verificar que no contiene datos sensibles persistentes.",
                rawData = "cacheDir size: $cacheSize bytes"
            ))
        }

        val db = context.getDatabasePath("july_offline.db")
        if (db.exists()) {
            findings.add(SecurityFinding(
                id = "APP_STOR_003", severity = INFO, category = STORAGE,
                title = "Base de datos Room detectada",
                description = "Archivo: ${db.absolutePath}. Tamaño: ${db.length() / 1024}KB.",
                recommendation = "En producción considerar cifrado SQLCipher.",
                rawData = "size: ${db.length()}"
            ))
        }

        return findings
    }

    private fun checkBuildConfig(): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()
        try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            val isDebug = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (isDebug) {
                findings.add(SecurityFinding(
                    id = "APP_BUILD_001", severity = HIGH, category = CONFIGURATION,
                    title = "Build debug detectado",
                    description = "La app está compilada en modo DEBUG. Permite attach de debugger y expone logs.",
                    recommendation = "En producción usar builds release con minificación activada.",
                    rawData = "FLAG_DEBUGGABLE: true"
                ))
            }
        } catch (e: Exception) {
            logger.logError("AppAuditor", "Build config check failed", e)
        }
        return findings
    }

    private fun checkJniLibraries(): List<SecurityFinding> {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        return listOf("libwhisper.so", "libpiper.so", "libllama.so", "libggml.so", "libonnxruntime.so")
            .mapNotNull { libName ->
                val f = File(nativeLibDir, libName)
                if (f.exists()) SecurityFinding(
                    id = "APP_JNI_${libName.removeSuffix(".so").removePrefix("lib").uppercase()}",
                    severity = INFO, category = CONFIGURATION,
                    title = "Librería nativa: $libName",
                    description = "Tamaño: ${f.length() / 1024}KB.",
                    recommendation = "Verificar SHA-256 contra hashes oficiales del release.",
                    rawData = "size: ${f.length()}"
                ) else null
            }
    }

    private fun checkNetworkSecurity(): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()
        val hasNsc = context.resources.getIdentifier(
            "network_security_config", "xml", context.packageName
        ) != 0

        if (!hasNsc) {
            findings.add(SecurityFinding(
                id = "APP_NET_001", severity = MEDIUM, category = CONFIGURATION,
                title = "Sin Network Security Config",
                description = "La app no declara network_security_config.xml explícito.",
                recommendation = "Añadir res/xml/network_security_config.xml con cleartextTrafficPermitted=\"false\".",
                rawData = "network_security_config: NOT FOUND"
            ))
        }

        findings.add(SecurityFinding(
            id = "APP_NET_002", severity = INFO, category = NETWORK,
            title = "Servidor LLM en localhost",
            description = "El servidor LLM local está configurado en 127.0.0.1.",
            recommendation = "Verificar que el puerto del servidor LLM no está expuesto en el firewall.",
            rawData = "llm_host: 127.0.0.1"
        ))

        return findings
    }

    companion object {
        private val DANGEROUS_PERMISSIONS = setOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}
