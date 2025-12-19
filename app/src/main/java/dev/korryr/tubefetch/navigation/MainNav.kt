package dev.korryr.tubefetch.navigation

import HomeScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.korryr.tubefetch.core.connectivity.NetworkStatus
import dev.korryr.tubefetch.ui.connectivity.ConnectivityViewModel
import dev.korryr.tubefetch.ui.features.browser.view.BrowserScreen
import dev.korryr.tubefetch.ui.features.history.view.DownloadsScreen
import dev.korryr.tubefetch.ui.features.settings.view.SettingsScreen

@Composable
fun MainNav(
    navController: androidx.navigation.NavHostController,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val connectivityVm: ConnectivityViewModel = hiltViewModel()

    LaunchedEffect(connectivityVm) {
        connectivityVm.status.collect { s ->
            val msg = when (s) {
                is NetworkStatus.Available -> "Back online"
                is NetworkStatus.Losing -> "Network is unstable"
                is NetworkStatus.Lost -> "No internet connection"
                is NetworkStatus.Unavailable -> "No internet connection"
                is NetworkStatus.Slow -> "Slow internet connection"
            }
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = { BottomBarRow(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home.route,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(route = Routes.Home.route) {
                HomeScreen(navController = navController)
            }

            composable(route = Routes.Downloads.route) {
                DownloadsScreen()
            }

            composable(route = Routes.Browser.route) {
                BrowserScreen()
            }

            composable(route = Routes.Settings.route) {
                SettingsScreen()
            }
        }
    }
}