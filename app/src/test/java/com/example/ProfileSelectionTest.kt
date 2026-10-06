package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.datasource.local.UserPreferencesDataStore
import com.example.data.repository.UserRepositoryImpl
import com.example.domain.model.Content
import com.example.domain.model.UserProfile
import com.example.domain.repository.ContentRepository
import com.example.domain.repository.DownloadRepository
import com.example.presentation.home.HomeViewModel
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class ProfileSelectionTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var context: Context
    private lateinit var preferencesDataStore: UserPreferencesDataStore
    private lateinit var userRepository: UserRepositoryImpl

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        preferencesDataStore = UserPreferencesDataStore(context)
        // Supply null firestoreProvider to use local/offline fallback seamlessly
        userRepository = UserRepositoryImpl(
            preferencesDataStore = preferencesDataStore,
            firestoreProvider = { null }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetProfiles_returnsAvailableProfiles() = testScope.runTest {
        val profiles = userRepository.getProfiles().first()
        assertTrue("Available profiles should not be empty", profiles.isNotEmpty())
        assertEquals(4, profiles.size)
        assertTrue("Should include Kids profile", profiles.any { it.isKids })
        assertTrue("Should include Seiko profile", profiles.any { it.name == "Seiko" })
    }

    @Test
    fun testSelectProfile_persistsActiveProfileIdInDataStore() = testScope.runTest {
        val profiles = userRepository.getProfiles().first()
        val kidsProfile = profiles.first { it.isKids }

        userRepository.selectProfile(kidsProfile)
        advanceUntilIdle()

        val persistedId = preferencesDataStore.activeProfileId.first()
        assertEquals(kidsProfile.id, persistedId)

        val activeProfile = userRepository.getActiveProfile().first()
        assertEquals(kidsProfile.id, activeProfile.id)
        assertTrue(activeProfile.isKids)
    }

    @Test
    fun testCreateProfile_addsNewProfile() = testScope.runTest {
        userRepository.createProfile("Invitado", 0xFF3B82F6, false)
        advanceUntilIdle()

        val profiles = userRepository.getProfiles().first()
        assertTrue("Should contain newly created profile", profiles.any { it.name == "Invitado" })
    }

    @Test
    fun testHomeContentFiltering_kidsProfileFiltersRestrictedContent() = testScope.runTest {
        val allContent = listOf(
            Content(
                id = "c1",
                title = "Anime Familiar Aventura",
                rating = "TP",
                genre = listOf("Animación", "Aventura"),
                type = "series"
            ),
            Content(
                id = "c2",
                title = "Película Terror Gore +18",
                rating = "+18",
                genre = listOf("Terror", "Gore"),
                type = "movie"
            ),
            Content(
                id = "c3",
                title = "Serie Acción Juvenil",
                rating = "13+",
                genre = listOf("Acción", "Fantasía"),
                type = "series"
            )
        )

        val fakeContentRepo = object : ContentRepository {
            override fun getContentStream(limit: Long, startAfterDocument: DocumentSnapshot?): Flow<List<Content>> =
                flowOf(allContent)

            override fun getFeaturedContent(): Flow<Content?> = flowOf(allContent.first())
            override fun getContinueWatching(profileId: String): Flow<List<Content>> = flowOf(emptyList())
            override fun getRecommendations(): Flow<List<Content>> = flowOf(allContent)
            override fun getSeries(): Flow<List<Content>> = flowOf(allContent.filter { it.type == "series" })
            override fun getMovies(): Flow<List<Content>> = flowOf(allContent.filter { it.type == "movie" })
            override fun getContentById(id: String): Flow<Content?> = flowOf(allContent.firstOrNull { it.id == id })
            override suspend fun saveProgress(contentId: String, episodeId: String?, profileId: String, progressMs: Long, durationMs: Long) {}
            override fun isFavorite(contentId: String, profileId: String): Flow<Boolean> = flowOf(false)
            override suspend fun toggleFavorite(content: Content, profileId: String) {}
        }

        val fakeDownloadRepo = object : DownloadRepository {
            override fun getDownloads(): Flow<List<com.example.domain.model.DownloadItem>> =
                flowOf(emptyList())
            override suspend fun startDownload(contentId: String, episodeId: String?, title: String, subtitle: String, thumbnailUrl: String, videoUrl: String) {}
            override suspend fun pauseDownload(id: String) {}
            override suspend fun resumeDownload(id: String) {}
            override suspend fun deleteDownload(id: String) {}
        }

        // 1. Test Adult/Normal Profile: sees all content including +18
        val normalProfile = UserProfile(id = "p1", name = "Seiko", isKids = false)
        userRepository.selectProfile(normalProfile)
        advanceUntilIdle()

        val viewModel = HomeViewModel(fakeContentRepo, userRepository, fakeDownloadRepo)
        advanceUntilIdle()

        val normalState = viewModel.uiState.value
        assertEquals(3, normalState.recommendations.size)
        assertTrue(normalState.recommendations.any { it.title.contains("Terror") })

        // 2. Switch to Kids Profile: filters out restricted content
        val kidsProfile = UserProfile(id = "p4", name = "Kids", isKids = true)
        userRepository.selectProfile(kidsProfile)
        advanceUntilIdle()

        val kidsState = viewModel.uiState.value
        assertTrue("Kids active state should be kids profile", kidsState.activeProfile.isKids)
        assertFalse(
            "Kids mode should filter out +18 Terror content",
            kidsState.recommendations.any { it.title.contains("Terror") || it.rating == "+18" }
        )
        assertTrue(kidsState.recommendations.any { it.title == "Anime Familiar Aventura" })
    }
}
