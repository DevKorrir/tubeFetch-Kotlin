package dev.korryr.tubefetch.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.korryr.tubefetch.ui.features.home.view.HomeScreen

@Composable
fun MainNav(
    navController: androidx.navigation.NavHostController,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
        }
    }

}