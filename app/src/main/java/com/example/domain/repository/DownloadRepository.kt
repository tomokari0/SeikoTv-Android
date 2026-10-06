package com.example.domain.repository

import com.example.domain.model.DownloadItem
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getDownloads(): Flow<List<DownloadItem>>
    suspend fun startDownload(
        contentId: String,
        episodeId: String?,
        title: String,
        subtitle: String,
        thumbnailUrl: String,
        videoUrl: String
    )
    suspend fun pauseDownload(id: String)
    suspend fun resumeDownload(id: String)
    suspend fun deleteDownload(id: String)
}
