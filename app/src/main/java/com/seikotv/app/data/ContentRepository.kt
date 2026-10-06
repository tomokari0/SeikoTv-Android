package com.seikotv.app.data

import com.google.firebase.firestore.DocumentSnapshot
import com.seikotv.app.domain.model.Content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ContentRepository {
    fun getContentStream(limit: Long = 20L, startAfterDocument: DocumentSnapshot? = null): Flow<List<Content>>
    suspend fun getPaginatedContent(limit: Long = 20L, startAfterDocument: DocumentSnapshot? = null): Result<PaginatedContentResult>
    fun getFeatured(): Flow<Content?>
    fun getSeries(): Flow<List<Content>>
    fun getMovies(): Flow<List<Content>>
    fun getContentById(id: String): Flow<Content?>
    fun searchContent(query: String): Flow<List<Content>>
}

class ContentRepositoryImpl(
    private val firestoreDataSource: FirestoreContentDataSource
) : ContentRepository {

    override fun getContentStream(limit: Long, startAfterDocument: DocumentSnapshot?): Flow<List<Content>> =
        firestoreDataSource.getContentStream(limit, startAfterDocument)

    override suspend fun getPaginatedContent(
        limit: Long,
        startAfterDocument: DocumentSnapshot?
    ): Result<PaginatedContentResult> =
        firestoreDataSource.getPaginatedContent(limit, startAfterDocument)

    override fun getFeatured(): Flow<Content?> =
        getContentStream().map { list ->
            list.firstOrNull { it.featured } ?: list.firstOrNull()
        }

    override fun getSeries(): Flow<List<Content>> =
        getContentStream().map { list ->
            list.filter { it.type.equals("series", ignoreCase = true) }
        }

    override fun getMovies(): Flow<List<Content>> =
        getContentStream().map { list ->
            list.filter { it.type.equals("movie", ignoreCase = true) }
        }

    override fun getContentById(id: String): Flow<Content?> =
        getContentStream().map { list ->
            list.firstOrNull { it.id == id }
        }

    override fun searchContent(query: String): Flow<List<Content>> =
        getContentStream().map { list ->
            if (query.isBlank()) emptyList() else {
                list.filter { item ->
                    item.title.contains(query, ignoreCase = true) ||
                            item.genre.any { g -> g.contains(query, ignoreCase = true) } ||
                            item.actors?.contains(query, ignoreCase = true) == true
                }
            }
        }
}
