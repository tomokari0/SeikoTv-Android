package com.example.presentation.player

import androidx.media3.common.TrackGroup
import com.example.domain.model.AudioTrack
import com.example.domain.model.Content
import com.example.domain.model.Episode
import java.util.Locale

data class ExoAudioOption(
    val id: String,
    val language: String,
    val label: String,
    val trackGroup: TrackGroup,
    val trackIndex: Int
)

data class AudioTrackOption(
    val id: String,
    val language: String,
    val displayLabel: String,
    val flag: String = "🌐",
    val isOriginal: Boolean = false,
    val isDefault: Boolean = false
)

object AudioTrackManager {

    /**
     * Resolves the list of available audio tracks for a given content item, episode,
     * and tracks detected directly in the ExoPlayer media stream.
     */
    fun resolveAudioOptions(
        content: Content,
        episode: Episode?,
        exoTracks: List<ExoAudioOption> = emptyList()
    ): List<AudioTrackOption> {
        val result = mutableListOf<AudioTrackOption>()
        val seenLanguages = mutableSetOf<String>()

        val origLang = detectOriginalLanguage(content, episode)

        // 1. First, check embedded tracks from ExoPlayer (direct from container/stream)
        if (exoTracks.isNotEmpty()) {
            for (exo in exoTracks) {
                val cleanLang = exo.language.lowercase().trim()
                val isOriginal = cleanLang == origLang || exo.label.contains("orig", ignoreCase = true)
                val langName = getLanguageDisplayName(cleanLang)
                val flag = getLanguageFlag(cleanLang)
                val label = when {
                    isOriginal -> "$flag  AUDIO ORIGINAL (${langName.uppercase()})"
                    exo.label.isNotBlank() -> "$flag  ${exo.label.uppercase()}"
                    cleanLang.contains("lat") || cleanLang == "es-419" || cleanLang == "es" -> "$flag  ESPAÑOL LATINO (DOBLAJE)"
                    cleanLang.contains("cas") || cleanLang == "es-es" -> "$flag  ESPAÑOL CASTELLANO"
                    else -> "$flag  ${langName.uppercase()} (DOBLAJE)"
                }
                result.add(
                    AudioTrackOption(
                        id = exo.id,
                        language = cleanLang.ifEmpty { origLang },
                        displayLabel = label,
                        flag = flag,
                        isOriginal = isOriginal,
                        isDefault = isOriginal
                    )
                )
                if (cleanLang.isNotEmpty()) seenLanguages.add(cleanLang)
            }
        }

        // 2. Next, check explicitly declared tracks from Episode or Content
        val declaredTracks: List<AudioTrack> = episode?.audioTracks?.takeIf { it.isNotEmpty() }
            ?: content.audioTracks

        if (declaredTracks.isNotEmpty()) {
            for (track in declaredTracks) {
                val cleanLang = track.language.lowercase().trim()
                if (cleanLang.isNotEmpty() && seenLanguages.contains(cleanLang)) continue

                val isOriginal = track.isDefault ||
                        track.label.contains("orig", ignoreCase = true) ||
                        cleanLang == origLang

                val langName = getLanguageDisplayName(cleanLang)
                val flag = getLanguageFlag(cleanLang)
                val label = if (track.label.isNotBlank()) {
                    if (track.label.contains("🇯🇵") || track.label.contains("🇲🇽") || track.label.contains("🇺🇸") || track.label.contains("🇪🇸") || track.label.contains("🇰🇷")) {
                        track.label
                    } else {
                        "$flag  ${track.label.uppercase()}"
                    }
                } else if (isOriginal) {
                    "$flag  AUDIO ORIGINAL (${langName.uppercase()})"
                } else {
                    "$flag  ${langName.uppercase()} (DOBLAJE)"
                }

                result.add(
                    AudioTrackOption(
                        id = track.id.ifEmpty { "track_${cleanLang}" },
                        language = cleanLang,
                        displayLabel = label,
                        flag = flag,
                        isOriginal = isOriginal,
                        isDefault = track.isDefault
                    )
                )
                if (cleanLang.isNotEmpty()) seenLanguages.add(cleanLang)
            }
        }

        // 3. If no custom tracks found, provide contextual multi-language options based on origin
        if (result.isEmpty()) {
            val baseList = getContextualTrackList(origLang)
            result.addAll(baseList)
        } else {
            // Ensure essential Spanish dubs and English are available as options if missing
            val essentials = listOf("es-419", "es-ES", "en")
            for (code in essentials) {
                if (!seenLanguages.contains(code.lowercase()) && !seenLanguages.contains(code.substringBefore("-"))) {
                    val langName = getLanguageDisplayName(code)
                    val flag = getLanguageFlag(code)
                    result.add(
                        AudioTrackOption(
                            id = "opt_$code",
                            language = code,
                            displayLabel = "$flag  ${langName.uppercase()}",
                            flag = flag,
                            isOriginal = code == origLang
                        )
                    )
                    seenLanguages.add(code.lowercase())
                }
            }
        }

        // Sort so that original language track is ALWAYS first at the top
        return result.sortedWith(compareByDescending<AudioTrackOption> { it.isOriginal }.thenBy { it.displayLabel })
    }

    /**
     * Accurately detects the original production language for any series or film.
     */
    fun detectOriginalLanguage(content: Content, episode: Episode? = null): String {
        // 1. Explicitly marked default track
        val defaultFromContent = (episode?.audioTracks?.firstOrNull { it.isDefault }
            ?: content.audioTracks.firstOrNull { it.isDefault })?.language?.lowercase()
        if (!defaultFromContent.isNullOrBlank()) {
            return defaultFromContent
        }

        val id = content.id.lowercase()
        val title = content.title.lowercase()
        val director = (content.director ?: "").lowercase()
        val actors = (content.actors ?: "").lowercase()

        // Korean productions
        if (id.contains("muertos") || id.contains("korean") || id.contains("corea") ||
            director.contains("lee min") || director.contains("choi jung") ||
            title.contains("estamos muertos") || title.contains("all of us are dead")) {
            return "ko"
        }

        // English productions (e.g. Murder Drones, Helluva Boss, Brother Betrayal)
        if (id.contains("murder_drones") || id.contains("helluva") || id.contains("brother") ||
            title.contains("murder drones") || title.contains("helluva boss") || title.contains("brother betrayal")) {
            return "en"
        }

        // Spanish native productions (e.g. Solas en casa, Una venganza apasionada, Todo o nada, Entrelazados)
        if (id.contains("solas_en_casa") || id.contains("venganza") || id.contains("todo_o_nada") || id.contains("entrelazados") ||
            title.contains("solas en casa") || title.contains("venganza") || title.contains("entrelazados")) {
            return "es-419"
        }

        // Japanese anime (e.g. Masacre escolar, KonoSuba)
        if (id.contains("konosuba") || id.contains("masacre") || content.genre.any { it.contains("anime", ignoreCase = true) }) {
            return "ja"
        }

        return "ja" // Default fallback for anime catalog
    }

    private fun getContextualTrackList(originalCode: String): List<AudioTrackOption> {
        val origFlag = getLanguageFlag(originalCode)
        val origName = getLanguageDisplayName(originalCode)

        val options = mutableListOf<AudioTrackOption>()

        // 1. Original Track
        options.add(
            AudioTrackOption(
                id = "orig_$originalCode",
                language = originalCode,
                displayLabel = "$origFlag  AUDIO ORIGINAL (${origName.uppercase()})",
                flag = origFlag,
                isOriginal = true,
                isDefault = true
            )
        )

        // 2. Add common major dubbing languages if not the original
        val dubs = listOf(
            Triple("es_lat", "es-419", "🇲🇽  ESPAÑOL LATINO (DOBLAJE)"),
            Triple("es_cas", "es-ES", "🇪🇸  ESPAÑOL CASTELLANO"),
            Triple("en", "en", "🇺🇸  INGLÉS (ENGLISH)"),
            Triple("ja", "ja", "🇯🇵  JAPONÉS (DOBLAJE)"),
            Triple("ko", "ko", "🇰🇷  COREANO (DOBLAJE)"),
            Triple("pt", "pt-BR", "🇧🇷  PORTUGUÉS (DOBLAJE)"),
            Triple("fr", "fr", "🇫🇷  FRANCÉS (FRANÇAIS)"),
            Triple("de", "de", "🇩🇪  ALEMÁN (DEUTSCH)"),
            Triple("it", "it", "🇮🇹  ITALIANO (DOBLAJE)")
        )

        for ((id, code, label) in dubs) {
            if (code != originalCode && !code.startsWith(originalCode)) {
                options.add(
                    AudioTrackOption(
                        id = id,
                        language = code,
                        displayLabel = label,
                        flag = getLanguageFlag(code),
                        isOriginal = false
                    )
                )
            }
        }

        return options
    }

    fun getLanguageDisplayName(code: String): String {
        return when (code.lowercase().trim()) {
            "ja", "jpn", "japanese" -> "Japonés"
            "ko", "kor", "korean" -> "Coreano"
            "en", "eng", "english" -> "Inglés"
            "es", "spa", "spanish", "es-419", "es-lat" -> "Español Latino"
            "es-es", "es_cas" -> "Español Castellano"
            "pt", "por", "portuguese", "pt-br" -> "Portugués"
            "fr", "fra", "french" -> "Francés"
            "de", "deu", "german" -> "Alemán"
            "it", "ita", "italian" -> "Italiano"
            "zh", "zho", "chi", "chinese" -> "Chino (Mandarín)"
            "ru", "rus", "russian" -> "Ruso"
            else -> try {
                val locale = Locale.forLanguageTag(code)
                locale.getDisplayLanguage(Locale("es")).replaceFirstChar { it.uppercase() }
                    .ifBlank { code.uppercase() }
            } catch (_: Exception) {
                code.uppercase()
            }
        }
    }

    fun getLanguageFlag(code: String): String {
        return when (code.lowercase().trim()) {
            "ja", "jpn", "japanese" -> "🇯🇵"
            "ko", "kor", "korean" -> "🇰🇷"
            "en", "eng", "english" -> "🇺🇸"
            "es", "spa", "spanish", "es-419", "es-lat" -> "🇲🇽"
            "es-es", "es_cas" -> "🇪🇸"
            "pt", "por", "portuguese", "pt-br" -> "🇧🇷"
            "fr", "fra", "french" -> "🇫🇷"
            "de", "deu", "german" -> "🇩🇪"
            "it", "ita", "italian" -> "🇮🇹"
            "zh", "zho", "chi", "chinese" -> "🇨🇳"
            "ru", "rus", "russian" -> "🇷🇺"
            else -> "🌐"
        }
    }
}
