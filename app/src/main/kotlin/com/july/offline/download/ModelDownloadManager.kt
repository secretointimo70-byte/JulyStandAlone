package com.july.offline.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class DownloadStatus {
    object NotInstalled : DownloadStatus()
    object Installed : DownloadStatus()
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : DownloadStatus() {
        val progress: Float get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
        val progressPercent: Int get() = (progress * 100).toInt()
        val downloadedMb: String get() = "${"%.0f".format(downloadedBytes / 1_000_000.0)} MB"
        val totalMb: String get() = "${"%.0f".format(totalBytes / 1_000_000.0)} MB"
    }
    object Pending : DownloadStatus()
    object Failed : DownloadStatus()
}

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun startDownload(model: DownloadableModel): Long {
        val req = DownloadManager.Request(Uri.parse(model.url))
            .setTitle(model.displayName)
            .setDescription("Descargando ${model.filename}…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(context, null, model.filename)
        return dm.enqueue(req)
    }

    fun cancel(downloadId: Long) {
        dm.remove(downloadId)
    }

    fun queryStatus(downloadId: Long): DownloadStatus {
        val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
        if (!cursor.moveToFirst()) { cursor.close(); return DownloadStatus.NotInstalled }
        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
        cursor.close()
        return when (status) {
            DownloadManager.STATUS_RUNNING,
            DownloadManager.STATUS_PAUSED -> DownloadStatus.Downloading(downloaded, total)
            DownloadManager.STATUS_PENDING -> DownloadStatus.Pending
            DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.Installed
            DownloadManager.STATUS_FAILED -> DownloadStatus.Failed
            else -> DownloadStatus.NotInstalled
        }
    }

    fun isInstalled(filename: String): Boolean {
        val candidates = listOf(
            File(context.getExternalFilesDir(null), filename),
            File("/sdcard/Android/data/${context.packageName}/files/$filename"),
            File("/storage/emulated/0/Android/data/${context.packageName}/files/$filename"),
            File(context.filesDir, filename)
        )
        return candidates.any { it.exists() && it.length() > 1_000_000L }
    }

    fun fileSizeOnDisk(filename: String): Long {
        val candidates = listOf(
            File(context.getExternalFilesDir(null), filename),
            File("/sdcard/Android/data/${context.packageName}/files/$filename"),
            File(context.filesDir, filename)
        )
        return candidates.firstOrNull { it.exists() }?.length() ?: 0L
    }
}
