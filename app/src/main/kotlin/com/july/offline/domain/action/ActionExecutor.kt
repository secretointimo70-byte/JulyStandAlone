package com.july.offline.domain.action

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Telephony
import com.july.offline.core.logging.DiagnosticsLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: DiagnosticsLogger
) {
    private val TAG = "ActionExecutor"
    private var torchOn = false

    fun execute(command: ActionCommand) {
        logger.logInfo(TAG, "Ejecutando: $command")
        try {
            when (command) {
                is ActionCommand.Call        -> dial(command.number)
                is ActionCommand.SetAlarm    -> setAlarm(command.hour, command.minute, command.label)
                is ActionCommand.FlashlightOn  -> setTorch(true)
                is ActionCommand.FlashlightOff -> setTorch(false)
                is ActionCommand.OpenApp     -> openApp(command.packageHint)
                is ActionCommand.Navigate    -> navigate(command.destination)
                is ActionCommand.ComposeSms  -> composeSms(command.number, command.body)
            }
        } catch (e: Exception) {
            logger.logError(TAG, "Fallo al ejecutar $command", e)
        }
    }

    private fun dial(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun setAlarm(hour: Int, minute: Int, label: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            if (label.isNotBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun setTorch(on: Boolean) {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return
        manager.setTorchMode(cameraId, on)
        torchOn = on
    }

    private fun openApp(hint: String) {
        val pm = context.packageManager
        val candidates = listOf(hint) + (packageFallbacks[hint] ?: emptyList())
        val intent = candidates
            .firstNotNullOfOrNull { pkg -> pm.getLaunchIntentForPackage(pkg) }
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent != null) context.startActivity(intent)
        else logger.logWarning(TAG, "App no encontrada o no instalada: $hint")
    }

    companion object {
        // Fallbacks para apps con múltiples variantes o paquetes por fabricante
        private val packageFallbacks = mapOf(
            "com.facebook.katana"          to listOf("com.facebook.lite", "com.facebook.mlite"),
            "com.android.dialer"           to listOf("com.google.android.dialer", "com.samsung.android.dialer", "com.huawei.contacts"),
            "com.android.camera"           to listOf("com.google.android.GoogleCamera", "com.huawei.camera"),
            "com.android.gallery3d"        to listOf("com.google.android.apps.photos", "com.huawei.photos"),
            "com.android.mms"              to listOf("com.google.android.apps.messaging", "com.samsung.android.messaging"),
            "com.android.contacts"         to listOf("com.google.android.contacts", "com.huawei.contacts"),
            "com.android.calculator2"      to listOf("com.google.android.calculator", "com.huawei.calculator"),
            "com.android.calendar"         to listOf("com.google.android.calendar", "com.huawei.calendar"),
        )
    }

    private fun navigate(destination: String) {
        val encoded = Uri.encode(destination)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun composeSms(number: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
            if (body.isNotBlank()) putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
