package com.example.domain.model

data class Content(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val backdropUrl: String = "",
    val videoUrl: String = "",
    val type: String = "movie", // "movie" or "series"
    val genre: List<String> = emptyList(),
    val releaseYear: Int = 2024,
    val rating: String = "TV-PG",
    val status: String? = null, // "ongoing", "completed", "cancelled"
    val duration: String? = null,
    val featured: Boolean = false,
    val matchPercentage: Int = 95,
    val isLiked: Boolean = false,
    val isInMyList: Boolean = false,
    val progressMs: Long = 0L,
    val totalDurationMs: Long = 0L,

    // IMDb metadata
    val imdbId: String? = null,
    val imdbRating: String? = null,
    val imdbVotes: String? = null,
    val director: String? = null,
    val actors: String? = null,

    // Audio, subtitles & skip segments
    val serverType: String = "r2",
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList(),
    val skipSegments: SkipSegments? = null,
    val seasons: List<Season> = emptyList()
)

data class Episode(
    val id: String = "",
    val episodeNumber: Int = 1,
    val title: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val videoUrl: String = "",
    val duration: String = "",
    val durationMs: Long = 0L,
    val progressMs: Long = 0L,
    val seasonNumber: Int = 1,
    val skipSegments: SkipSegments? = null,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList()
)

data class Season(
    val id: String = "",
    val seasonNumber: Int = 1,
    val title: String = "",
    val episodes: List<Episode> = emptyList()
)

data class SkipSegments(
    val introStart: Double = 0.0,
    val introEnd: Double = 0.0
)

data class AudioTrack(
    val id: String = "",
    val label: String = "",
    val src: String = "",
    val language: String = "",
    val isDefault: Boolean = false
)

data class SubtitleTrack(
    val id: String = "",
    val label: String = "",
    val src: String = "",
    val language: String = "es"
)
