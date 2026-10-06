package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.datasource.download.DownloadManager
import com.example.data.datasource.local.SeikoDatabase
import com.example.data.repository.DownloadRepositoryImpl
import com.example.domain.model.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DownloadManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var context: Context
    private lateinit var database: SeikoDatabase
    private lateinit var downloadManager: DownloadManager
    private lateinit var downloadRepository: DownloadRepositoryImpl

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        database = SeikoDatabase.getInstance(context)
        downloadManager = DownloadManager.getInstance(context)
        downloadRepository = DownloadRepositoryImpl(
            context = context,
            downloadDao = database.downloadDao(),
            applicationScope = testScope
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDownloadManager_componentsInitialized() {
        assertNotNull("Media3 DownloadManager should be initialized", downloadManager.getMedia3DownloadManager())
        assertNotNull("NotificationHelper should be initialized", downloadManager.getNotificationHelper())
        assertNotNull("DownloadCache should be initialized", downloadManager.getDownloadCache())
        assertNotNull("DataSourceFactory should be created", downloadManager.createDataSourceFactory())
    }

    @Test
    fun testStartDownload_persistsItemInDatabase() = testScope.runTest {
        val contentId = "test_offline_video_1"
        val videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

        downloadRepository.startDownload(
            contentId = contentId,
            episodeId = null,
            title = "Video Offline Test",
            subtitle = "Prueba de descarga",
            thumbnailUrl = "https://example.com/thumb.jpg",
            videoUrl = videoUrl
        )

        advanceUntilIdle()

        val downloads = downloadRepository.getDownloads().first()
        assertTrue("Downloads list should not be empty", downloads.isNotEmpty())

        val item = downloads.firstOrNull { it.contentId == contentId }
        assertNotNull("Item should exist in database", item)
        assertEquals("Video Offline Test", item?.title)
        assertTrue(
            "Status should be DOWNLOADING or COMPLETED",
            item?.status == DownloadStatus.DOWNLOADING || item?.status == DownloadStatus.COMPLETED
        )
    }

    @Test
    fun testPauseAndResumeDownload() = testScope.runTest {
        val contentId = "test_pause_resume"
        val entity = com.example.data.datasource.local.DownloadEntity(
            id = contentId,
            contentId = contentId,
            episodeId = null,
            title = "Pausa Test",
            subtitle = "Sub",
            thumbnailUrl = "",
            videoUrl = "https://example.com/video.mp4",
            status = DownloadStatus.DOWNLOADING.name,
            progress = 0.5f,
            bytesDownloaded = 50_000_000L,
            totalBytes = 100_000_000L
        )
        database.downloadDao().insertOrUpdate(entity)

        downloadRepository.pauseDownload(contentId)
        advanceUntilIdle()

        val itemPaused = downloadRepository.getDownloads().first().firstOrNull { it.id == contentId }
        assertEquals(DownloadStatus.PAUSED, itemPaused?.status)

        downloadRepository.resumeDownload(contentId)
        advanceUntilIdle()

        val itemResumed = downloadRepository.getDownloads().first().firstOrNull { it.id == contentId }
        assertEquals(DownloadStatus.DOWNLOADING, itemResumed?.status)
    }

    @Test
    fun testDeleteDownload_removesItemFromDatabase() = testScope.runTest {
        val contentId = "test_delete"
        downloadRepository.startDownload(
            contentId = contentId,
            episodeId = null,
            title = "Delete Test",
            subtitle = "Sub",
            thumbnailUrl = "",
            videoUrl = "https://example.com/video.mp4"
        )
        advanceUntilIdle()

        downloadRepository.deleteDownload(contentId)
        advanceUntilIdle()

        val item = downloadRepository.getDownloads().first().firstOrNull { it.id == contentId }
        assertTrue("Item should be removed from database", item == null)
    }
}
