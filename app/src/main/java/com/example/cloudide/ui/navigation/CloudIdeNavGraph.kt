package com.example.cloudide.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.cloudide.ui.build.BuildScreen
import com.example.cloudide.ui.connect.ConnectScreen
import com.example.cloudide.ui.gallery.ComponentsGalleryScreen
import com.example.cloudide.ui.onboarding.OnboardingScreen
import com.example.cloudide.ui.placeholder.PlaceholderScreen
import com.example.cloudide.ui.projects.ProjectsScreen

@Composable
fun CloudIdeNavGraph(
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Onboarding,
    ) {
        composable(Routes.Onboarding) {
            OnboardingScreen(
                onContinue = {
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
                }
            )
        }

        composable(Routes.Projects) {
            ProjectsScreen(onOpenDrawer = onOpenDrawer)
        }

        composable(Routes.Editor) {
            PlaceholderScreen(
                title = "Editor",
                onOpenDrawer = onOpenDrawer,
            )
        }

        composable(Routes.Build) {
            BuildScreen(onOpenDrawer = onOpenDrawer)
        }

        composable(Routes.Settings) {
            PlaceholderScreen(
                title = "Settings",
                onOpenDrawer = onOpenDrawer,
            )
        }

        composable(Routes.Gallery) {
            ComponentsGalleryScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}