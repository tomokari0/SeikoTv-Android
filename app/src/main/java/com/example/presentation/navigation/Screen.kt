package com.example.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Profiles : Screen("profiles")
    data object Home : Screen("home")
    data object Series : Screen("series")
    data object Movies : Screen("movies")
    data object Downloads : Screen("downloads")
    data object Player : Screen("player/{contentId}?episodeId={episodeId}") {
        fun createRoute(contentId: String, episodeId: String? = null): String {
            return if (episodeId != null) {
                "player/$contentId?episodeId=$episodeId"
            } else {
                "player/$contentId"
            }
        }
    }
}
