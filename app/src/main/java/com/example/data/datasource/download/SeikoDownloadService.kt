package com.example.data.datasource.download

import android.app.Notification
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager as Media3DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.example.R

/**
 * Service de Media3 para ejecutar y persistir descargas multimedia en segundo plano
 * con soporte para reanudación automática y notificaciones de progreso.
 */
@OptIn(UnstableApi::class)
class SeikoDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.download_notification_channel_name,
    R.string.download_notification_description
) {

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "seiko_media3_downloads"
    }

    override fun getDownloadManager(): Media3DownloadManager {
        return DownloadManager.getInstance(this).getMedia3DownloadManager()
    }

    override fun getScheduler(): Scheduler? {
        return null
    }

    override fun getForegroundNotification(
        downloads: List<Download>,
        notMetRequirements: Int
    ): Notification {
        val helper: DownloadNotificationHelper = DownloadManager.getInstance(this).getNotificationHelper()
        return helper.buildProgressNotification(
            this,
            R.drawable.ic_launcher_foreground,
            null,
            null,
            downloads,
            notMetRequirements
        )
    }
}
