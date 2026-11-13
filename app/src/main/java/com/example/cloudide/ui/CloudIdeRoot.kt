package com.example.cloudide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cloudide.ui.drawer.AppDrawerContent
import com.example.cloudide.ui.navigation.CloudIdeNavGraph
import com.example.cloudide.ui.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun CloudIdeRoot(viewModel: MainViewModel = hiltViewModel()) {
    val startRoute by viewModel.startRoute.collectAsStateWithLifecycle()
    val start = startRoute

    if (start == null) {
        // Reading the token (a few milliseconds): show only the background.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
    } else {
        CloudIdeContent(viewModel = viewModel, startDestination = start)
    }
}

@Composable
private fun CloudIdeContent(viewModel: MainViewModel, startDestination: String) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val drawerUiState by viewModel.drawerState.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val drawerEnabled = currentRoute in Routes.drawerRoutes

    // 401 anywhere: the session was cleared, go to the Connect screen.
    LaunchedEffect(Unit) {
        viewModel.sessionExpired.collect {
            drawerState.close()
            navController.navigate(Routes.Connect) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerEnabled,
        drawerContent = {
            AppDrawerContent(
                state = drawerUiState,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigateTopLevel(route)
                },
                onRepoClick = { repoName ->
                    viewModel.selectRecent(repoName)
                    scope.launch { drawerState.close() }
                    navController.navigateTopLevel(Routes.Editor)
                },
                onSignOut = {
                    scope.launch { drawerState.close() }
                    viewModel.signOut()
                    navController.navigate(Routes.Onboarding) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    ) {
        CloudIdeNavGraph(
            navController = navController,
            startDestination = startDestination,
            onOpenDrawer = { scope.launch { drawerState.open() } }
        )
    }
}

private fun NavController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(Routes.Projects) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}