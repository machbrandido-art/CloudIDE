package com.example.cloudide.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.cloudide.BuildConfig
import com.example.cloudide.R
import com.example.cloudide.ui.connect.ConnectScreen
import com.example.cloudide.ui.gallery.ComponentsGalleryScreen
import com.example.cloudide.ui.onboarding.OnboardingScreen
import com.example.cloudide.ui.placeholder.PlaceholderScreen
import com.example.cloudide.ui.projects.ProjectsScreen

@Composable
fun CloudIdeNavGraph(
    navController: NavHostController,
    startDestination: String,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.Onboarding) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Routes.Connect) { launchSingleTop = true }
                }
            )
        }
        composable(Routes.Connect) {
            // No back arrow when Connect is the only screen (e.g. after the session expired).
            val canGoBack = navController.previousBackStackEntry != null
            ConnectScreen(
                onConnected = {
                    navController.navigate(Routes.Projects) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = if (canGoBack) {
                    { navController.popBackStack() }
                } else {
                    null
                }
            )
        }
        composable(Routes.Projects) {
            ProjectsScreen(
                onMenuClick = onOpenDrawer,
                onOpenEditor = {
                    navController.navigate(Routes.Editor) { launchSingleTop = true }
                },
                onViewError = {
                    // Stub: the real error screen comes in Stages 5 and 7.
                    navController.navigate(Routes.Build) { launchSingleTop = true }
                }
            )
        }
        composable(Routes.Editor) {
            PlaceholderScreen(
                title = stringResource(R.string.nav_editor),
                onMenuClick = onOpenDrawer
            )
        }
        composable(Routes.Build) {
            PlaceholderScreen(
                title = stringResource(R.string.screen_build),
                onMenuClick = onOpenDrawer
            )
        }
        composable(Routes.Settings) {
            PlaceholderScreen(
                title = stringResource(R.string.nav_settings),
                onMenuClick = onOpenDrawer
            )
        }
        if (BuildConfig.DEBUG) {
            composable(Routes.Gallery) {
                ComponentsGalleryScreen(onMenuClick = onOpenDrawer)
            }
        }
    }
}