package com.july.offline.domain.action

sealed class ActionCommand {
    data class Call(val number: String) : ActionCommand()
    data class SetAlarm(val hour: Int, val minute: Int, val label: String = "") : ActionCommand()
    data object FlashlightOn : ActionCommand()
    data object FlashlightOff : ActionCommand()
    data class OpenApp(val packageHint: String) : ActionCommand()
    data class Navigate(val destination: String) : ActionCommand()
    data class ComposeSms(val number: String, val body: String = "") : ActionCommand()

    companion object {
        private val TAG_REGEX = Regex("\\[ACCION:([^\\]]+)\\]")

        fun parse(text: String): ActionCommand? {
            val match = TAG_REGEX.find(text) ?: return null
            val parts = match.groupValues[1].split(":")
            return when (parts.getOrNull(0)?.uppercase()) {
                "LLAMAR"   -> parts.getOrNull(1)?.let { Call(it) }
                "ALARMA"   -> parseAlarm(parts)
                "LINTERNA" -> if (parts.getOrNull(1)?.uppercase() == "OFF") FlashlightOff else FlashlightOn
                "ABRIR"    -> parts.getOrNull(1)?.let { OpenApp(it) }
                "NAVEGAR"  -> parts.drop(1).joinToString(":").let { Navigate(it) }
                "SMS"      -> parts.getOrNull(1)?.let { ComposeSms(it, parts.drop(2).joinToString(":")) }
                else       -> null
            }
        }

        fun strip(text: String): String = TAG_REGEX.replace(text, "").trim()

        private fun parseAlarm(parts: List<String>): ActionCommand? {
            val time = parts.getOrNull(1) ?: return null
            val timeParts = time.split(":")
            val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: return null
            val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
            val label = parts.drop(2).joinToString(":")
            return SetAlarm(hour, minute, label)
        }
    }
}
