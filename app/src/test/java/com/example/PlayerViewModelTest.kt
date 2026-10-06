package com.example

import com.example.domain.model.Content
import com.example.domain.model.Episode
import com.example.domain.model.Season
import com.example.presentation.player.PlaybackSourceType
import com.example.presentation.player.PlayerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlayerViewModelTest {

    @Test
    fun testIsAppIntroVideo_identifiesIntroVariants() {
        // Official intro
        assertTrue(PlayerViewModel.isAppIntroVideo("https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/Intro.mp4"))
        assertTrue(PlayerViewModel.isAppIntroVideo("https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/intro.mp4?token=123"))
        assertTrue(PlayerViewModel.isAppIntroVideo("/videos/Intro.mp4"))
        assertTrue(PlayerViewModel.isAppIntroVideo("videos/intro.mp4"))
        assertTrue(PlayerViewModel.isAppIntroVideo("Intro.mp4"))
        assertTrue(PlayerViewModel.isAppIntroVideo("intro.mp4"))
        assertTrue(PlayerViewModel.isAppIntroVideo(""))
        assertTrue(PlayerViewModel.isAppIntroVideo(null))
        assertTrue(PlayerViewModel.isAppIntroVideo("   "))
    }

    @Test
    fun testIsActualContentUrl_acceptsRealFirestoreContent() {
        val realUrl = "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/1785032493879-e8eu97-MASACRE-ESCOLAR-CAPITULO-1.mp4"
        assertTrue(PlayerViewModel.isActualContentUrl(realUrl))

        val cdnVideo = "https://602td9md5u.ucarecd.net/5036ece7-f331-4fa1-b4a8-77fd5b8b8c4d/AnimeOnlineNinjaDragonBallSuperHeroLatino.mp4"
        assertTrue(PlayerViewModel.isActualContentUrl(cdnVideo))

        // Intro video must NOT be considered actual content
        assertFalse(PlayerViewModel.isActualContentUrl("https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/Intro.mp4"))
        assertFalse(PlayerViewModel.isActualContentUrl("Intro.mp4"))
        assertFalse(PlayerViewModel.isActualContentUrl(""))
        assertFalse(PlayerViewModel.isActualContentUrl(null))
    }

    @Test
    fun testResolvePlayableUrl_seriesWithIntroUrl_filtersIntroAndPlaysRealStream() {
        val viewModel = PlayerViewModel(null)
        val seriesWithIntro = Content(
            id = "test_series_intro",
            title = "Test Series",
            type = "series",
            videoUrl = "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/Intro.mp4",
            seasons = emptyList()
        )

        val result = viewModel.resolvePlayableUrl(seriesWithIntro, null)

        // Must NEVER be the intro URL!
        assertNotEquals("https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/Intro.mp4", result.url)
        assertFalse(result.url.endsWith("Intro.mp4", ignoreCase = true))
        assertTrue(result.isAppIntroFiltered)
        assertTrue(result.url.isNotEmpty())
    }

    @Test
    fun testResolvePlayableUrl_seriesWithEpisodes_playsEpisodeUrl() {
        val viewModel = PlayerViewModel(null)
        val ep1Url = "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/Ep1_RealContent.mp4"
        val episode1 = Episode(
            id = "ep1",
            episodeNumber = 1,
            title = "Capítulo 1",
            videoUrl = ep1Url
        )
        val series = Content(
            id = "test_series",
            title = "Serie Real",
            type = "series",
            videoUrl = "",
            seasons = listOf(
                Season(
                    id = "s1",
                    seasonNumber = 1,
                    title = "Temporada 1",
                    episodes = listOf(episode1)
                )
            )
        )

        val result = viewModel.resolvePlayableUrl(series, episode1)

        assertEquals(ep1Url, result.url)
        assertEquals(PlaybackSourceType.EPISODE_FIRESTORE, result.sourceType)
        assertFalse(result.isAppIntroFiltered)
    }

    @Test
    fun testResolvePlayableUrl_movieWithIntro_filtersIntroAndUsesFallback() {
        val viewModel = PlayerViewModel(null)
        val movieWithIntro = Content(
            id = "movie_123",
            title = "Pelicula Con Intro Erronea",
            type = "movie",
            videoUrl = "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/Intro.mp4"
        )

        val result = viewModel.resolvePlayableUrl(movieWithIntro, null)

        assertNotEquals("https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/Intro.mp4", result.url)
        assertFalse(result.url.endsWith("Intro.mp4", ignoreCase = true))
        assertTrue(result.isAppIntroFiltered)
        assertEquals(PlaybackSourceType.STREAM_BACKUP, result.sourceType)
    }

    @Test
    fun testResolvePlayableUrl_offlineDownloadedFile_resolvesLocalOfflinePlayback() {
        val viewModel = PlayerViewModel(null)
        val offlinePath = "/data/user/0/com.example/files/downloads/ep1_offline.mp4"
        val offlineContent = Content(
            id = "offline_item",
            title = "Video Descargado Offline",
            type = "movie",
            videoUrl = offlinePath
        )

        val result = viewModel.resolvePlayableUrl(offlineContent, null)

        assertEquals(PlaybackSourceType.LOCAL_OFFLINE, result.sourceType)
        assertFalse(result.isAppIntroFiltered)
        assertTrue(result.url.startsWith("file://") || result.url == offlinePath)
        assertFalse(result.url.contains("r2.dev"))
    }
}
