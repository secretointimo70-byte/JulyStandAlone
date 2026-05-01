package com.july.offline.domain.action

object TranscriptActionDetector {

    fun detect(transcript: String): Pair<ActionCommand, String>? {
        val t = transcript.lowercase().trim()
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ü", "u")

        // ── Linterna ──────────────────────────────────────────────────────
        if (t.contains(Regex("(enciende|activa|prende|pon).{0,15}(linterna|flash|luz)")))
            return ActionCommand.FlashlightOn to "Encendiendo la linterna."
        if (t.contains(Regex("(apaga|desactiva|quita).{0,15}(linterna|flash|luz)")))
            return ActionCommand.FlashlightOff to "Apagando la linterna."

        // ── Alarma ────────────────────────────────────────────────────────
        val alarmMatch = Regex("(alarma|despiertame|despertador).{0,20}?(\\d{1,2})(?:[:\\s]h?(\\d{2}))?")
            .find(t)
        if (alarmMatch != null) {
            val hour   = alarmMatch.groupValues[2].toIntOrNull() ?: return null
            val minute = alarmMatch.groupValues[3].toIntOrNull() ?: 0
            return ActionCommand.SetAlarm(hour, minute) to
                "Alarma configurada para las $hour:${minute.toString().padStart(2, '0')}."
        }

        // ── Llamada con número ─────────────────────────────────────────────
        val callNumMatch = Regex("(llama|llamar|llame|marca|marcar|llama al|llama a).{0,10}?(\\d[\\d\\s]{3,})")
            .find(t)
        if (callNumMatch != null) {
            val number = callNumMatch.groupValues[2].replace(" ", "").trim()
            return ActionCommand.Call(number) to "Llamando al $number."
        }

        // ── Abrir app / acción de sistema ─────────────────────────────────
        val openMatch = Regex("(abre|abrir|abri|lanza|ejecuta|inicia|muestra|ir a|ve a|entra a|entra en)\\s+(.+)")
            .find(t)
        if (openMatch != null) {
            val raw = openMatch.groupValues[2]
                .replace(Regex("\\b(la|el|los|las|por favor|ahora|rapido|ya)\\b"), "")
                .trim()
            if (raw.isNotBlank()) return resolveAppOrAction(raw)
        }

        // ── Navegar ───────────────────────────────────────────────────────
        val navMatch = Regex("(navega|llevame|dirigete|como llego|llevame a|como voy a)\\s+(.+)")
            .find(t)
        if (navMatch != null) {
            val dest = navMatch.groupValues[2].trim()
            if (dest.isNotBlank()) return ActionCommand.Navigate(dest) to "Navegando hacia $dest."
        }

        return null
    }

    private fun resolveAppOrAction(hint: String): Pair<ActionCommand, String>? {
        val h = hint.trim()

        // Apps del sistema con nombre en español
        val systemMap = mapOf(
            "configuracion" to ActionCommand.OpenApp("com.android.settings"),
            "ajustes"       to ActionCommand.OpenApp("com.android.settings"),
            "settings"      to ActionCommand.OpenApp("com.android.settings"),
            "llamadas"      to ActionCommand.OpenApp("com.android.dialer"),
            "marcador"      to ActionCommand.OpenApp("com.android.dialer"),
            "telefono"      to ActionCommand.OpenApp("com.android.dialer"),
            "camara"        to ActionCommand.OpenApp("com.android.camera"),
            "galeria"       to ActionCommand.OpenApp("com.android.gallery3d"),
            "fotos"         to ActionCommand.OpenApp("com.google.android.apps.photos"),
            "contactos"     to ActionCommand.OpenApp("com.android.contacts"),
            "mensajes"      to ActionCommand.OpenApp("com.android.mms"),
            "sms"           to ActionCommand.OpenApp("com.android.mms"),
            "maps"          to ActionCommand.OpenApp("com.google.android.apps.maps"),
            "mapa"          to ActionCommand.OpenApp("com.google.android.apps.maps"),
            "youtube"       to ActionCommand.OpenApp("com.google.android.youtube"),
            "chrome"        to ActionCommand.OpenApp("com.android.chrome"),
            "gmail"         to ActionCommand.OpenApp("com.google.android.gm"),
            "correo"        to ActionCommand.OpenApp("com.google.android.gm"),
            "calendario"    to ActionCommand.OpenApp("com.android.calendar"),
            "calculadora"   to ActionCommand.OpenApp("com.android.calculator2"),
            "whatsapp"      to ActionCommand.OpenApp("com.whatsapp"),
            "facebook"      to ActionCommand.OpenApp("com.facebook.katana"),
            "instagram"     to ActionCommand.OpenApp("com.instagram.android"),
            "tiktok"        to ActionCommand.OpenApp("com.zhiliaoapp.musically"),
            "spotify"       to ActionCommand.OpenApp("com.spotify.music"),
            "telegram"      to ActionCommand.OpenApp("org.telegram.messenger"),
        )

        // Buscar coincidencia exacta o parcial en el diccionario
        val found = systemMap.entries.firstOrNull { h.contains(it.key) }
        if (found != null) return found.value to "Abriendo ${found.key}."

        // Si no está en el diccionario, buscar por nombre genérico
        return ActionCommand.OpenApp(h) to "Abriendo $h."
    }
}
