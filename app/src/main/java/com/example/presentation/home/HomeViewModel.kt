package com.example.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Content
import com.example.domain.model.UserProfile
import com.example.domain.repository.ContentRepository
import com.example.domain.repository.DownloadRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val featured: Content? = null,
    val continueWatching: List<Content> = emptyList(),
    val recommendations: List<Content> = emptyList(),
    val recommendedForYou: List<Content> = emptyList(),
    val userTopGenres: List<String> = emptyList(),
    val series: List<Content> = emptyList(),
    val movies: List<Content> = emptyList(),
    val myList: List<Content> = emptyList(),
    val activeProfile: UserProfile = UserProfile(),
    val selectedFilter: String = "TODOS", // TODOS, RECIENTES, MÁS VISTOS
    val searchQuery: String = "",
    val searchResults: List<Content> = emptyList(),
    val selectedContentForDetail: Content? = null
)

class HomeViewModel(
    private val contentRepository: ContentRepository,
    private val userRepository: UserRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        // Combinar el perfil activo persistido en DataStore con el stream de contenido de Firestore
        viewModelScope.launch {
            combine(
                userRepository.getActiveProfile(),
                contentRepository.getContentStream()
            ) { activeProfile, allContent ->
                // Filtrar catálogo si el perfil activo es KIDS (infantil)
                val filteredContent = if (activeProfile.isKids) {
                    allContent.filter { item ->
                        val isRestrictedRating = item.rating.contains("18") ||
                                item.rating.contains("16") ||
                                item.rating.equals("R", ignoreCase = true) ||
                                item.rating.equals("NC-17", ignoreCase = true) ||
                                item.rating.equals("TV-MA", ignoreCase = true)
                        val isAdultGenre = item.genre.any { g ->
                            g.contains("Terror", ignoreCase = true) ||
                            g.contains("Gore", ignoreCase = true) ||
                            g.contains("Adulto", ignoreCase = true) ||
                            g.contains("Horror", ignoreCase = true) ||
                            g.contains("Violencia", ignoreCase = true)
                        }
                        !isRestrictedRating && !isAdultGenre
                    }
                } else {
                    allContent
                }

                val featured = filteredContent.firstOrNull { it.featured } ?: filteredContent.firstOrNull()
                val series = filteredContent.filter { it.type == "series" }
                val movies = filteredContent.filter { it.type == "movie" }

                val (topGenres, recommended) = calculateRecommendationsForUser(
                    watchedContent = _uiState.value.continueWatching,
                    allContent = filteredContent
                )

                _uiState.update { state ->
                    state.copy(
                        activeProfile = activeProfile,
                        featured = featured,
                        recommendations = filteredContent,
                        recommendedForYou = recommended,
                        userTopGenres = topGenres,
                        series = series,
                        movies = movies
                    )
                }
            }.collect()
        }

        // Observar 'Seguir Viendo' filtrado específicamente por el ID del perfil activo
        viewModelScope.launch {
            userRepository.getActiveProfile().collect { profile ->
                contentRepository.getContinueWatching(profile.id).collect { profileProgress ->
                    _uiState.update { state ->
                        val watched = if (profileProgress.isNotEmpty()) {
                            profileProgress
                        } else if (!profile.isKids) {
                            state.recommendations.take(3).map { it.copy(progressMs = 300000L, totalDurationMs = 600000L) }
                        } else {
                            emptyList()
                        }

                        val (topGenres, recommended) = calculateRecommendationsForUser(
                            watchedContent = watched,
                            allContent = state.recommendations
                        )

                        state.copy(
                            continueWatching = watched,
                            recommendedForYou = recommended,
                            userTopGenres = topGenres
                        )
                    }
                }
            }
        }
    }

    /**
     * Calcula la lista de 'Recomendados para ti' analizando las etiquetas de género
     * de las series y animes más vistos por el usuario en su historial de reproducción.
     */
    fun calculateRecommendationsForUser(
        watchedContent: List<Content>,
        allContent: List<Content>
    ): Pair<List<String>, List<Content>> {
        if (allContent.isEmpty()) return emptyList<String>() to emptyList()

        // 1. Contabilizar frecuencia de etiquetas de género en el contenido más visto
        val genreFrequency = mutableMapOf<String, Int>()
        watchedContent.forEach { item ->
            item.genre.forEach { rawGenre ->
                val g = rawGenre.trim()
                if (g.isNotEmpty()) {
                    genreFrequency[g] = (genreFrequency[g] ?: 0) + 1
                }
            }
        }

        // Obtener los géneros top más vistos por el usuario
        val topGenres = if (genreFrequency.isNotEmpty()) {
            genreFrequency.entries
                .sortedByDescending { it.value }
                .map { it.key }
                .take(3)
        } else {
            // Si el usuario aún no tiene reproducciones registradas, usar géneros destacados del catálogo
            allContent.flatMap { it.genre }
                .groupingBy { it.trim() }
                .eachCount()
                .entries
                .filter { it.key.isNotEmpty() }
                .sortedByDescending { it.value }
                .map { it.key }
                .take(3)
        }

        val watchedIds = watchedContent.map { it.id }.toSet()

        // 2. Filtrar el catálogo para encontrar títulos que compartan esas etiquetas de género
        val filtered = allContent.filter { item ->
            item.genre.any { itemGenre ->
                topGenres.any { top ->
                    top.equals(itemGenre.trim(), ignoreCase = true) ||
                    itemGenre.trim().contains(top, ignoreCase = true) ||
                    top.contains(itemGenre.trim(), ignoreCase = true)
                }
            }
        }.sortedWith(
            compareByDescending<Content> { item ->
                // Ponderar por cantidad de géneros coincidentes
                item.genre.count { itemGenre ->
                    topGenres.any { top ->
                        top.equals(itemGenre.trim(), ignoreCase = true) ||
                        itemGenre.trim().contains(top, ignoreCase = true) ||
                        top.contains(itemGenre.trim(), ignoreCase = true)
                    }
                }
            }.thenByDescending { item ->
                // Dar prioridad a títulos que el usuario aún no haya completado
                if (watchedIds.contains(item.id)) 0 else 1
            }.thenByDescending { item ->
                item.matchPercentage
            }
        )

        val resultList = if (filtered.isNotEmpty()) filtered else allContent

        return topGenres to resultList
    }

    fun setFilter(filter: String) {
        val currentAll = _uiState.value.series + _uiState.value.movies
        val filtered = when (filter) {
            "RECIENTES" -> currentAll.sortedByDescending { it.releaseYear }
            "MÁS VISTOS" -> currentAll.sortedByDescending { it.matchPercentage }
            else -> currentAll
        }
        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            recommendations = filtered
        )
    }

    fun updateSearchQuery(query: String) {
        val all = _uiState.value.series + _uiState.value.movies
        val results = if (query.isBlank()) emptyList() else {
            all.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.genre.any { g -> g.contains(query, ignoreCase = true) } ||
                        it.actors?.contains(query, ignoreCase = true) == true
            }
        }
        _uiState.value = _uiState.value.copy(searchQuery = query, searchResults = results)
    }

    fun openDetail(content: Content) {
        val catalogMatch = com.example.data.datasource.remote.DefaultContentCatalog.getAll().firstOrNull {
            it.id.equals(content.id, ignoreCase = true) || it.title.equals(content.title, ignoreCase = true)
        }
        val effectiveContent = if (catalogMatch != null && catalogMatch.seasons.size > content.seasons.size) {
            val availableEps = content.seasons.flatMap { it.episodes }.associateBy { it.id }
            val enrichedSeasons = catalogMatch.seasons.map { season ->
                season.copy(
                    episodes = season.episodes.map { catEp ->
                        availableEps[catEp.id]?.let { existing ->
                            catEp.copy(
                                videoUrl = if (existing.videoUrl.isNotBlank() && !existing.videoUrl.contains("Intro.mp4", ignoreCase = true)) existing.videoUrl else catEp.videoUrl,
                                thumbnailUrl = existing.thumbnailUrl.ifBlank { catEp.thumbnailUrl }
                            )
                        } ?: catEp
                    }
                )
            }
            content.copy(type = "series", seasons = enrichedSeasons)
        } else {
            content
        }
        _uiState.value = _uiState.value.copy(selectedContentForDetail = effectiveContent)
    }

    fun closeDetail() {
        _uiState.value = _uiState.value.copy(selectedContentForDetail = null)
    }

    fun toggleFavorite(content: Content) {
        viewModelScope.launch {
            contentRepository.toggleFavorite(content, _uiState.value.activeProfile.id)
        }
    }

    fun startDownload(content: Content, episode: com.example.domain.model.Episode) {
        viewModelScope.launch {
            val resolvedVideoUrl = if (com.example.presentation.player.PlayerViewModel.isActualContentUrl(episode.videoUrl)) {
                com.example.presentation.player.PlayerViewModel.ensureAbsoluteCdnUrl(episode.videoUrl)
            } else if (com.example.presentation.player.PlayerViewModel.isActualContentUrl(content.videoUrl)) {
                com.example.presentation.player.PlayerViewModel.ensureAbsoluteCdnUrl(content.videoUrl)
            } else {
                val catalogMatch = com.example.presentation.player.PlayerViewModel.findCatalogMatch(content)
                val catalogEp = catalogMatch?.seasons?.flatMap { it.episodes }
                    ?.firstOrNull { it.episodeNumber == episode.episodeNumber }?.videoUrl
                catalogEp ?: catalogMatch?.videoUrl ?: com.example.presentation.player.PlayerViewModel.getFallbackStream(content.id, episode.id)
            }

            val subtitleText = if (content.type == "series") {
                "T1 • E${episode.episodeNumber}: ${episode.title}"
            } else {
                content.title
            }

            downloadRepository.startDownload(
                contentId = content.id,
                episodeId = episode.id,
                title = content.title,
                subtitle = subtitleText,
                thumbnailUrl = episode.thumbnailUrl.ifEmpty { content.thumbnailUrl },
                videoUrl = resolvedVideoUrl
            )
        }
    }

    fun saveProgress(contentId: String, episodeId: String?, progressMs: Long, durationMs: Long) {
        viewModelScope.launch {
            contentRepository.saveProgress(
                contentId = contentId,
                episodeId = episodeId,
                profileId = _uiState.value.activeProfile.id,
                progressMs = progressMs,
                durationMs = durationMs
            )
        }
    }
}
