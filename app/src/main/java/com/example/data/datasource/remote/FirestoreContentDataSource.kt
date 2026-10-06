package com.example.data.datasource.remote

import android.util.Log
import com.example.domain.model.AudioTrack
import com.example.domain.model.Content
import com.example.domain.model.Episode
import com.example.domain.model.Season
import com.example.domain.model.SkipSegments
import com.example.domain.model.SubtitleTrack
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.seikotv.app.data.config.FirebaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class PaginatedContentResult(
    val items: List<Content>,
    val lastDocument: DocumentSnapshot?,
    val hasMore: Boolean
)

class FirestoreContentDataSource {

    companion object {
        private const val TAG = "FirestoreDataSource"
        private const val R2_BASE = "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev"
        const val DEFAULT_PAGE_SIZE = 20L
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(com.example.SeikoApplication.instance).isNotEmpty()) {
                val db = FirebaseConfig.getFirestore()
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                db.firestoreSettings = settings
                val projectId = FirebaseApp.getInstance().options.projectId
                Log.d(TAG, "Firestore conectado con éxito al proyecto: $projectId")
                db
            } else {
                Log.w(TAG, "FirebaseApp no inicializado todavía.")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando Firestore: ${e.message}", e)
            null
        }
    }

    /**
     * Escucha en tiempo real la colección 'content' de Firestore
     * aplicando paginación mediante limit() y startAfter() para optimizar el feed principal.
     */
    fun getContentStream(
        limit: Long = DEFAULT_PAGE_SIZE,
        startAfterDocument: DocumentSnapshot? = null
    ): Flow<List<Content>> = callbackFlow {
        val db = firestore
        if (db == null) {
            Log.w(TAG, "Firestore no disponible, emitiendo catálogo por defecto.")
            trySend(DefaultContentCatalog.getAll().take(limit.toInt()))
            channel.close()
            return@callbackFlow
        }

        Log.d(TAG, "Iniciando listener paginado en 'content' (limit=$limit, startAfter=${startAfterDocument?.id})...")

        var query: Query = db.collection("content")
            .limit(limit)

        if (startAfterDocument != null) {
            query = query.startAfter(startAfterDocument)
        }

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error leyendo 'content': ${error.message}", error)
                    trySend(DefaultContentCatalog.getAll())
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    Log.d(TAG, "Documentos recibidos de Firestore: ${snapshot.size()}")
                    val parsedContentMap = mutableMapOf<String, Content>()

                    // 1. Parse base document fields
                    snapshot.documents.forEach { doc ->
                        try {
                            val data = doc.data ?: return@forEach
                            val content = parseContent(doc.id, data)
                            parsedContentMap[doc.id] = content
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parseando documento ${doc.id}: ${e.message}", e)
                        }
                    }

                    if (parsedContentMap.isNotEmpty()) {
                        // Emit base items immediately for quick UI render
                        trySend(parsedContentMap.values.toList())

                        // 2. Query subcollections 'episodes' for series in coroutine
                        launch(Dispatchers.IO) {
                            for (doc in snapshot.documents) {
                                val current = parsedContentMap[doc.id] ?: continue
                                if (current.type == "series" || current.seasons.isEmpty()) {
                                    try {
                                        val resolvedSeasons = fetchSeriesSeasonsAndEpisodes(doc, current)
                                        if (resolvedSeasons.isNotEmpty()) {
                                            val resolvedVideo = resolvedSeasons.flatMap { it.episodes }.firstOrNull()?.videoUrl?.takeIf {
                                                it.isNotBlank() && !isIntroUrl(it)
                                            } ?: current.videoUrl

                                            parsedContentMap[doc.id] = current.copy(
                                                seasons = resolvedSeasons,
                                                videoUrl = resolvedVideo
                                            )
                                        } else {
                                            enrichFromDefaultCatalogIfEmpty(current)?.let { enriched ->
                                                parsedContentMap[doc.id] = enriched
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Error cargando temporadas/episodios de '${current.title}': ${e.message}")
                                        enrichFromDefaultCatalogIfEmpty(current)?.let { enriched ->
                                            parsedContentMap[doc.id] = enriched
                                        }
                                    }
                                }
                            }
                            // Emit fully enriched content list with all episodes
                            trySend(parsedContentMap.values.toList())
                        }
                    } else {
                        trySend(DefaultContentCatalog.getAll())
                    }
                } else {
                    Log.d(TAG, "La colección 'content' está vacía en Firestore.")
                    trySend(DefaultContentCatalog.getAll())
                }
            }

        awaitClose {
            Log.d(TAG, "Cerrando listener de Firestore.")
            listenerRegistration.remove()
        }
    }

    /**
     * Consulta paginada one-shot utilizando limit() y startAfter()
     * optimizada para scroll infinito y carga incremental del feed principal.
     */
    suspend fun getPaginatedContent(
        limit: Long = DEFAULT_PAGE_SIZE,
        startAfterDocument: DocumentSnapshot? = null
    ): PaginatedContentResult {
        val db = firestore ?: return PaginatedContentResult(
            items = DefaultContentCatalog.getAll().take(limit.toInt()),
            lastDocument = null,
            hasMore = false
        )

        var query: Query = db.collection("content")
            .limit(limit)

        if (startAfterDocument != null) {
            query = query.startAfter(startAfterDocument)
        }

        return try {
            val snapshot = query.get().await()
            val parsedContentMap = mutableMapOf<String, Content>()

            snapshot.documents.forEach { doc ->
                val data = doc.data ?: return@forEach
                parsedContentMap[doc.id] = parseContent(doc.id, data)
            }

            for (doc in snapshot.documents) {
                val current = parsedContentMap[doc.id] ?: continue
                if (current.type == "series" || current.seasons.isEmpty()) {
                    try {
                        val resolvedSeasons = fetchSeriesSeasonsAndEpisodes(doc, current)
                        if (resolvedSeasons.isNotEmpty()) {
                            val resolvedVideo = resolvedSeasons.flatMap { it.episodes }.firstOrNull()?.videoUrl?.takeIf {
                                it.isNotBlank() && !isIntroUrl(it)
                            } ?: current.videoUrl

                            parsedContentMap[doc.id] = current.copy(
                                seasons = resolvedSeasons,
                                videoUrl = resolvedVideo
                            )
                        } else {
                            enrichFromDefaultCatalogIfEmpty(current)?.let { enriched ->
                                parsedContentMap[doc.id] = enriched
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error fetching episodes/seasons in page for '${current.title}': ${e.message}")
                    }
                }
            }

            val lastDoc = snapshot.documents.lastOrNull()
            val hasMore = snapshot.size().toLong() == limit

            PaginatedContentResult(
                items = parsedContentMap.values.toList(),
                lastDocument = lastDoc,
                hasMore = hasMore
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error en getPaginatedContent: ${e.message}", e)
            PaginatedContentResult(
                items = emptyList(),
                lastDocument = null,
                hasMore = false
            )
        }
    }

    private fun enrichFromDefaultCatalogIfEmpty(content: Content): Content? {
        if (content.type != "series" || content.seasons.isNotEmpty()) return null
        val defaultMatch = DefaultContentCatalog.getAll().firstOrNull {
            it.id.equals(content.id, ignoreCase = true) ||
                    it.title.equals(content.title, ignoreCase = true) ||
                    content.title.contains(it.title, ignoreCase = true) ||
                    it.title.contains(content.title, ignoreCase = true)
        }
        return if (defaultMatch != null && defaultMatch.seasons.isNotEmpty()) {
            content.copy(
                seasons = defaultMatch.seasons,
                videoUrl = defaultMatch.seasons.firstOrNull()?.episodes?.firstOrNull()?.videoUrl ?: content.videoUrl
            )
        } else null
    }

    private suspend fun fetchSeriesSeasonsAndEpisodes(
        doc: DocumentSnapshot,
        current: Content
    ): List<Season> {
        val defaultThumb = current.backdropUrl.ifEmpty { current.thumbnailUrl }

        // 1. Check subcollection 'seasons' in Firestore
        try {
            val seasonsSnapshot = doc.reference.collection("seasons").get().await()
            if (!seasonsSnapshot.isEmpty) {
                Log.d(TAG, "Cargadas ${seasonsSnapshot.size()} temporadas de subcolección 'seasons' para '${current.title}'")
                val seasonsList = mutableListOf<Season>()
                for ((sIndex, sDoc) in seasonsSnapshot.documents.withIndex()) {
                    val sData = sDoc.data ?: continue
                    val sNum = (sData["seasonNumber"] as? Number)?.toInt()
                        ?: (sData["season"] as? Number)?.toInt()
                        ?: extractSeasonNumber(sDoc.id)
                        ?: extractSeasonNumber(sData["title"] as? String ?: "")
                        ?: (sIndex + 1)
                    val sTitle = (sData["title"] as? String)
                        ?: (sData["name"] as? String)
                        ?: (if (sNum == 0) "Cortos" else "Temporada $sNum")

                    val epSubSnap = try {
                        sDoc.reference.collection("episodes").get().await()
                    } catch (_: Exception) { null }

                    val seasonEpisodes = if (epSubSnap != null && !epSubSnap.isEmpty) {
                        epSubSnap.documents.mapNotNull { epDoc ->
                            val epData = epDoc.data ?: return@mapNotNull null
                            parseSubcollectionEpisode(
                                docId = epDoc.id,
                                data = epData,
                                defaultThumb = defaultThumb,
                                defaultSeasonNumber = sNum
                            )
                        }.sortedWith(compareBy({ it.episodeNumber }, { it.title }))
                    } else {
                        val rawEps = (sData["episodes"] as? List<Map<String, Any>>)
                            ?: ((sData["episodes"] as? Map<String, Map<String, Any>>)?.values?.toList())
                            ?: emptyList()
                        rawEps.mapIndexed { eIndex, eMap ->
                            parseSingleEmbeddedEpisode(
                                defaultId = "${current.id}_s${sNum}_e${eIndex + 1}",
                                defaultIndex = eIndex + 1,
                                map = eMap,
                                defaultThumb = defaultThumb,
                                defaultVideo = current.videoUrl,
                                defaultSkip = current.skipSegments,
                                forceSeasonNumber = sNum
                            )
                        }
                    }

                    seasonsList.add(
                        Season(
                            id = sDoc.id,
                            seasonNumber = sNum,
                            title = if (sTitle.contains("EP", ignoreCase = true)) sTitle else "$sTitle (${seasonEpisodes.size} EP)",
                            episodes = seasonEpisodes
                        )
                    )
                }
                if (seasonsList.isNotEmpty()) {
                    return seasonsList.sortedBy { it.seasonNumber }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Subcolección 'seasons' no disponible para '${current.title}': ${e.message}")
        }

        // 2. Check subcollection 'episodes' in Firestore
        try {
            val epSnapshot = doc.reference.collection("episodes").get().await()
            if (!epSnapshot.isEmpty) {
                Log.d(TAG, "Cargados ${epSnapshot.size()} episodios de subcolección 'episodes' para '${current.title}'")
                val episodes = epSnapshot.documents.mapNotNull { epDoc ->
                    val epData = epDoc.data ?: return@mapNotNull null
                    parseSubcollectionEpisode(
                        docId = epDoc.id,
                        data = epData,
                        defaultThumb = defaultThumb
                    )
                }
                if (episodes.isNotEmpty()) {
                    val catalogMatch = DefaultContentCatalog.getAll().firstOrNull {
                        it.id.equals(current.id, ignoreCase = true) || it.title.equals(current.title, ignoreCase = true)
                    }
                    val effectiveExisting = if (current.seasons.size > 1) {
                        current.seasons
                    } else if (catalogMatch != null && catalogMatch.seasons.size > 1) {
                        catalogMatch.seasons
                    } else {
                        current.seasons
                    }
                    return groupEpisodesIntoSeasons(episodes, effectiveExisting)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Subcolección 'episodes' no disponible para '${current.title}': ${e.message}")
        }

        // 3. If current already has multiple seasons from root doc parsing, preserve them
        if (current.seasons.isNotEmpty()) {
            return current.seasons
        }

        // 4. Default Content Catalog fallback
        val catalogMatch = DefaultContentCatalog.getAll().firstOrNull {
            it.id.equals(current.id, ignoreCase = true) || it.title.equals(current.title, ignoreCase = true)
        }
        return catalogMatch?.seasons ?: emptyList()
    }

    private fun groupEpisodesIntoSeasons(
        episodes: List<Episode>,
        existingSeasons: List<Season> = emptyList()
    ): List<Season> {
        if (episodes.isEmpty()) return existingSeasons

        // If existing seasons has multiple seasons (e.g. Cortos, Temporada 1, Temporada 2):
        // Reconcile and enrich those seasons with the incoming episodes
        if (existingSeasons.size > 1) {
            val epMapById = episodes.associateBy { it.id }
            val epMapByNum = episodes.groupBy { it.seasonNumber to it.episodeNumber }
            val epMapByTitle = episodes.associateBy { it.title.trim().lowercase() }

            return existingSeasons.map { season ->
                val enrichedEps = season.episodes.map { sEp ->
                    val matched = epMapById[sEp.id]
                        ?: epMapByNum[season.seasonNumber to sEp.episodeNumber]?.firstOrNull()
                        ?: epMapByTitle[sEp.title.trim().lowercase()]
                    if (matched != null) {
                        sEp.copy(
                            videoUrl = if (matched.videoUrl.isNotBlank() && !isIntroUrl(matched.videoUrl)) matched.videoUrl else sEp.videoUrl,
                            thumbnailUrl = matched.thumbnailUrl.ifBlank { sEp.thumbnailUrl },
                            duration = matched.duration.ifBlank { sEp.duration }
                        )
                    } else sEp
                }
                season.copy(
                    title = if (season.title.contains("EP", ignoreCase = true)) season.title else "${season.title} (${enrichedEps.size} EP)",
                    episodes = enrichedEps
                )
            }
        }

        // If all episodes have the same seasonNumber (e.g. defaulted to 1), detect number resets or title cues
        val resolvedEpisodes = if (episodes.all { it.seasonNumber == episodes.first().seasonNumber }) {
            var currentSeason = episodes.first().seasonNumber
            var lastEpNum = -1
            episodes.map { ep ->
                val isCorto = ep.title.contains("corto", ignoreCase = true) || ep.id.contains("corto", ignoreCase = true) || ep.id.contains("_c", ignoreCase = true)
                if (isCorto) {
                    ep.copy(seasonNumber = 0)
                } else {
                    if (lastEpNum > 0 && ep.episodeNumber <= lastEpNum) {
                        currentSeason++
                    }
                    lastEpNum = ep.episodeNumber
                    ep.copy(seasonNumber = currentSeason)
                }
            }
        } else {
            episodes
        }

        val grouped = resolvedEpisodes.groupBy { it.seasonNumber }
        return grouped.entries.sortedBy { it.key }.map { (sNum, eps) ->
            val sortedEps = eps.sortedWith(compareBy({ it.episodeNumber }, { it.title }))
            val title = if (sNum == 0) {
                "Cortos (${sortedEps.size} EP)"
            } else {
                "Temporada $sNum (${sortedEps.size} EP)"
            }
            Season(
                id = "season_$sNum",
                seasonNumber = sNum,
                title = title,
                episodes = sortedEps
            )
        }
    }

    private fun extractSeasonNumber(text: String): Int? {
        val patterns = listOf(
            Regex("""(?i)(?:temporada|temp|season|s)\s*(\d+)"""),
            Regex("""(?i)^s(\d+)"""),
            Regex("""(?i)\b(\d+)[xX]\d+\b"""),
            Regex("""(?i)[_-]s(\d+)[_-]"""),
            Regex("""(?i)[_-]t(\d+)[_-]"""),
            Regex("""(?i)s(\d+)e\d+"""),
            Regex("""(?i)t(\d+)c\d+"""),
            Regex("""(?i)t(\d+)""")
        )
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1].toIntOrNull()
            }
        }
        if (text.contains("corto", ignoreCase = true) || text.contains("special", ignoreCase = true) || text.contains("especial", ignoreCase = true) || text.contains("_c", ignoreCase = true)) {
            return 0
        }
        return null
    }

    private fun parseSubcollectionEpisode(
        docId: String,
        data: Map<String, Any>,
        defaultThumb: String,
        defaultSeasonNumber: Int? = null
    ): Episode {
        val title = (data["title"] as? String)
            ?: (data["name"] as? String)
            ?: (data["titulo"] as? String)
            ?: "Episodio"

        val epNumber = (data["episodeNumber"] as? Number)?.toInt()
            ?: extractEpisodeNumber(title)
            ?: 1

        val rawSeasonNum = (data["seasonNumber"] as? Number)?.toInt()
            ?: (data["season"] as? Number)?.toInt()
            ?: (data["season_number"] as? Number)?.toInt()
            ?: (data["temporada"] as? Number)?.toInt()
            ?: (data["season"] as? String)?.toIntOrNull()
            ?: (data["temporada"] as? String)?.toIntOrNull()
            ?: defaultSeasonNumber
            ?: extractSeasonNumber(title)
            ?: extractSeasonNumber(docId)
            ?: 1

        val rawVideo = (data["videoUrl"] as? String)
            ?: (data["video"] as? String)
            ?: (data["url"] as? String)
            ?: (data["streamUrl"] as? String)
            ?: (data["stream"] as? String)
            ?: ""

        val videoUrl = ensureAbsoluteCdnUrl(rawVideo)

        val rawThumb = (data["thumbnailUrl"] as? String)
            ?: (data["thumbnail"] as? String)
            ?: (data["image"] as? String)
            ?: (data["poster"] as? String)
            ?: defaultThumb

        val thumbnailUrl = ensureAbsoluteCdnUrl(rawThumb)

        val duration = (data["duration"] as? String)
            ?: (data["duracion"] as? String)
            ?: "24m"

        val description = (data["description"] as? String)
            ?: (data["synopsis"] as? String)
            ?: ""

        val skipIntroSec = (data["skipIntro"] as? Number)?.toDouble() ?: 0.0
        val skipSegments = if (skipIntroSec > 0.0) {
            SkipSegments(introStart = 0.0, introEnd = skipIntroSec)
        } else {
            (data["skipSegments"] as? Map<String, Any>)?.let {
                SkipSegments(
                    introStart = (it["introStart"] as? Number)?.toDouble() ?: 0.0,
                    introEnd = (it["introEnd"] as? Number)?.toDouble() ?: 0.0
                )
            }
        }

        // Subtitles parsing
        val rawSubs = (data["subtitles"] as? List<Map<String, Any>>) ?: emptyList()
        val subtitles = rawSubs.map {
            SubtitleTrack(
                id = it["id"] as? String ?: "",
                label = it["label"] as? String ?: "",
                src = it["src"] as? String ?: "",
                language = it["language"] as? String ?: "es"
            )
        }

        return Episode(
            id = docId,
            episodeNumber = epNumber,
            title = title,
            description = description,
            thumbnailUrl = thumbnailUrl,
            videoUrl = videoUrl,
            duration = duration,
            seasonNumber = rawSeasonNum,
            skipSegments = skipSegments,
            subtitles = subtitles
        )
    }

    private fun extractEpisodeNumber(title: String): Int? {
        val patterns = listOf(
            Regex("""(?i)(?:cap[ií]tulo|cap|ep|episodio)\s*(\d+)"""),
            Regex("""(?i)ep\.?\s*(\d+)"""),
            Regex("""\b(\d+)\b""")
        )
        for (pattern in patterns) {
            val match = pattern.find(title)
            if (match != null) {
                return match.groupValues[1].toIntOrNull()
            }
        }
        return null
    }

    private fun ensureAbsoluteCdnUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("blob:")) {
            return trimmed
        }
        val cleanPath = if (trimmed.startsWith("/")) trimmed.substring(1) else trimmed
        return "$R2_BASE/$cleanPath"
    }

    private fun isIntroUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return true
        val lower = url.lowercase().trim()
        return lower.contains("/videos/intro") ||
                lower.contains("intro.mp4") ||
                lower.endsWith("intro.mp4") ||
                lower == "intro"
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseContent(id: String, data: Map<String, Any>): Content {
        val title = (data["title"] as? String)
            ?: (data["name"] as? String)
            ?: (data["titulo"] as? String)
            ?: id

        val description = (data["description"] as? String)
            ?: (data["synopsis"] as? String)
            ?: (data["sinopsis"] as? String)
            ?: (data["overview"] as? String)
            ?: ""

        val rawThumb = (data["thumbnailUrl"] as? String)
            ?: (data["poster"] as? String)
            ?: (data["posterUrl"] as? String)
            ?: (data["image"] as? String)
            ?: (data["thumbnail"] as? String)
            ?: (data["cover"] as? String)
            ?: ""
        val thumbnailUrl = ensureAbsoluteCdnUrl(rawThumb)

        val rawBackdrop = (data["backdropUrl"] as? String)
            ?: (data["backdrop"] as? String)
            ?: (data["banner"] as? String)
            ?: (data["fondo"] as? String)
            ?: rawThumb
        val backdropUrl = ensureAbsoluteCdnUrl(rawBackdrop)

        val rawVideo = (data["videoUrl"] as? String)
            ?: (data["video"] as? String)
            ?: (data["url"] as? String)
            ?: (data["streamUrl"] as? String)
            ?: (data["stream"] as? String)
            ?: ""
        var videoUrl = ensureAbsoluteCdnUrl(rawVideo)

        val typeRaw = (data["type"] as? String) ?: (data["tipo"] as? String) ?: "movie"
        val type = if (typeRaw.equals("serie", ignoreCase = true) || typeRaw.equals("series", ignoreCase = true)) {
            "series"
        } else {
            "movie"
        }

        val genreRaw = data["genre"] ?: data["genres"] ?: data["genero"] ?: data["generos"]
        val genre = when (genreRaw) {
            is List<*> -> genreRaw.mapNotNull { it?.toString() }
            is String -> genreRaw.split(",").map { it.trim() }
            else -> emptyList()
        }

        val releaseYear = (data["releaseYear"] as? Number)?.toInt()
            ?: (data["year"] as? Number)?.toInt()
            ?: (data["año"] as? Number)?.toInt()
            ?: 2024

        val rating = (data["rating"] as? String) ?: "TV-PG"
        val status = (data["status"] as? String) ?: (data["estado"] as? String)
        val duration = (data["duration"] as? String) ?: (data["duracion"] as? String)
        val featured = (data["featured"] as? Boolean) ?: (data["destacado"] as? Boolean) ?: false
        val matchPercentage = (data["matchPercentage"] as? Number)?.toInt() ?: 95

        val imdbId = data["imdbId"] as? String
        val imdbRating = (data["imdbRating"] as? String) ?: (data["imdb"] as? String)
        val imdbVotes = data["imdbVotes"] as? String
        val director = (data["director"] as? String) ?: (data["creators"] as? String)
        val actors = (data["actors"] as? String) ?: (data["cast"] as? String) ?: (data["reparto"] as? String)

        val skipIntroSec = (data["skipIntro"] as? Number)?.toDouble() ?: 0.0
        val skipMap = (data["skipSegments"] as? Map<String, Any>) ?: (data["skipIntro"] as? Map<String, Any>)
        val skipSegments = if (skipIntroSec > 0.0) {
            SkipSegments(introStart = 0.0, introEnd = skipIntroSec)
        } else {
            skipMap?.let {
                SkipSegments(
                    introStart = (it["introStart"] as? Number)?.toDouble() ?: 0.0,
                    introEnd = (it["introEnd"] as? Number)?.toDouble() ?: 0.0
                )
            }
        }

        val rawAudios = (data["audioTracks"] as? List<Map<String, Any>>) ?: emptyList()
        val audioTracks = rawAudios.map {
            AudioTrack(
                id = it["id"] as? String ?: "",
                label = it["label"] as? String ?: "",
                src = it["src"] as? String ?: "",
                language = it["language"] as? String ?: ""
            )
        }

        val rawSubs = (data["subtitles"] as? List<Map<String, Any>>) ?: emptyList()
        val subtitles = rawSubs.map {
            SubtitleTrack(
                id = it["id"] as? String ?: "",
                label = it["label"] as? String ?: "",
                src = it["src"] as? String ?: "",
                language = it["language"] as? String ?: "es"
            )
        }

        // Embedded seasons & episodes (e.g. if stored directly in root document as List or Map)
        val seasons = parseEmbeddedSeasonsAndEpisodes(id, data, thumbnailUrl, videoUrl, skipSegments)

        // If it's a movie and videoUrl is empty or points to Intro.mp4, check DefaultContentCatalog
        if (type == "movie" && (videoUrl.isEmpty() || isIntroUrl(videoUrl) || videoUrl.contains("streamtape.com/e/"))) {
            val catalogMatch = DefaultContentCatalog.getAll().firstOrNull {
                it.id.equals(id, ignoreCase = true) || it.title.equals(title, ignoreCase = true)
            }
            if (catalogMatch != null && catalogMatch.videoUrl.isNotBlank() && !isIntroUrl(catalogMatch.videoUrl)) {
                videoUrl = catalogMatch.videoUrl
            }
        }

        return Content(
            id = id,
            title = title,
            description = description,
            thumbnailUrl = thumbnailUrl,
            backdropUrl = backdropUrl,
            videoUrl = videoUrl,
            type = if (seasons.isNotEmpty()) "series" else type,
            genre = genre,
            releaseYear = releaseYear,
            rating = rating,
            status = status,
            duration = duration,
            featured = featured,
            matchPercentage = matchPercentage,
            imdbId = imdbId,
            imdbRating = imdbRating,
            imdbVotes = imdbVotes,
            director = director,
            actors = actors,
            audioTracks = audioTracks,
            subtitles = subtitles,
            skipSegments = skipSegments,
            seasons = seasons
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseEmbeddedSeasonsAndEpisodes(
        contentId: String,
        data: Map<String, Any>,
        defaultThumb: String,
        defaultVideo: String,
        defaultSkip: SkipSegments?
    ): List<Season> {
        // 1. Check 'seasons' as List
        val rawSeasonsList = data["seasons"] as? List<Map<String, Any>>
        if (!rawSeasonsList.isNullOrEmpty()) {
            return rawSeasonsList.mapIndexed { sIndex, sMap ->
                val sNumber = (sMap["seasonNumber"] as? Number)?.toInt() ?: (sIndex + 1)
                val sTitle = sMap["title"] as? String ?: "Temporada $sNumber"
                val rawEpisodes = (sMap["episodes"] as? List<Map<String, Any>>)
                    ?: ((sMap["episodes"] as? Map<String, Map<String, Any>>)?.values?.toList())
                    ?: emptyList()

                val episodes = rawEpisodes.mapIndexed { eIndex, eMap ->
                    parseSingleEmbeddedEpisode("${contentId}_s${sNumber}_e${eIndex + 1}", eIndex + 1, eMap, defaultThumb, defaultVideo, defaultSkip)
                }
                Season(
                    id = sMap["id"] as? String ?: "season_$sNumber",
                    seasonNumber = sNumber,
                    title = sTitle,
                    episodes = episodes
                )
            }
        }

        // 2. Check 'seasons' as Map
        val rawSeasonsMap = data["seasons"] as? Map<String, Map<String, Any>>
        if (!rawSeasonsMap.isNullOrEmpty()) {
            return rawSeasonsMap.entries.mapIndexed { sIndex, entry ->
                val sMap = entry.value
                val sNumber = (sMap["seasonNumber"] as? Number)?.toInt() ?: (sIndex + 1)
                val sTitle = sMap["title"] as? String ?: "Temporada $sNumber"
                val rawEpisodes = (sMap["episodes"] as? List<Map<String, Any>>)
                    ?: ((sMap["episodes"] as? Map<String, Map<String, Any>>)?.values?.toList())
                    ?: emptyList()

                val episodes = rawEpisodes.mapIndexed { eIndex, eMap ->
                    parseSingleEmbeddedEpisode("${contentId}_s${sNumber}_e${eIndex + 1}", eIndex + 1, eMap, defaultThumb, defaultVideo, defaultSkip)
                }
                Season(
                    id = sMap["id"] as? String ?: entry.key,
                    seasonNumber = sNumber,
                    title = sTitle,
                    episodes = episodes
                )
            }
        }

        // 3. Check root 'episodes' or 'capitulos' as List
        val rawRootEpisodes = (data["episodes"] as? List<Map<String, Any>>)
            ?: (data["capitulos"] as? List<Map<String, Any>>)
            ?: (data["episodios"] as? List<Map<String, Any>>)
        if (!rawRootEpisodes.isNullOrEmpty()) {
            val episodes = rawRootEpisodes.mapIndexed { eIndex, eMap ->
                parseSingleEmbeddedEpisode("${contentId}_ep${eIndex + 1}", eIndex + 1, eMap, defaultThumb, defaultVideo, defaultSkip)
            }
            return groupEpisodesIntoSeasons(episodes)
        }

        // 4. Check root 'episodes' as Map
        val rawRootEpisodesMap = (data["episodes"] as? Map<String, Map<String, Any>>)
            ?: (data["capitulos"] as? Map<String, Map<String, Any>>)
        if (!rawRootEpisodesMap.isNullOrEmpty()) {
            val episodes = rawRootEpisodesMap.entries.mapIndexed { eIndex, entry ->
                parseSingleEmbeddedEpisode("${contentId}_ep${eIndex + 1}", eIndex + 1, entry.value, defaultThumb, defaultVideo, defaultSkip)
            }
            return groupEpisodesIntoSeasons(episodes)
        }

        return emptyList()
    }

    private fun parseSingleEmbeddedEpisode(
        defaultId: String,
        defaultIndex: Int,
        map: Map<String, Any>,
        defaultThumb: String,
        defaultVideo: String,
        defaultSkip: SkipSegments?,
        forceSeasonNumber: Int? = null
    ): Episode {
        val epSkip = (map["skipSegments"] as? Map<String, Any>)?.let {
            SkipSegments(
                introStart = (it["introStart"] as? Number)?.toDouble() ?: 0.0,
                introEnd = (it["introEnd"] as? Number)?.toDouble() ?: 0.0
            )
        }

        val epTitle = (map["title"] as? String) ?: (map["name"] as? String) ?: "Capítulo $defaultIndex"
        val rawVideo = (map["videoUrl"] as? String)
            ?: (map["video"] as? String)
            ?: (map["url"] as? String)
            ?: (map["streamUrl"] as? String)
            ?: defaultVideo

        val epVideo = ensureAbsoluteCdnUrl(rawVideo)
        val rawThumb = (map["thumbnailUrl"] as? String)
            ?: (map["thumbnail"] as? String)
            ?: (map["image"] as? String)
            ?: defaultThumb
        val epThumb = ensureAbsoluteCdnUrl(rawThumb)
        val epDuration = (map["duration"] as? String) ?: "24m"

        val epSeason = forceSeasonNumber
            ?: (map["seasonNumber"] as? Number)?.toInt()
            ?: (map["season"] as? Number)?.toInt()
            ?: (map["temporada"] as? Number)?.toInt()
            ?: (map["season"] as? String)?.toIntOrNull()
            ?: (map["temporada"] as? String)?.toIntOrNull()
            ?: extractSeasonNumber(epTitle)
            ?: extractSeasonNumber(defaultId)
            ?: 1

        return Episode(
            id = map["id"] as? String ?: defaultId,
            episodeNumber = (map["episodeNumber"] as? Number)?.toInt() ?: defaultIndex,
            title = epTitle,
            description = (map["description"] as? String) ?: "",
            thumbnailUrl = epThumb,
            videoUrl = epVideo,
            duration = epDuration,
            seasonNumber = epSeason,
            skipSegments = epSkip ?: defaultSkip
        )
    }
}
