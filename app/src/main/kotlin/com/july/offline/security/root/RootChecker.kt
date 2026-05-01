package com.july.offline.security.root

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootChecker @Inject constructor() {

    data class RootStatus(val isRooted: Boolean, val indicators: List<String>)

    fun check(): RootStatus {
        val indicators = mutableListOf<String>()

        listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/data/local/su", "/data/local/bin/su", "/data/local/xbin/su"
        ).forEach { if (File(it).exists()) indicators.add("su binary: $it") }

        listOf(
            "com.topjohnwu.magisk", "eu.chainfire.supersu",
            "com.noshufou.android.su", "com.koushikdutta.superuser"
        ).forEach { pkg ->
            if (File("/data/data/$pkg").exists()) indicators.add("Root app: $pkg")
        }

        try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val result = p.inputStream.bufferedReader().readLine()
            if (result?.contains("uid=0") == true) indicators.add("su execution: $result")
            p.destroy()
        } catch (e: Exception) { /* not rooted */ }

        return RootStatus(indicators.isNotEmpty(), indicators)
    }
}
