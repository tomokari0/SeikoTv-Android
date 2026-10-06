package com.example.domain.repository

import com.example.domain.model.Content
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    fun getContentStream(limit: Long = 20L, startAfterDocument: DocumentSnapshot? = null): Flow<List<Content>>
    fun getFeaturedContent(): Flow<Content?>
    fun getContinueWatching(profileId: String): Flow<List<Content>>
    fun getRecommendations(): Flow<List<Content>>
    fun getSeries(): Flow<List<Content>>
    fun getMovies(): Flow<List<Content>>
    fun getContentById(id: String): Flow<Content?>
    suspend fun saveProgress(contentId: String, episodeId: String?, profileId: String, progressMs: Long, durationMs: Long)
    fun isFavorite(contentId: String, profileId: String): Flow<Boolean>
    suspend fun toggleFavorite(content: Content, profileId: String)
}
