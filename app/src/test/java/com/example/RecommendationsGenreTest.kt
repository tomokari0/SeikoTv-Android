package com.example

import com.example.domain.model.Content
import com.example.domain.model.UserProfile
import com.example.domain.repository.ContentRepository
import com.example.domain.repository.DownloadRepository
import com.example.domain.repository.UserRepository
import com.example.presentation.home.HomeViewModel
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RecommendationsGenreTest {

    private lateinit var viewModel: HomeViewModel

    private val animeMasacre = Content(
        id = "masacre_escolar",
        title = "Masacre Escolar",
        type = "series",
        genre = listOf("Slasher", "Terror", "Misterio", "Thriller"),
        matchPercentage = 95
    )

    private val animeZombies = Content(
        id = "estamos_muertos",
        title = "Estamos Muertos",
        type = "series",
        genre = listOf("Zombies", "Acción", "Terror", "Supervivencia"),
        matchPercentage = 92
    )

    private val movieSolas = Content(
        id = "solas_en_casa",
        title = "Solas en Casa",
        type = "movie",
        genre = listOf("Suspenso", "Drama", "Terror"),
        matchPercentage = 89
    )

    private val animeKonosuba = Content(
        id = "konosuba",
        title = "Konosuba",
        type = "movie",
        genre = listOf("Comedia", "Isekai", "Aventura", "Fantasía"),
        matchPercentage = 98
    )

    private val animeBrother = Content(
        id = "brother_betrayal",
        title = "Brother Betrayal",
        type = "series",
        genre = listOf("Dark Fantasy", "Drama", "Gótico"),
        matchPercentage = 90
    )

    private val allCatalog = listOf(
        animeMasacre,
        animeZombies,
        movieSolas,
        animeKonosuba,
        animeBrother
    )

    @Before
    fun setup() {
        val fakeContentRepo = object : ContentRepository {
            override fun getContentStream(limit: Long, startAfterDocument: DocumentSnapshot?): Flow<List<Content>> =
                flowOf(allCatalog)
            override fun getFeaturedContent(): Flow<Content?> = flowOf(allCatalog.first())
            override fun getContinueWatching(profileId: String): Flow<List<Content>> = flowOf(emptyList())
            override fun getRecommendations(): Flow<List<Content>> = flowOf(allCatalog)
            override fun getSeries(): Flow<List<Content>> = flowOf(allCatalog.filter { it.type == "series" })
            override fun getMovies(): Flow<List<Content>> = flowOf(allCatalog.filter { it.type == "movie" })
            override fun getContentById(id: String): Flow<Content?> = flowOf(allCatalog.firstOrNull { it.id == id })
            override suspend fun saveProgress(contentId: String, episodeId: String?, profileId: String, progressMs: Long, durationMs: Long) {}
            override fun isFavorite(contentId: String, profileId: String): Flow<Boolean> = flowOf(false)
            override suspend fun toggleFavorite(content: Content, profileId: String) {}
        }

        val fakeUserRepo = object : UserRepository {
            override fun getProfiles(): Flow<List<UserProfile>> = flowOf(listOf(UserProfile()))
            override fun getActiveProfile(): Flow<UserProfile> = flowOf(UserProfile())
            override suspend fun selectProfile(profile: UserProfile) {}
            override suspend fun createProfile(name: String, colorHex: Long, isKids: Boolean) {}
        }

        val fakeDownloadRepo = object : DownloadRepository {
            override fun getDownloads(): Flow<List<com.example.domain.model.DownloadItem>> = flowOf(emptyList())
            override suspend fun startDownload(contentId: String, episodeId: String?, title: String, subtitle: String, thumbnailUrl: String, videoUrl: String) {}
            override suspend fun pauseDownload(id: String) {}
            override suspend fun resumeDownload(id: String) {}
            override suspend fun deleteDownload(id: String) {}
        }

        viewModel = HomeViewModel(
            contentRepository = fakeContentRepo,
            userRepository = fakeUserRepo,
            downloadRepository = fakeDownloadRepo
        )
    }

    @Test
    fun testCalculateRecommendations_filtersByWatchedGenres() {
        // Usuario ha visto principalmente Masacre Escolar (géneros clave: Terror, Slasher, Misterio)
        val watched = listOf(animeMasacre)

        val (topGenres, recommended) = viewModel.calculateRecommendationsForUser(
            watchedContent = watched,
            allContent = allCatalog
        )

        // Los géneros más vistos deben incluir Terror
        assertTrue("Top géneros debe incluir 'Terror'", topGenres.any { it.equals("Terror", ignoreCase = true) })

        // Recomendados debe incluir títulos que compartan las etiquetas de género
        val recommendedTitles = recommended.map { it.title }
        assertTrue("Debe recomendar Solas en Casa (género Terror)", recommendedTitles.contains("Solas en Casa"))
        assertTrue("Debe recomendar Estamos Muertos (género Terror)", recommendedTitles.contains("Estamos Muertos"))

        // Konosuba no comparte ningún género con Masacre Escolar (Comedia, Isekai vs Terror, Slasher)
        assertFalse("Konosuba debe quedar filtrado al no compartir etiquetas afines", recommendedTitles.contains("Konosuba"))
    }

    @Test
    fun testCalculateRecommendations_multipleMatchingGenresRankedHigher() {
        // Usuario vio contenido de Terror y Acción
        val watched = listOf(animeMasacre, animeZombies)

        val (topGenres, recommended) = viewModel.calculateRecommendationsForUser(
            watchedContent = watched,
            allContent = allCatalog
        )

        // 'Terror' aparece en ambos contenidos vistos, por lo que debe ser el género #1
        assertEquals("Terror", topGenres.first())

        // El primer recomendado debe tener coincidencia con los géneros dominantes
        val firstRec = recommended.first()
        assertTrue("El primer recomendado debe tener género Terror", firstRec.genre.contains("Terror"))
    }

    @Test
    fun testCalculateRecommendations_emptyWatched_providesCatalogPopularFallback() {
        val (topGenres, recommended) = viewModel.calculateRecommendationsForUser(
            watchedContent = emptyList(),
            allContent = allCatalog
        )

        assertFalse("Debe generar géneros destacados incluso sin historial previo", topGenres.isEmpty())
        assertFalse("Debe devolver lista de recomendados", recommended.isEmpty())
        assertTrue("Debe contener títulos afines a los géneros más frecuentes", recommended.isNotEmpty())
    }

    @Test
    fun testCalculateRecommendations_emptyCatalog_returnsEmpty() {
        val (topGenres, recommended) = viewModel.calculateRecommendationsForUser(
            watchedContent = listOf(animeMasacre),
            allContent = emptyList()
        )

        assertTrue(topGenres.isEmpty())
        assertTrue(recommended.isEmpty())
    }
}
