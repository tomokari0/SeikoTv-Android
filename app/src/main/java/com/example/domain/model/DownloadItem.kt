package com.example.domain.model

enum class DownloadStatus {
    IDLE,
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    PAUSED
}

data class DownloadItem(
    val id: String,
    val contentId: String,
    val episodeId: String? = null,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val localFilePath: String? = null,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val createdAt: Long = System.currentTimeMillis()
)
