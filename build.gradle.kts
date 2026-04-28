// build.gradle.kts (root)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.junit5.android) apply false
}

// ── Tarea QA: copiar modelos al dispositivo conectado ─────────────────────
//
// Uso:
//   ./gradlew pushModels
//   (o doble clic en install-models.bat para Windows sin terminal)
//
// Coloca los modelos en la carpeta models/ del proyecto antes de ejecutar.

val modelsDir = file("models")
val deviceFilesPath = "/data/data/com.july.offline/files"

val modelFiles = mapOf(
    "whisper-small.bin"                        to "Whisper Small (STT)",
    "Llama-3.2-3B-Instruct-Q4_K_M.gguf"       to "Llama 3.2 3B (LLM)"
)

tasks.register("pushModels") {
    group = "july"
    description = "Copia los modelos de IA al dispositivo Android conectado via ADB"

    doLast {
        val androidHome = System.getenv("ANDROID_HOME")
            ?: "${System.getProperty("user.home")}/AppData/Local/Android/Sdk"
        val adb = "$androidHome/platform-tools/adb"

        fun adb(vararg args: String) = exec { commandLine(adb, *args) }
        fun adbOut(vararg args: String): String {
            val out = java.io.ByteArrayOutputStream()
            exec { commandLine(adb, *args); standardOutput = out }
            return out.toString().trim()
        }

        // Preparar directorio destino via run-as (directorio privado de la app)
        adb("shell", "run-as com.july.offline mkdir -p $deviceFilesPath")

        var allOk = true
        modelFiles.forEach { (fileName, description) ->
            val modelFile = modelsDir.resolve(fileName)
            if (!modelFile.exists()) {
                println("⚠  FALTA: models/$fileName  ($description)")
                println("   Ver models/README.md para saber donde descargarlo.")
                allOk = false
                return@forEach
            }

            println("→  Copiando $fileName a /sdcard/ ...")
            adb("push", modelFile.absolutePath, "/sdcard/$fileName")

            println("   Moviendo al directorio privado de la app...")
            adb("shell", "run-as com.july.offline cp /sdcard/$fileName $deviceFilesPath/$fileName")
            adb("shell", "rm /sdcard/$fileName")

            println("✓  $fileName instalado")
        }

        println(if (allOk) "\n✓ Todos los modelos instalados. Abre la app July."
                else        "\n⚠ Algunos modelos no estaban disponibles.")
    }
}
