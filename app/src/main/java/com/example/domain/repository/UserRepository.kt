package com.example.domain.repository

import com.example.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getProfiles(): Flow<List<UserProfile>>
    fun getActiveProfile(): Flow<UserProfile>
    suspend fun selectProfile(profile: UserProfile)
    suspend fun createProfile(name: String, colorHex: Long, isKids: Boolean)
}
