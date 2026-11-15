package com.example.cloudide.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.cloudide.ui.build.BuildScreen
import com.example.cloudide.ui.build.BuildsListScreen
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
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.Onboarding) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Routes.Connect) {
                        popUpTo(Routes.Onboarding) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Connect) {
            ConnectScreen(
                onConnected = {
                    navController.navigate(Routes.Projects) {
                        popUpTo(Routes.Connect) { inclusive = true }
                    }
                },
                onBack = null,
            )
        }

        composable(Routes.Projects) {
            ProjectsScreen(
                onMenuClick = onOpenDrawer,
                onOpenEditor = {
                    navController.navigate(Routes.Editor) { launchSingleTop = true }
                },
                onViewError = {
                    navController.navigate(Routes.BuildsList) { launchSingleTop = true }
                },
            )
        }

        composable(Routes.Editor) {
            PlaceholderScreen(
                title = "Editor",
                onMenuClick = onOpenDrawer,
            )
        }

        composable(Routes.Build) {
            BuildScreen(onMenuClick = onOpenDrawer)
        }

        composable(Routes.BuildsList) {
            BuildsListScreen(
                onMenuClick = onOpenDrawer,
                onOpenRun = {
                    navController.navigate(Routes.Build) { launchSingleTop = true }
                },
            )
        }

        composable(Routes.Settings) {
            PlaceholderScreen(
                title = "Settings",
                onMenuClick = onOpenDrawer,
            )
        }

        composable(Routes.Gallery) {
            ComponentsGalleryScreen(onMenuClick = onOpenDrawer)
        }
    }
}