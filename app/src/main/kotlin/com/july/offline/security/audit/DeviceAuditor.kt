package com.july.offline.security.audit

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.security.report.SecurityFinding
import com.july.offline.security.report.SecurityFinding.Severity.*
import com.july.offline.security.report.SecurityFinding.FindingCategory.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeviceAuditor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: DiagnosticsLogger
) {

    suspend fun audit(): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()
        logger.logInfo("DeviceAuditor", "Starting device audit")
        findings += checkAndroidVersion()
        findings += checkEncryption()
        findings += checkLockScreen()
        findings += checkDeveloperOptions()
        findings += checkInstalledApps()
        findings += checkSecurityPatch()
        logger.logInfo("DeviceAuditor", "Device audit complete: ${findings.size} findings")
        return findings
    }

    private fun checkAndroidVersion(): List<SecurityFinding> {
        val sdk = Build.VERSION.SDK_INT
        val rel = Build.VERSION.RELEASE
        val severity = if (sdk < 29) HIGH else INFO
        return listOf(SecurityFinding(
            id = "DEV_OS_001", severity = severity, category = CONFIGURATION,
            title = "Versión de Android",
            description = "Android $rel (API $sdk)." +
                if (sdk < 29) " Versiones anteriores a Android 10 tienen vulnerabilidades conocidas." else " Versión aceptable.",
            recommendation = "Mantener actualizado a la última versión disponible.",
            rawData = "SDK: $sdk, Release: $rel"
        ))
    }

    private fun checkEncryption(): List<SecurityFinding> {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val status = dpm.storageEncryptionStatus
        val (sev, desc) = when (status) {
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE,
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER,
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY ->
                INFO to "Cifrado del almacenamiento activo."
            DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE ->
                CRITICAL to "El almacenamiento NO está cifrado. Acceso físico expone todos los datos."
            DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED ->
                HIGH to "El dispositivo no soporta cifrado."
            else -> INFO to "Estado de cifrado: $status"
        }
        return listOf(SecurityFinding(
            id = "DEV_ENC_001", severity = sev, category = ENCRYPTION,
            title = "Estado de cifrado del almacenamiento",
            description = desc,
            recommendation = if (sev == CRITICAL || sev == HIGH)
                "Habilitar cifrado en Ajustes > Seguridad > Cifrado."
            else "El cifrado está activo.",
            rawData = "encryption_status: $status"
        ))
    }

    private fun checkLockScreen(): List<SecurityFinding> {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val secure = km.isDeviceSecure
        return listOf(SecurityFinding(
            id = "DEV_LOCK_001",
            severity = if (secure) INFO else CRITICAL,
            category = AUTHENTICATION,
            title = if (secure) "Pantalla de bloqueo configurada" else "Sin pantalla de bloqueo",
            description = if (secure)
                "El dispositivo tiene protección de pantalla de bloqueo activa."
            else
                "Sin PIN, patrón ni biométrico. Acceso físico expone todos los datos.",
            recommendation = if (secure)
                "Verificar que el timeout de bloqueo es <= 5 minutos."
            else
                "Configurar un PIN de al menos 6 dígitos inmediatamente.",
            rawData = "keyguard_secure: $secure"
        ))
    }

    private fun checkDeveloperOptions(): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()
        val devEnabled = Settings.Global.getInt(
            context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
        ) != 0
        val adbEnabled = Settings.Global.getInt(
            context.contentResolver, Settings.Global.ADB_ENABLED, 0
        ) != 0

        if (devEnabled) findings.add(SecurityFinding(
            id = "DEV_OPT_001", severity = MEDIUM, category = CONFIGURATION,
            title = "Opciones de desarrollador habilitadas",
            description = "Las opciones de desarrollador están activas.",
            recommendation = "Deshabilitar en dispositivos de producción.",
            rawData = "DEVELOPMENT_SETTINGS_ENABLED: 1"
        ))

        if (adbEnabled) findings.add(SecurityFinding(
            id = "DEV_OPT_002", severity = HIGH, category = CONFIGURATION,
            title = "USB Debugging habilitado",
            description = "ADB por USB está activo. Permite ejecutar comandos en el dispositivo.",
            recommendation = "Deshabilitar USB Debugging en producción.",
            rawData = "ADB_ENABLED: 1"
        ))

        return findings
    }

    private fun checkInstalledApps(): List<SecurityFinding> {
        return try {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val sideloaded = apps.filter { app ->
                try {
                    val installer = pm.getInstallerPackageName(app.packageName)
                    installer == null &&
                    app.packageName != context.packageName &&
                    (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
                } catch (e: Exception) { false }
            }
            if (sideloaded.isNotEmpty()) listOf(SecurityFinding(
                id = "DEV_APP_001", severity = MEDIUM, category = CONFIGURATION,
                title = "${sideloaded.size} apps instaladas fuera de Play Store",
                description = "Apps sin instalador conocido: " +
                    sideloaded.take(5).joinToString(", ") { it.packageName } +
                    if (sideloaded.size > 5) " y ${sideloaded.size - 5} más..." else "",
                recommendation = "Verificar que cada app es de fuente confiable.",
                rawData = sideloaded.joinToString("\n") { it.packageName }
            )) else emptyList()
        } catch (e: Exception) {
            logger.logWarning("DeviceAuditor", "App list check limited: ${e.message}")
            listOf(SecurityFinding(
                id = "DEV_APP_001", severity = INFO, category = CONFIGURATION,
                title = "Verificación de apps limitada",
                description = "No se pudo obtener la lista completa de apps instaladas.",
                recommendation = "Para auditoría completa de apps, revisar manualmente Ajustes > Apps.",
                rawData = "error: ${e.message}"
            ))
        }
    }

    private fun checkSecurityPatch(): List<SecurityFinding> {
        return listOf(SecurityFinding(
            id = "DEV_PATCH_001", severity = INFO, category = CONFIGURATION,
            title = "Nivel de parche de seguridad Android",
            description = "Parche actual: ${Build.VERSION.SECURITY_PATCH}.",
            recommendation = "Mantener el parche con menos de 90 días de antigüedad.",
            rawData = "SECURITY_PATCH: ${Build.VERSION.SECURITY_PATCH}"
        ))
    }
}
