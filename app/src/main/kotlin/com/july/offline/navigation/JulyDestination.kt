package com.july.offline.navigation

/** Destinos de navegación de la app. */
sealed class JulyDestination(val route: String) {
    object Splash : JulyDestination("splash")
    object Conversation : JulyDestination("conversation")
    object Settings : JulyDestination("settings")
    object Emergency : JulyDestination("emergency")
    object Downloads : JulyDestination("downloads")
}
