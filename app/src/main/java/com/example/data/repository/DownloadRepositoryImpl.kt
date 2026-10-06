package com.example.data.repository

import android.content.Context
import com.example.data.datasource.download.DownloadManager
import com.example.data.datasource.local.DownloadDao
import com.example.domain.model.DownloadItem
import com.example.domain.repository.DownloadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DownloadRepositoryImpl(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val applicationScope: CoroutineScope
) : DownloadRepository {

    private val downloadManager: DownloadManager by lazy {
        DownloadManager.getInstance(context)
    }

    override fun getDownloads(): Flow<List<DownloadItem>> =
        downloadDao.getAllDownloads().map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun startDownload(
        contentId: String,
        episodeId: String?,
        title: String,
        subtitle: String,
        thumbnailUrl: String,
        videoUrl: String
    ) {
        downloadManager.startDownload(
            contentId = contentId,
            episodeId = episodeId,
            title = title,
            subtitle = subtitle,
            thumbnailUrl = thumbnailUrl,
            videoUrl = videoUrl
        )
    }

    override suspend fun pauseDownload(id: String) {
        downloadManager.pauseDownload(id)
    }

    override suspend fun resumeDownload(id: String) {
        downloadManager.resumeDownload(id)
    }

    override suspend fun deleteDownload(id: String) {
        downloadManager.deleteDownload(id)
    }
}
