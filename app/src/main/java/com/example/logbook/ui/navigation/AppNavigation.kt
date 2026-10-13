package com.example.logbook.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.logbook.ui.main.MainScreen
import com.example.logbook.ui.profile.ProfileListScreen
import com.example.logbook.ui.settings.SettingsScreen
import com.example.logbook.ui.tabs.resources.aggregates.AggregatesScreen
import com.example.logbook.ui.tabs.resources.aggregatesOnBoard.AggregatesOnBoardScreen
import com.example.logbook.ui.tabs.resources.oils.OilsScreen
import kotlinx.serialization.Serializable

@Serializable
object ProfileListRoute

@Serializable
object SettingsRoute

@Serializable
data class MainRoute(
    val profileId: Long,
    val showSetupAggregates: Boolean = false
)

@Serializable
data class AggregatesRoute(
    val profileId: Long
)

@Serializable
data class OilsRoute(
    val profileId: Long
)

@Serializable
data class AggregatesOnBoardRoute(
    val profileId: Long
)

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = ProfileListRoute
    ) {
        composable<ProfileListRoute> {
            ProfileListScreen(
                onProfileSelected = { profileId, showSetup ->
                    navController.navigate(
                        MainRoute(
                            profileId = profileId,
                            showSetupAggregates = showSetup
                        )
                    )
                },
                onOpenSettings = { navController.navigate(SettingsRoute) }
            )
        }

        composable<MainRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<MainRoute>()
            MainScreen(
                profileId = route.profileId,
                showSetupAggregatesOnStart = route.showSetupAggregates,
                onBack = { navController.popBackStack() },
                onNavigateToAggregates = { profileId ->
                    navController.navigate(AggregatesRoute(profileId))
                },
                onNavigateToOils = { profileId ->
                    navController.navigate(OilsRoute(profileId))
                },
                onNavigateToAggregatesOnBoard = { profileId ->
                    navController.navigate(AggregatesOnBoardRoute(profileId))
                },
                onOpenSettings = {                          // ← НОВОЕ
                    navController.navigate(SettingsRoute)
                }
            )
        }

        composable<AggregatesRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AggregatesRoute>()
            AggregatesScreen(
                profileId = route.profileId,
                onBack = { navController.popBackStack() }
            )
        }

        composable<OilsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<OilsRoute>()
            OilsScreen(
                profileId = route.profileId,
                onBack = { navController.popBackStack() }
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onRestored = {
                    navController.navigate(ProfileListRoute) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<AggregatesOnBoardRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AggregatesOnBoardRoute>()
            AggregatesOnBoardScreen(
                profileId = route.profileId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
