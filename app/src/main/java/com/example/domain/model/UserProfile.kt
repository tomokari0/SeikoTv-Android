package com.example.domain.model

data class UserProfile(
    val id: String = "profile_1",
    val name: String = "Seiko",
    val avatarUrl: String = "",
    val avatarColorHex: Long = 0xFFE50914,
    val isKids: Boolean = false
)
