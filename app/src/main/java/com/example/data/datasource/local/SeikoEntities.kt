package com.example.data.datasource.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.DownloadItem
import com.example.domain.model.DownloadStatus

@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val id: String, // contentId or "contentId_episodeId"
    val contentId: String,
    val episodeId: String? = null,
    val profileId: String = "profile_1",
    val progressMs: Long = 0L,
    val durationMs: Long = 0L,
    val percentage: Float = 0f,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
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
    val status: String = DownloadStatus.IDLE.name,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): DownloadItem = DownloadItem(
        id = id,
        contentId = contentId,
        episodeId = episodeId,
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        videoUrl = videoUrl,
        localFilePath = localFilePath,
        progress = progress,
        bytesDownloaded = bytesDownloaded,
        totalBytes = totalBytes,
        status = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.IDLE),
        createdAt = createdAt
    )
}

@Entity(tableName = "my_list")
data class MyListEntity(
    @PrimaryKey val contentId: String,
    val profileId: String = "profile_1",
    val title: String,
    val thumbnailUrl: String,
    val type: String,
    val addedAt: Long = System.currentTimeMillis()
)
