package com.seikotv.app.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.seikotv.app.data.config.FirebaseConfig
import com.seikotv.app.data.config.FirebaseConnectionInterceptor
import com.seikotv.app.domain.model.AudioTrack
import com.seikotv.app.domain.model.Content
import com.seikotv.app.domain.model.Episode
import com.seikotv.app.domain.model.Season
import com.seikotv.app.domain.model.SkipSegments
import com.seikotv.app.domain.model.SubtitleTrack
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

class FirestoreContentDataSource(
    private val context: Context? = null
) {

    companion object {
        private const val TAG = "FirestoreContentDS"
        private const val COLLECTION_CONTENT = "content"
        const val DEFAULT_PAGE_SIZE = 20L
    }

    private val interceptor: FirebaseConnectionInterceptor? by lazy {
        context?.let { FirebaseConnectionInterceptor(it) }
    }

    private fun getFirestore(): FirebaseFirestore {
        return FirebaseConfig.getFirestore()
    }

    /**
     * Escucha en tiempo real la colección 'content' de Firestore
     * aplicando paginación mediante limit() y startAfter() para optimizar el feed.
     */
    fun getContentStream(
        limit: Long = DEFAULT_PAGE_SIZE,
        startAfterDocument: DocumentSnapshot? = null
    ): Flow<List<Content>> = callbackFlow {
        val firestore = getFirestore()
        Log.d(TAG, "Iniciando listener paginado en '$COLLECTION_CONTENT' (limit=$limit, startAfter=${startAfterDocument?.id})")

        var query: Query = firestore.collection(COLLECTION_CONTENT)
            .limit(limit)

        if (startAfterDocument != null) {
            query = query.startAfter(startAfterDocument)
        }

        val registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando colección '$COLLECTION_CONTENT': ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    Log.d(TAG, "Recibidos ${snapshot.size()} documentos desde Firestore")
                    val contentMap = mutableMapOf<String, Content>()
                    snapshot.documents.forEach { doc ->
                        try {
                            val data = doc.data ?: return@forEach
                            contentMap[doc.id] = mapDocumentToContent(doc.id, data)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parseando documento '${doc.id}': ${e.message}", e)
                        }
                    }
                    trySend(contentMap.values.toList())

                    // Query subcollections 'episodes' for series
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        for (doc in snapshot.documents) {
                            val current = contentMap[doc.id] ?: continue
                            if (current.type == "series" || current.seasons.isEmpty()) {
                                try {
                                    val epSnapshot = doc.reference.collection("episodes").get().await()
                                    if (!epSnapshot.isEmpty) {
                                        val episodes = epSnapshot.documents.mapNotNull { epDoc ->
                                            val epData = epDoc.data ?: return@mapNotNull null
                                            val epTitle = (epData["title"] as? String) ?: (epData["name"] as? String) ?: "Episodio"
                                            val epNum = (epData["episodeNumber"] as? Number)?.toInt() ?: 1
                                            val rawVideo = (epData["videoUrl"] as? String) ?: (epData["video"] as? String) ?: (epData["streamUrl"] as? String) ?: ""
                                            val epVideo = if (rawVideo.isNotBlank() && !rawVideo.startsWith("http://") && !rawVideo.startsWith("https://")) {
                                                "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/${if (rawVideo.startsWith("/")) rawVideo.substring(1) else rawVideo}"
                                            } else rawVideo
                                            val rawThumb = (epData["thumbnailUrl"] as? String) ?: (epData["image"] as? String) ?: current.thumbnailUrl
                                            val epThumb = if (rawThumb.isNotBlank() && !rawThumb.startsWith("http://") && !rawThumb.startsWith("https://")) {
                                                "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/${if (rawThumb.startsWith("/")) rawThumb.substring(1) else rawThumb}"
                                            } else rawThumb
                                            val epDuration = (epData["duration"] as? String) ?: "24m"
                                            val skipSec = (epData["skipIntro"] as? Number)?.toDouble() ?: 0.0
                                            val skip = if (skipSec > 0.0) SkipSegments(0.0, skipSec) else null
                                            val sNum = (epData["seasonNumber"] as? Number)?.toInt()
                                                ?: (epData["season"] as? Number)?.toInt()
                                                ?: (epData["temporada"] as? Number)?.toInt()
                                                ?: 1
                                            Episode(
                                                id = epDoc.id,
                                                episodeNumber = epNum,
                                                title = epTitle,
                                                description = (epData["description"] as? String) ?: "",
                                                thumbnailUrl = epThumb,
                                                videoUrl = epVideo,
                                                duration = epDuration,
                                                seasonNumber = sNum,
                                                skipSegments = skip
                                            )
                                        }.sortedBy { it.episodeNumber }

                                        if (episodes.isNotEmpty()) {
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
                                            val resolvedSeasons = if (grouped.size == 1 && current.seasons.size > 1) {
                                                current.seasons
                                            } else {
                                                grouped.entries.sortedBy { it.key }.map { (seasonNum, eps) ->
                                                    Season(
                                                        id = "season_$seasonNum",
                                                        seasonNumber = seasonNum,
                                                        title = if (seasonNum == 0) "Cortos (${eps.size} EP)" else "Temporada $seasonNum (${eps.size} EP)",
                                                        episodes = eps.sortedBy { it.episodeNumber }
                                                    )
                                                }
                                            }
                                            contentMap[doc.id] = current.copy(
                                                seasons = resolvedSeasons,
                                                videoUrl = episodes.firstOrNull()?.videoUrl ?: current.videoUrl
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Error fetching episodes subcollection for ${current.title}: ${e.message}")
                                }
                            }
                        }
                        trySend(contentMap.values.toList())
                    }
                }
            }

        awaitClose {
            Log.d(TAG, "Cerrando snapshot listener de '$COLLECTION_CONTENT'")
            registration.remove()
        }
    }

    /**
     * Consulta paginada one-shot utilizando limit() y startAfter()
     * optimizada para scroll infinito y carga por lotes del feed principal.
     */
    suspend fun getPaginatedContent(
        limit: Long = DEFAULT_PAGE_SIZE,
        startAfterDocument: DocumentSnapshot? = null
    ): Result<PaginatedContentResult> {
        val safeInterceptor = interceptor
        val block: suspend (FirebaseFirestore) -> PaginatedContentResult = { firestore ->
            var query: Query = firestore.collection(COLLECTION_CONTENT)
                .limit(limit)

            if (startAfterDocument != null) {
                query = query.startAfter(startAfterDocument)
            }

            val snapshot = query.get().await()
            val contentMap = mutableMapOf<String, Content>()

            snapshot.documents.forEach { doc ->
                val data = doc.data ?: return@forEach
                contentMap[doc.id] = mapDocumentToContent(doc.id, data)
            }

            // Cargar episodios de subcolecciones para las series de esta página
            for (doc in snapshot.documents) {
                val current = contentMap[doc.id] ?: continue
                if (current.type == "series" || current.seasons.isEmpty()) {
                    try {
                        val epSnapshot = doc.reference.collection("episodes").get().await()
                        if (!epSnapshot.isEmpty) {
                            val episodes = epSnapshot.documents.mapNotNull { epDoc ->
                                val epData = epDoc.data ?: return@mapNotNull null
                                val epTitle = (epData["title"] as? String) ?: (epData["name"] as? String) ?: "Episodio"
                                val epNum = (epData["episodeNumber"] as? Number)?.toInt() ?: 1
                                val rawVideo = (epData["videoUrl"] as? String) ?: (epData["video"] as? String) ?: (epData["streamUrl"] as? String) ?: ""
                                val epVideo = if (rawVideo.isNotBlank() && !rawVideo.startsWith("http://") && !rawVideo.startsWith("https://")) {
                                    "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/${if (rawVideo.startsWith("/")) rawVideo.substring(1) else rawVideo}"
                                } else rawVideo
                                val rawThumb = (epData["thumbnailUrl"] as? String) ?: (epData["image"] as? String) ?: current.thumbnailUrl
                                val epThumb = if (rawThumb.isNotBlank() && !rawThumb.startsWith("http://") && !rawThumb.startsWith("https://")) {
                                    "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/${if (rawThumb.startsWith("/")) rawThumb.substring(1) else rawThumb}"
                                } else rawThumb
                                val epDuration = (epData["duration"] as? String) ?: "24m"
                                val skipSec = (epData["skipIntro"] as? Number)?.toDouble() ?: 0.0
                                val skip = if (skipSec > 0.0) SkipSegments(0.0, skipSec) else null
                                val sNum = (epData["seasonNumber"] as? Number)?.toInt()
                                    ?: (epData["season"] as? Number)?.toInt()
                                    ?: (epData["temporada"] as? Number)?.toInt()
                                    ?: 1
                                Episode(
                                    id = epDoc.id,
                                    episodeNumber = epNum,
                                    title = epTitle,
                                    description = (epData["description"] as? String) ?: "",
                                    thumbnailUrl = epThumb,
                                    videoUrl = epVideo,
                                    duration = epDuration,
                                    seasonNumber = sNum,
                                    skipSegments = skip
                                )
                            }.sortedBy { it.episodeNumber }

                            if (episodes.isNotEmpty()) {
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
                                val resolvedSeasons = if (grouped.size == 1 && current.seasons.size > 1) {
                                    current.seasons
                                } else {
                                    grouped.entries.sortedBy { it.key }.map { (seasonNum, eps) ->
                                        Season(
                                            id = "season_$seasonNum",
                                            seasonNumber = seasonNum,
                                            title = if (seasonNum == 0) "Cortos (${eps.size} EP)" else "Temporada $seasonNum (${eps.size} EP)",
                                            episodes = eps.sortedBy { it.episodeNumber }
                                        )
                                    }
                                }
                                contentMap[doc.id] = current.copy(
                                    seasons = resolvedSeasons,
                                    videoUrl = episodes.firstOrNull()?.videoUrl ?: current.videoUrl
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error consultando episodios en página: ${e.message}")
                    }
                }
            }

            val lastDoc = snapshot.documents.lastOrNull()
            val hasMore = snapshot.size().toLong() == limit

            PaginatedContentResult(
                items = contentMap.values.toList(),
                lastDocument = lastDoc,
                hasMore = hasMore
            )
        }

        return if (safeInterceptor != null) {
            safeInterceptor.intercept("GetPaginatedContent") { firestore ->
                block(firestore)
            }
        } else {
            try {
                Result.success(block(getFirestore()))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Consulta puntual de un documento interceptada para verificar
     * inicialización, conexión y estado del motor Firebase.
     */
    suspend fun getDocumentById(id: String): Result<Content?> {
        val safeInterceptor = interceptor
        return if (safeInterceptor != null) {
            safeInterceptor.intercept("GetDocumentById_$id") { firestore ->
                val snapshot = firestore.collection(COLLECTION_CONTENT).document(id).get().await()
                val data = snapshot.data
                if (data != null) mapDocumentToContent(snapshot.id, data) else null
            }
        } else {
            try {
                val snapshot = getFirestore().collection(COLLECTION_CONTENT).document(id).get().await()
                val data = snapshot.data
                Result.success(if (data != null) mapDocumentToContent(snapshot.id, data) else null)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun mapDocumentToContent(id: String, data: Map<String, Any>): Content {
        val title = (data["title"] as? String)
            ?: (data["name"] as? String)
            ?: (data["titulo"] as? String)
            ?: id

        val description = (data["description"] as? String)
            ?: (data["synopsis"] as? String)
            ?: (data["sinopsis"] as? String)
            ?: (data["overview"] as? String)
            ?: ""

        val thumbnailUrl = (data["thumbnailUrl"] as? String)
            ?: (data["poster"] as? String)
            ?: (data["posterUrl"] as? String)
            ?: (data["image"] as? String)
            ?: (data["thumbnail"] as? String)
            ?: (data["cover"] as? String)
            ?: ""

        val backdropUrl = (data["backdropUrl"] as? String)
            ?: (data["backdrop"] as? String)
            ?: (data["banner"] as? String)
            ?: (data["fondo"] as? String)
            ?: thumbnailUrl

        val videoUrl = (data["videoUrl"] as? String)
            ?: (data["video"] as? String)
            ?: (data["url"] as? String)
            ?: (data["streamUrl"] as? String)
            ?: ""

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

        val serverType = data["serverType"] as? String ?: "r2"

        val rawAudios = (data["audioTracks"] as? List<Map<String, Any>>) ?: emptyList()
        val audioTracks = rawAudios.map { audioMap ->
            AudioTrack(
                id = audioMap["id"] as? String ?: "",
                label = audioMap["label"] as? String ?: "",
                src = audioMap["src"] as? String ?: "",
                language = audioMap["language"] as? String ?: "",
                isDefault = audioMap["isDefault"] as? Boolean ?: false
            )
        }

        val rawSubtitles = (data["subtitles"] as? List<Map<String, Any>>) ?: emptyList()
        val subtitles = rawSubtitles.map { subMap ->
            SubtitleTrack(
                id = subMap["id"] as? String ?: "",
                label = subMap["label"] as? String ?: "",
                src = subMap["src"] as? String ?: "",
                language = subMap["language"] as? String ?: "es"
            )
        }

        val skipMap = (data["skipSegments"] as? Map<String, Any>) ?: (data["skipIntro"] as? Map<String, Any>)
        val skipSegments = skipMap?.let {
            SkipSegments(
                introStart = (it["introStart"] as? Number)?.toDouble() ?: 0.0,
                introEnd = (it["introEnd"] as? Number)?.toDouble() ?: 0.0
            )
        }

        val rawSeasons = data["seasons"] as? List<Map<String, Any>>
        val seasons: List<Season> = if (!rawSeasons.isNullOrEmpty()) {
            rawSeasons.mapIndexed { sIndex, sMap ->
                val sNum = (sMap["seasonNumber"] as? Number)?.toInt() ?: (sIndex + 1)
                val sTitle = sMap["title"] as? String ?: "Temporada $sNum"
                val rawEpisodes = (sMap["episodes"] as? List<Map<String, Any>>) ?: emptyList()
                val episodes = rawEpisodes.mapIndexed { eIndex, eMap ->
                    val epSkip = (eMap["skipSegments"] as? Map<String, Any>)?.let {
                        SkipSegments(
                            introStart = (it["introStart"] as? Number)?.toDouble() ?: 0.0,
                            introEnd = (it["introEnd"] as? Number)?.toDouble() ?: 0.0
                        )
                    }
                    Episode(
                        id = eMap["id"] as? String ?: "${id}_s${sNum}_e${eIndex + 1}",
                        episodeNumber = (eMap["episodeNumber"] as? Number)?.toInt() ?: (eIndex + 1),
                        title = (eMap["title"] as? String) ?: "Capítulo ${eIndex + 1}",
                        description = eMap["description"] as? String ?: "",
                        thumbnailUrl = (eMap["thumbnailUrl"] as? String) ?: thumbnailUrl,
                        videoUrl = (eMap["videoUrl"] as? String) ?: videoUrl,
                        duration = (eMap["duration"] as? String) ?: "24m",
                        skipSegments = epSkip ?: skipSegments
                    )
                }
                Season(
                    id = sMap["id"] as? String ?: "season_$sNum",
                    seasonNumber = sNum,
                    title = sTitle,
                    episodes = episodes
                )
            }
        } else {
            val rootEpisodes = data["episodes"] as? List<Map<String, Any>>
            if (!rootEpisodes.isNullOrEmpty()) {
                val eps = rootEpisodes.mapIndexed { eIndex, eMap ->
                    Episode(
                        id = eMap["id"] as? String ?: "${id}_ep${eIndex + 1}",
                        episodeNumber = (eMap["episodeNumber"] as? Number)?.toInt() ?: (eIndex + 1),
                        title = (eMap["title"] as? String) ?: "Capítulo ${eIndex + 1}",
                        description = eMap["description"] as? String ?: "",
                        thumbnailUrl = (eMap["thumbnailUrl"] as? String) ?: thumbnailUrl,
                        videoUrl = (eMap["videoUrl"] as? String) ?: videoUrl,
                        duration = (eMap["duration"] as? String) ?: "24m",
                        skipSegments = skipSegments
                    )
                }
                listOf(Season(id = "season_1", seasonNumber = 1, title = "Temporada 1", episodes = eps))
            } else {
                emptyList()
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
            serverType = serverType,
            audioTracks = audioTracks,
            subtitles = subtitles,
            skipSegments = skipSegments,
            seasons = seasons
        )
    }
}
