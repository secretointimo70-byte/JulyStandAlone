package com.july.offline.core.startup

import android.content.Context
import com.july.offline.core.logging.DiagnosticsLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copia archivos desde assets/ a filesDir en el primer arranque.
 *
 * Uso principal en FASE 8:
 * - Modelo Porcupine (.ppn) para wake-word personalizado "Oye July"
 *   El .ppn debe estar en app/src/main/assets/oye_july_es.ppn
 *
 * Política de copia:
 * - Solo copia si el archivo de destino no existe (primer arranque)
 * - Si el archivo ya existe, verifica el tamaño. Si el source es más grande
 *   (actualización de modelo), reemplaza.
 * - Operación asíncrona en Dispatchers.IO
 *
 * Llamado desde JulyApplication.onCreate() antes de cualquier motor.
 */
@Singleton
class AssetCopier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: DiagnosticsLogger
) {

    data class CopyResult(
        val fileName: String,
        val destinationPath: String,
        val copied: Boolean,
        val skipped: Boolean,
        val error: Exception? = null
    )

    /**
     * Copia un archivo de assets a filesDir si es necesario.
     *
     * @param assetFileName nombre del archivo en assets/ (ej: "oye_july_es.ppn")
     * @param destinationFileName nombre en filesDir (puede ser diferente al asset)
     * @return CopyResult con el resultado de la operación
     */
    suspend fun copyIfNeeded(
        assetFileName: String,
        destinationFileName: String = assetFileName
    ): CopyResult = withContext(Dispatchers.IO) {

        val destination = File(context.filesDir, destinationFileName)

        try {
            // Verificar si el asset existe
            val assetFiles = context.assets.list("") ?: emptyArray()
            if (assetFileName !in assetFiles) {
                logger.logWarning(
                    "AssetCopier",
                    "Asset not found: $assetFileName — skipping copy"
                )
                return@withContext CopyResult(
                    fileName = assetFileName,
                    destinationPath = destination.absolutePath,
                    copied = false,
                    skipped = true
                )
            }

            // Verificar si ya existe y tiene el mismo tamaño
            if (destination.exists()) {
                val assetSize = context.assets.open(assetFileName).use { it.available().toLong() }
                if (destination.length() == assetSize) {
                    logger.logDebug(
                        "AssetCopier",
                        "Already exists, same size: $destinationFileName — skipping"
                    )
                    return@withContext CopyResult(
                        fileName = assetFileName,
                        destinationPath = destination.absolutePath,
                        copied = false,
                        skipped = true
                    )
                }
                logger.logInfo(
                    "AssetCopier",
                    "Size mismatch for $destinationFileName — replacing (model update)"
                )
            }

            // Copiar
            logger.logInfo("AssetCopier", "Copying $assetFileName → ${destination.absolutePath}")
            context.assets.open(assetFileName).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }

            logger.logInfo(
                "AssetCopier",
                "Copied $assetFileName (${destination.length()} bytes)"
            )

            CopyResult(
                fileName = assetFileName,
                destinationPath = destination.absolutePath,
                copied = true,
                skipped = false
            )

        } catch (e: Exception) {
            logger.logError("AssetCopier", "Failed to copy $assetFileName", e)
            CopyResult(
                fileName = assetFileName,
                destinationPath = destination.absolutePath,
                copied = false,
                skipped = false,
                error = e
            )
        }
    }

    /**
     * Copia múltiples assets en paralelo.
     * @param assets lista de pares (assetFileName, destinationFileName)
     */
    suspend fun copyAll(
        assets: List<Pair<String, String>>
    ): List<CopyResult> = withContext(Dispatchers.IO) {
        assets.map { (asset, dest) -> copyIfNeeded(asset, dest) }
    }
}
