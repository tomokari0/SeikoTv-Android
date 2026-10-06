package com.example.presentation.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.SeikoApplication
import com.example.domain.model.Content
import com.example.domain.model.Episode
import com.example.presentation.downloads.DownloadsScreen
import com.example.presentation.downloads.DownloadsViewModel
import com.example.presentation.home.HomeScreen
import com.example.presentation.home.HomeViewModel
import com.example.presentation.player.PlayerScreen
import com.example.presentation.profiles.ProfileSelectionScreen
import com.example.presentation.splash.IntroVideoSplashScreen
import com.example.ui.theme.SeikoBlack
import kotlinx.coroutines.launch

@Composable
fun SeikoNavHost(
    app: SeikoApplication,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var currentBottomRoute by remember { mutableStateOf(Screen.Home.route) }

    // Active playback state
    var activePlaybackContent by remember { mutableStateOf<Content?>(null) }
    var activePlaybackEpisode by remember { mutableStateOf<Episode?>(null) }

    // ViewModels
    val homeViewModel = remember {
        HomeViewModel(
            contentRepository = app.contentRepository,
            userRepository = app.userRepository,
            downloadRepository = app.downloadRepository
        )
    }
    val homeUiState by homeViewModel.uiState.collectAsState()

    val downloadsViewModel = remember {
        DownloadsViewModel(app.downloadRepository)
    }
    val downloadsList by downloadsViewModel.downloads.collectAsState()

    val profilesList by app.userRepository.getProfiles().collectAsState(initial = emptyList())

    Crossfade(
        targetState = currentScreen,
        label = "ScreenTransition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            is Screen.Splash -> {
                IntroVideoSplashScreen(
                    onFinish = {
                        currentScreen = Screen.Profiles
                    }
                )
            }
            is Screen.Profiles -> {
                ProfileSelectionScreen(
                    profiles = profilesList,
                    onSelectProfile = { profile ->
                        coroutineScope.launch {
                            app.userRepository.selectProfile(profile)
                            currentScreen = Screen.Home
                            currentBottomRoute = Screen.Home.route
                        }
                    },
                    onCreateProfile = { name, colorHex, isKids ->
                        coroutineScope.launch {
                            app.userRepository.createProfile(
                                name = name,
                                colorHex = colorHex,
                                isKids = isKids
                            )
                        }
                    },
                    onAddProfile = {
                        coroutineScope.launch {
                            app.userRepository.createProfile(
                                name = "Usuario ${profilesList.size + 1}",
                                colorHex = 0xFF8B5CF6,
                                isKids = false
                            )
                        }
                    }
                )
            }
            is Screen.Player -> {
                activePlaybackContent?.let { content ->
                    val resolvedInitialEpisode = activePlaybackEpisode
                        ?: (if (content.type == "series") content.seasons.firstOrNull()?.episodes?.firstOrNull() ?: content.seasons.flatMap { it.episodes }.firstOrNull() else null)
                    PlayerScreen(
                        content = content,
                        initialEpisode = resolvedInitialEpisode,
                        onBack = {
                            currentScreen = Screen.Home
                            activePlaybackContent = null
                            activePlaybackEpisode = null
                        },
                        onSaveProgress = { epId, progressMs, durationMs ->
                            homeViewModel.saveProgress(
                                contentId = content.id,
                                episodeId = epId ?: resolvedInitialEpisode?.id,
                                progressMs = progressMs,
                                durationMs = durationMs
                            )
                        }
                    )
                } ?: run {
                    currentScreen = Screen.Home
                }
            }
            else -> {
                // Main Shell with Bottom Navigation Bar
                Scaffold(
                    bottomBar = {
                        SeikoBottomNavBar(
                            currentRoute = currentBottomRoute,
                            onNavigate = { route ->
                                currentBottomRoute = route
                                when (route) {
                                    Screen.Home.route -> currentScreen = Screen.Home
                                    Screen.Series.route -> currentScreen = Screen.Series
                                    Screen.Movies.route -> currentScreen = Screen.Movies
                                    Screen.Downloads.route -> currentScreen = Screen.Downloads
                                    Screen.Profiles.route -> currentScreen = Screen.Profiles
                                }
                            }
                        )
                    },
                    containerColor = SeikoBlack
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                            .background(SeikoBlack)
                    ) {
                        when (currentBottomRoute) {
                            Screen.Home.route, Screen.Series.route, Screen.Movies.route -> {
                                val displayedUiState = when (currentBottomRoute) {
                                    Screen.Series.route -> homeUiState.copy(
                                        recommendations = homeUiState.series,
                                        featured = homeUiState.series.firstOrNull() ?: homeUiState.featured
                                    )
                                    Screen.Movies.route -> homeUiState.copy(
                                        recommendations = homeUiState.movies,
                                        featured = homeUiState.movies.firstOrNull() ?: homeUiState.featured
                                    )
                                    else -> homeUiState
                                }

                                HomeScreen(
                                    uiState = displayedUiState,
                                    onPlayContent = { content, episode ->
                                        val candidateId = episode?.id ?: content.id
                                        val downloadedItem = downloadsList.firstOrNull {
                                            (it.id == candidateId || it.contentId == content.id) &&
                                                    !it.localFilePath.isNullOrBlank() &&
                                                    java.io.File(it.localFilePath).exists()
                                        }
                                        if (downloadedItem != null) {
                                            val localFile = java.io.File(downloadedItem.localFilePath!!)
                                            val fileUri = android.net.Uri.fromFile(localFile).toString()
                                            val effectiveEpisode = episode?.copy(videoUrl = fileUri)
                                            val effectiveContent = content.copy(videoUrl = fileUri)
                                            activePlaybackContent = effectiveContent
                                            activePlaybackEpisode = effectiveEpisode
                                        } else {
                                            activePlaybackContent = content
                                            activePlaybackEpisode = episode
                                        }
                                        currentScreen = Screen.Player
                                    },
                                    onOpenDetail = { content ->
                                        homeViewModel.openDetail(content)
                                    },
                                    onCloseDetail = {
                                        homeViewModel.closeDetail()
                                    },
                                    onToggleMyList = { content ->
                                        homeViewModel.toggleFavorite(content)
                                    },
                                    onDownloadEpisode = { content, episode ->
                                        homeViewModel.startDownload(content, episode)
                                    },
                                    onFilterChange = { filter ->
                                        homeViewModel.setFilter(filter)
                                    },
                                    onSearchChange = { query ->
                                        homeViewModel.updateSearchQuery(query)
                                    },
                                    onProfileClick = {
                                        currentScreen = Screen.Profiles
                                    },
                                    onNavigateToDownloads = {
                                        currentBottomRoute = Screen.Downloads.route
                                        currentScreen = Screen.Downloads
                                    }
                                )
                            }
                            Screen.Downloads.route -> {
                                DownloadsScreen(
                                    downloads = downloadsList,
                                    onPlayOffline = { item ->
                                        // Play downloaded item
                                        val filePathOrUrl = item.localFilePath?.takeIf { it.isNotBlank() && java.io.File(it).exists() }
                                            ?: item.videoUrl
                                        val offlineContent = Content(
                                            id = item.contentId,
                                            title = item.title,
                                            description = item.subtitle,
                                            thumbnailUrl = item.thumbnailUrl,
                                            videoUrl = filePathOrUrl
                                        )
                                        activePlaybackContent = offlineContent
                                        activePlaybackEpisode = null
                                        currentScreen = Screen.Player
                                    },
                                    onPause = { downloadsViewModel.pauseDownload(it) },
                                    onResume = { downloadsViewModel.resumeDownload(it) },
                                    onDelete = { downloadsViewModel.deleteDownload(it) },
                                    onExploreCatalog = {
                                        currentBottomRoute = Screen.Home.route
                                        currentScreen = Screen.Home
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
