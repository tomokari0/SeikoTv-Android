package com.example.presentation.player

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datasource.remote.DefaultContentCatalog
import com.example.domain.model.Content
import com.example.domain.model.Episode
import com.example.domain.model.SkipSegments
import com.example.domain.repository.ContentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder

enum class PlaybackSourceType {
    UNKNOWN,
    EPISODE_FIRESTORE,
    MOVIE_FIRESTORE,
    CATALOG_FALLBACK,
    STREAM_BACKUP,
    LOCAL_OFFLINE
}

data class PlayableUrlResult(
    val url: String,
    val sourceType: PlaybackSourceType,
    val isAppIntroFiltered: Boolean,
    val originalUrl: String
)

data class PlayerUiState(
    val content: Content? = null,
    val currentEpisode: Episode? = null,
    val activeVideoUrl: String = "",
    val sourceType: PlaybackSourceType = PlaybackSourceType.UNKNOWN,
    val wasIntroVideoFiltered: Boolean = false,
    val isPlaying: Boolean = true,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val skipSegments: SkipSegments? = null,
    val isSkipIntroActive: Boolean = false,
    val errorMessage: String? = null,
    val selectedSubtitleId: String = "none",
    val selectedAudioId: String = "ja",
    val currentSubtitleText: String? = null,
    val audioFeedbackToast: String? = null
)

class PlayerViewModel(
    private val contentRepository: ContentRepository? = null
) : ViewModel() {

    companion object {
        private const val TAG = "PlayerViewModel"

        /**
         * Official SeikoTV branding video URL used strictly for the startup splash screen.
         * Must NEVER be played as the actual series or movie content stream.
         */
        const val APP_INTRO_VIDEO_URL = "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/Intro.mp4"

        const val R2_CDN_BASE = "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev"

        /**
         * Determines whether a given URL is the SeikoTV App Intro video.
         * Strips query parameters, hashes, whitespace, and tests against intro video patterns.
         */
        fun isAppIntroVideo(url: String?): Boolean {
            if (url.isNullOrBlank()) return true
            val trimmed = url.trim()

            // 1. Exact canonical URL match
            if (trimmed.equals(APP_INTRO_VIDEO_URL, ignoreCase = true)) {
                return true
            }

            // 2. Decode URL entities if needed
            val decoded = try {
                URLDecoder.decode(trimmed, "UTF-8")
            } catch (_: Exception) {
                trimmed
            }

            // 3. Path-based check without query parameters or anchors
            val cleanPath = try {
                val uri = Uri.parse(decoded)
                uri.path ?: decoded
            } catch (_: Exception) {
                decoded.substringBefore("?").substringBefore("#")
            }

            val lowerPath = cleanPath.lowercase().trim()
            val lowerRaw = decoded.lowercase().trim()

            return lowerPath.endsWith("/videos/intro.mp4") ||
                    lowerPath.endsWith("intro.mp4") ||
                    lowerPath.contains("/videos/intro") ||
                    lowerPath.contains("seikotv_intro") ||
                    lowerPath.contains("seiko_intro") ||
                    lowerPath.endsWith("/intro") ||
                    lowerPath == "intro" ||
                    lowerPath == "/intro" ||
                    lowerRaw.contains("/videos/intro.mp4") ||
                    lowerRaw.endsWith("intro.mp4") ||
                    lowerRaw == "intro.mp4" ||
                    lowerRaw == "/intro.mp4" ||
                    lowerRaw == "null" ||
                    lowerRaw == "undefined"
        }

        /**
         * Detects unsupported web embed URLs (e.g. HTML pages from streamtape, uqload, or local blob URLs).
         */
        fun isUnsupportedEmbed(url: String?): Boolean {
            if (url.isNullOrBlank()) return false
            val lower = url.lowercase().trim()
            return lower.startsWith("blob:") ||
                    lower.contains("streamtape.com/e/") ||
                    lower.contains("uqload.bz/embed") ||
                    lower.contains("/embed-") ||
                    lower.contains("drive.google.com/file") ||
                    lower.contains("mega.nz") ||
                    lower.contains("youtube.com/watch") ||
                    lower.contains("youtu.be/")
        }

        /**
         * Returns true if the URL is valid, non-blank, not the app intro video,
         * and not an unsupported web embed.
         */
        fun isActualContentUrl(url: String?): Boolean {
            if (url.isNullOrBlank()) return false
            if (isAppIntroVideo(url)) return false
            if (isUnsupportedEmbed(url)) return false
            return true
        }

        fun ensureAbsoluteCdnUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim()
            if (trimmed.isEmpty()) return ""
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://") ||
                trimmed.startsWith("file://") || trimmed.startsWith("content://") ||
                trimmed.startsWith("/data/") || trimmed.startsWith("/storage/") ||
                trimmed.startsWith("/sdcard/") || java.io.File(trimmed).exists()) {
                return trimmed
            }
            val cleanPath = if (trimmed.startsWith("/")) trimmed.substring(1) else trimmed
            return "$R2_CDN_BASE/$cleanPath"
        }

        fun getFallbackStream(contentId: String, episodeId: String? = null): String {
            val idHash = kotlin.math.abs(contentId.hashCode() + (episodeId?.hashCode() ?: 0))
            return when (idHash % 4) {
                0 -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                1 -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
                2 -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
                else -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
            }
        }

        fun findCatalogMatch(content: Content): Content? {
            val allCatalog = DefaultContentCatalog.getAll()
            return allCatalog.firstOrNull {
                it.id.equals(content.id, ignoreCase = true)
            } ?: allCatalog.firstOrNull {
                it.title.trim().equals(content.title.trim(), ignoreCase = true)
            } ?: allCatalog.firstOrNull {
                val cleanA = content.title.replace(Regex("""[^a-zA-Z0-9]"""), "").lowercase()
                val cleanB = it.title.replace(Regex("""[^a-zA-Z0-9]"""), "").lowercase()
                cleanA.isNotEmpty() && cleanB.isNotEmpty() && (cleanA.contains(cleanB) || cleanB.contains(cleanA))
            }
        }

        fun resolvePlayableUrlStatic(content: Content, episode: Episode?): PlayableUrlResult {
            return PlayerViewModel(null).resolvePlayableUrl(content, episode)
        }
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var liveContentObserverJob: Job? = null

    /**
     * Initializes playback for a given Content and optional initial Episode.
     * Accurately determines the active episode and resolves a valid playable URL
     * while guaranteeing that the SeikoTV app intro video is NEVER played as content.
     */
    fun initPlayback(content: Content, initialEpisode: Episode? = null) {
        liveContentObserverJob?.cancel()

        // 1. Enrich content with catalog seasons/episodes if currently empty for series
        val effectiveContent = enrichContentIfEmpty(content)
        val resolvedEpisode = resolveActiveEpisode(effectiveContent, initialEpisode)
        val resolution = resolvePlayableUrl(effectiveContent, resolvedEpisode)

        val activeSkip = resolvedEpisode?.skipSegments ?: effectiveContent.skipSegments

        _uiState.update {
            it.copy(
                content = effectiveContent,
                currentEpisode = resolvedEpisode,
                activeVideoUrl = resolution.url,
                sourceType = resolution.sourceType,
                wasIntroVideoFiltered = resolution.isAppIntroFiltered,
                isPlaying = true,
                currentPositionMs = effectiveContent.progressMs,
                totalDurationMs = 0L,
                skipSegments = activeSkip,
                errorMessage = null
            )
        }

        Log.d(TAG, "initPlayback: Content='${effectiveContent.title}', Type=${effectiveContent.type}, " +
                "Ep='${resolvedEpisode?.title}', Resolution=$resolution")

        // 2. Observe Firestore live updates in background if contentRepository is available
        observeLiveFirestoreUpdates(effectiveContent)
    }

    private fun enrichContentIfEmpty(content: Content): Content {
        val catalogMatch = findCatalogMatch(content) ?: return content
        if (content.type.equals("series", ignoreCase = true) || content.seasons.isNotEmpty() || catalogMatch.type.equals("series", ignoreCase = true)) {
            if (catalogMatch.seasons.isNotEmpty()) {
                if (content.seasons.isEmpty() || (content.seasons.size == 1 && catalogMatch.seasons.size > 1)) {
                    Log.d(TAG, "Enriching series '${content.title}' with ${catalogMatch.seasons.size} seasons from catalog.")
                    val availableEps = content.seasons.flatMap { it.episodes }.associateBy { it.id }
                    val enrichedSeasons = catalogMatch.seasons.map { season ->
                        season.copy(
                            episodes = season.episodes.map { catEp ->
                                availableEps[catEp.id]?.let { existing ->
                                    catEp.copy(
                                        videoUrl = if (isActualContentUrl(existing.videoUrl)) existing.videoUrl else catEp.videoUrl,
                                        thumbnailUrl = existing.thumbnailUrl.ifBlank { catEp.thumbnailUrl }
                                    )
                                } ?: catEp
                            }
                        )
                    }
                    return content.copy(
                        type = "series",
                        seasons = enrichedSeasons,
                        videoUrl = enrichedSeasons.firstOrNull()?.episodes?.firstOrNull()?.videoUrl ?: content.videoUrl
                    )
                }
            }
        }
        return content
    }

    private fun observeLiveFirestoreUpdates(currentContent: Content) {
        val repo = contentRepository ?: return
        liveContentObserverJob = viewModelScope.launch {
            try {
                repo.getContentById(currentContent.id).collect { rawUpdated ->
                    if (rawUpdated != null && _uiState.value.content?.id == rawUpdated.id) {
                        val updated = enrichContentIfEmpty(rawUpdated)
                        val activeEp = _uiState.value.currentEpisode
                        if (activeEp != null) {
                            val matchedEp = updated.seasons.flatMap { it.episodes }.firstOrNull {
                                it.id == activeEp.id || (it.seasonNumber == activeEp.seasonNumber && it.episodeNumber == activeEp.episodeNumber)
                            }
                            if (matchedEp != null) {
                                val currentUrl = _uiState.value.activeVideoUrl
                                if ((currentUrl.isBlank() || isAppIntroVideo(currentUrl)) && isActualContentUrl(matchedEp.videoUrl)) {
                                    val res = resolvePlayableUrl(updated, matchedEp)
                                    _uiState.update { state ->
                                        state.copy(
                                            content = updated,
                                            currentEpisode = matchedEp,
                                            activeVideoUrl = res.url,
                                            sourceType = res.sourceType,
                                            wasIntroVideoFiltered = res.isAppIntroFiltered,
                                            skipSegments = matchedEp.skipSegments ?: updated.skipSegments
                                        )
                                    }
                                } else {
                                    _uiState.update { state ->
                                        state.copy(
                                            content = updated,
                                            currentEpisode = matchedEp
                                        )
                                    }
                                }
                            } else {
                                _uiState.update { it.copy(content = updated) }
                            }
                        } else {
                            val firstEp = updated.seasons.firstOrNull()?.episodes?.firstOrNull()
                            _uiState.update { it.copy(content = updated, currentEpisode = firstEp) }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Live Firestore observer cancelled: ${e.message}")
            }
        }
    }

    /**
     * Changes active episode when user selects an episode from drawer or list.
     */
    fun selectEpisode(episode: Episode) {
        val currentContent = _uiState.value.content ?: return
        val resolution = resolvePlayableUrl(currentContent, episode)
        val activeSkip = episode.skipSegments ?: currentContent.skipSegments

        _uiState.update {
            it.copy(
                currentEpisode = episode,
                activeVideoUrl = resolution.url,
                sourceType = resolution.sourceType,
                wasIntroVideoFiltered = resolution.isAppIntroFiltered,
                isPlaying = true,
                currentPositionMs = episode.progressMs,
                totalDurationMs = 0L,
                skipSegments = activeSkip
            )
        }
        Log.d(TAG, "selectEpisode: Ep='${episode.title}', Resolution=$resolution")
    }

    /**
     * Updates playback position and recalculates skip intro and subtitle status.
     */
    fun updatePosition(currentMs: Long, durationMs: Long, exoCueText: String? = null) {
        val current = _uiState.value
        val skip = current.skipSegments
        val isSkipIntro = skip != null &&
                currentMs >= (skip.introStart * 1000).toLong() &&
                currentMs <= (skip.introEnd * 1000).toLong()

        val subtitleText = SubtitleManager.getSubtitle(
            positionMs = currentMs,
            subtitleId = current.selectedSubtitleId,
            content = current.content,
            episode = current.currentEpisode,
            exoCueText = exoCueText
        )

        _uiState.update {
            it.copy(
                currentPositionMs = currentMs,
                totalDurationMs = durationMs.coerceAtLeast(it.totalDurationMs),
                isSkipIntroActive = isSkipIntro,
                currentSubtitleText = subtitleText
            )
        }
    }

    fun selectSubtitle(subtitleId: String) {
        _uiState.update { state ->
            val subtitleText = SubtitleManager.getSubtitle(
                positionMs = state.currentPositionMs,
                subtitleId = subtitleId,
                content = state.content,
                episode = state.currentEpisode
            )
            state.copy(
                selectedSubtitleId = subtitleId,
                currentSubtitleText = subtitleText
            )
        }
    }

    fun selectAudioTrack(audioId: String, audioLabel: String) {
        _uiState.update {
            it.copy(
                selectedAudioId = audioId,
                audioFeedbackToast = audioLabel
            )
        }
    }

    fun clearAudioToast() {
        _uiState.update { it.copy(audioFeedbackToast = null) }
    }

    fun setPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }

    /**
     * Recovers from player errors (e.g., network failure, unsupported codec or 404).
     */
    fun handlePlayerError(error: Throwable?) {
        val current = _uiState.value
        val contentId = current.content?.id ?: "unknown"
        val fallback = getFallbackStream(contentId, current.currentEpisode?.id)

        Log.e(TAG, "handlePlayerError: ${error?.message}. Switching to fallback: $fallback")

        _uiState.update {
            it.copy(
                activeVideoUrl = fallback,
                sourceType = PlaybackSourceType.STREAM_BACKUP,
                errorMessage = "Reconectando transmisión multimedia..."
            )
        }
    }

    fun saveProgress(profileId: String) {
        val state = _uiState.value
        val content = state.content ?: return
        val repo = contentRepository ?: return

        viewModelScope.launch {
            try {
                repo.saveProgress(
                    contentId = content.id,
                    episodeId = state.currentEpisode?.id,
                    profileId = profileId,
                    progressMs = state.currentPositionMs,
                    durationMs = state.totalDurationMs
                )
            } catch (e: Exception) {
                Log.w(TAG, "saveProgress failed: ${e.message}")
            }
        }
    }

    // Instance accessors
    fun isAppIntroVideo(url: String?): Boolean = Companion.isAppIntroVideo(url)
    fun isUnsupportedEmbed(url: String?): Boolean = Companion.isUnsupportedEmbed(url)
    fun isActualContentUrl(url: String?): Boolean = Companion.isActualContentUrl(url)

    /**
     * Resolves the active episode for playback:
     * - If initialEpisode is provided, uses it.
     * - If content is a series, retrieves the first episode from seasons.
     * - Otherwise returns null (for standalone movies).
     */
    fun resolveActiveEpisode(content: Content, initialEpisode: Episode?): Episode? {
        if (initialEpisode != null) {
            val matchedInContent = content.seasons.flatMap { it.episodes }.firstOrNull {
                it.id == initialEpisode.id || (it.seasonNumber == initialEpisode.seasonNumber && it.episodeNumber == initialEpisode.episodeNumber)
            }
            return matchedInContent ?: initialEpisode
        }
        if (content.type.equals("series", ignoreCase = true) || content.seasons.isNotEmpty()) {
            return content.seasons.firstOrNull()?.episodes?.firstOrNull()
                ?: content.seasons.flatMap { it.episodes }.firstOrNull()
        }
        return null
    }

    /**
     * Centralized, robust resolution logic that strictly differentiates between
     * the app intro video and actual content URLs from Firestore 'content' collection.
     */
    fun resolvePlayableUrl(content: Content, episode: Episode?): PlayableUrlResult {
        var introFiltered = false
        if (isAppIntroVideo(content.videoUrl) || (episode != null && isAppIntroVideo(episode.videoUrl))) {
            introFiltered = true
        }

        // 0. Check for Local Offline Downloaded File (persisted from DownloadManager)
        val offlineCandidate = episode?.videoUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: content.videoUrl.trim().takeIf { it.isNotEmpty() }

        if (offlineCandidate != null) {
            val isLocalFile = offlineCandidate.startsWith("file://") ||
                    offlineCandidate.startsWith("/data/") ||
                    offlineCandidate.startsWith("/storage/") ||
                    offlineCandidate.startsWith("/sdcard/") ||
                    java.io.File(offlineCandidate).exists()

            if (isLocalFile) {
                val fileUri = if (offlineCandidate.startsWith("file://")) {
                    offlineCandidate
                } else {
                    android.net.Uri.fromFile(java.io.File(offlineCandidate)).toString()
                }
                Log.d(TAG, "Reproduciendo archivo descargado offline: $fileUri")
                return PlayableUrlResult(
                    url = fileUri,
                    sourceType = PlaybackSourceType.LOCAL_OFFLINE,
                    isAppIntroFiltered = false,
                    originalUrl = offlineCandidate
                )
            }
        }

        // 1. Check Episode URL (for series)
        val rawEpUrl = episode?.videoUrl?.trim()
        if (!rawEpUrl.isNullOrEmpty()) {
            if (isAppIntroVideo(rawEpUrl)) {
                Log.w(TAG, "Episode '${episode.title}' has intro URL ($rawEpUrl). Filtering out intro.")
                introFiltered = true
            } else if (!isUnsupportedEmbed(rawEpUrl)) {
                val absoluteUrl = ensureAbsoluteCdnUrl(rawEpUrl)
                return PlayableUrlResult(
                    url = absoluteUrl,
                    sourceType = PlaybackSourceType.EPISODE_FIRESTORE,
                    isAppIntroFiltered = false,
                    originalUrl = rawEpUrl
                )
            }
        }

        // 2. Check Default Content Catalog fallback matching by Episode ID or Season & Episode Number
        val catalogMatch = findCatalogMatch(content)
        if (catalogMatch != null) {
            val catalogEpUrl = episode?.let { ep ->
                // Priority 1: Exact episode ID
                catalogMatch.seasons.flatMap { it.episodes }
                    .firstOrNull { it.id == ep.id }?.videoUrl
                // Priority 2: Exact seasonNumber and episodeNumber
                ?: catalogMatch.seasons.firstOrNull { it.seasonNumber == ep.seasonNumber }
                    ?.episodes?.firstOrNull { it.episodeNumber == ep.episodeNumber }?.videoUrl
                // Priority 3: Episode Title
                ?: catalogMatch.seasons.flatMap { it.episodes }
                    .firstOrNull { it.title.equals(ep.title, ignoreCase = true) }?.videoUrl
                // Priority 4: ONLY if catalog has 1 season, fallback to episodeNumber
                ?: (if (catalogMatch.seasons.size == 1) {
                    catalogMatch.seasons.firstOrNull()?.episodes?.firstOrNull { it.episodeNumber == ep.episodeNumber }?.videoUrl
                } else null)
            } ?: (if (content.type.equals("series", ignoreCase = true)) catalogMatch.seasons.firstOrNull()?.episodes?.firstOrNull()?.videoUrl else null)

            if (!catalogEpUrl.isNullOrBlank() && isActualContentUrl(catalogEpUrl)) {
                return PlayableUrlResult(
                    url = ensureAbsoluteCdnUrl(catalogEpUrl),
                    sourceType = PlaybackSourceType.CATALOG_FALLBACK,
                    isAppIntroFiltered = introFiltered,
                    originalUrl = catalogEpUrl
                )
            }

            val catalogMovieUrl = catalogMatch.videoUrl.trim()
            if (catalogMovieUrl.isNotBlank() && isActualContentUrl(catalogMovieUrl) && !content.type.equals("series", ignoreCase = true)) {
                return PlayableUrlResult(
                    url = ensureAbsoluteCdnUrl(catalogMovieUrl),
                    sourceType = PlaybackSourceType.CATALOG_FALLBACK,
                    isAppIntroFiltered = introFiltered,
                    originalUrl = catalogMovieUrl
                )
            }
        }

        // 3. If series playback was started without a specified episode, pick first episode
        if (episode == null && (content.type.equals("series", ignoreCase = true) || content.seasons.isNotEmpty())) {
            val firstEp = content.seasons.firstOrNull()?.episodes?.firstOrNull()
                ?: content.seasons.flatMap { it.episodes }.firstOrNull {
                    !it.videoUrl.isNullOrBlank() && isActualContentUrl(it.videoUrl)
                }
            if (firstEp != null && isActualContentUrl(firstEp.videoUrl)) {
                val absoluteUrl = ensureAbsoluteCdnUrl(firstEp.videoUrl)
                return PlayableUrlResult(
                    url = absoluteUrl,
                    sourceType = PlaybackSourceType.EPISODE_FIRESTORE,
                    isAppIntroFiltered = introFiltered,
                    originalUrl = firstEp.videoUrl
                )
            }
        }

        // 4. Check Content Root videoUrl (strictly only for standalone movies, never intro video)
        val rawContentUrl = content.videoUrl.trim()
        if (rawContentUrl.isNotEmpty() && !content.type.equals("series", ignoreCase = true)) {
            if (isAppIntroVideo(rawContentUrl)) {
                Log.w(TAG, "Content '${content.title}' has app intro URL ($rawContentUrl). Filtering out intro.")
                introFiltered = true
            } else if (!isUnsupportedEmbed(rawContentUrl)) {
                val absoluteUrl = ensureAbsoluteCdnUrl(rawContentUrl)
                return PlayableUrlResult(
                    url = absoluteUrl,
                    sourceType = PlaybackSourceType.MOVIE_FIRESTORE,
                    isAppIntroFiltered = introFiltered,
                    originalUrl = rawContentUrl
                )
            }
        }

        // 5. Guaranteed backup high-quality stream (Never the intro video!)
        val fallbackStream = getFallbackStream(content.id, episode?.id)
        Log.i(TAG, "Using backup stream for '${content.title}' (IntroFiltered=$introFiltered): $fallbackStream")
        return PlayableUrlResult(
            url = fallbackStream,
            sourceType = PlaybackSourceType.STREAM_BACKUP,
            isAppIntroFiltered = introFiltered,
            originalUrl = rawContentUrl
        )
    }
}
