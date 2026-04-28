package com.july.offline.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.july.offline.ui.conversation.ConversationScreen
import com.july.offline.ui.download.DownloadScreen
import com.july.offline.ui.emergency.EmergencyScreen
import com.july.offline.ui.settings.SettingsScreen
import com.july.offline.ui.splash.SplashScreen

@Composable
fun JulyNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = JulyDestination.Splash.route
    ) {
        composable(JulyDestination.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(JulyDestination.Conversation.route) {
                        popUpTo(JulyDestination.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(JulyDestination.Conversation.route) {
            ConversationScreen(
                onNavigateToSettings = {
                    navController.navigate(JulyDestination.Settings.route)
                },
                onNavigateToEmergency = {
                    navController.navigate(JulyDestination.Emergency.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(JulyDestination.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDownloads = {
                    navController.navigate(JulyDestination.Downloads.route)
                }
            )
        }
        composable(JulyDestination.Downloads.route) {
            DownloadScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(JulyDestination.Emergency.route) {
            EmergencyScreen(
                onExitEmergency = { navController.popBackStack() }
            )
        }
    }
}
