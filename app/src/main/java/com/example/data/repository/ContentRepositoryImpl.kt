package com.example.data.repository

import com.example.data.datasource.local.MyListDao
import com.example.data.datasource.local.MyListEntity
import com.example.data.datasource.local.WatchProgressDao
import com.example.data.datasource.local.WatchProgressEntity
import com.example.data.datasource.remote.FirestoreContentDataSource
import com.example.domain.model.Content
import com.example.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ContentRepositoryImpl(
    private val firestoreDataSource: FirestoreContentDataSource,
    private val watchProgressDao: WatchProgressDao,
    private val myListDao: MyListDao
) : ContentRepository {

    override fun getContentStream(
        limit: Long,
        startAfterDocument: com.google.firebase.firestore.DocumentSnapshot?
    ): Flow<List<Content>> =
        firestoreDataSource.getContentStream(limit, startAfterDocument)

    override fun getFeaturedContent(): Flow<Content?> =
        getContentStream().map { list ->
            list.firstOrNull { it.featured } ?: list.firstOrNull()
        }

    override fun getContinueWatching(profileId: String): Flow<List<Content>> {
        return combine(
            getContentStream(),
            watchProgressDao.getAllProgressForProfile(profileId)
        ) { allContent, progressList ->
            val progressMap = progressList.associateBy { it.contentId }
            allContent.filter { progressMap.containsKey(it.id) }
                .map { content ->
                    val p = progressMap[content.id]
                    content.copy(
                        progressMs = p?.progressMs ?: 0L,
                        totalDurationMs = p?.durationMs ?: 0L
                    )
                }
        }
    }

    override fun getRecommendations(): Flow<List<Content>> =
        getContentStream().map { list ->
            list.sortedByDescending { it.matchPercentage }
        }

    override fun getSeries(): Flow<List<Content>> =
        getContentStream().map { list ->
            list.filter { it.type == "series" }
        }

    override fun getMovies(): Flow<List<Content>> =
        getContentStream().map { list ->
            list.filter { it.type == "movie" }
        }

    override fun getContentById(id: String): Flow<Content?> =
        getContentStream().map { list ->
            list.firstOrNull { it.id == id }
        }

    override suspend fun saveProgress(
        contentId: String,
        episodeId: String?,
        profileId: String,
        progressMs: Long,
        durationMs: Long
    ) {
        val key = if (episodeId != null) "${contentId}_${episodeId}" else contentId
        val percentage = if (durationMs > 0) (progressMs.toFloat() / durationMs.toFloat()) else 0f
        watchProgressDao.saveProgress(
            WatchProgressEntity(
                id = key,
                contentId = contentId,
                episodeId = episodeId,
                profileId = profileId,
                progressMs = progressMs,
                durationMs = durationMs,
                percentage = percentage,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override fun isFavorite(contentId: String, profileId: String): Flow<Boolean> =
        myListDao.isInMyList(contentId, profileId)

    override suspend fun toggleFavorite(content: Content, profileId: String) {
        val currentFav = myListDao.isInMyList(content.id, profileId).first()
        if (currentFav) {
            myListDao.removeFromMyList(content.id, profileId)
        } else {
            myListDao.addToMyList(
                MyListEntity(
                    contentId = content.id,
                    profileId = profileId,
                    title = content.title,
                    thumbnailUrl = content.thumbnailUrl,
                    type = content.type
                )
            )
        }
    }
}
