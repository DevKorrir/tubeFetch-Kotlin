package dev.korryr.tubefetch.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Routes (
    val route: String
){
    object Home : Routes("home")
    object Downloads : Routes("downloads")
    object Settings : Routes("settings")
    object Browser : Routes("browser")
    object About : Routes("about")
    object Help : Routes("help")

}

sealed class Bottombar (
    val route: String,
    val icon: ImageVector,
    val title: String,
) {
    object Home : Bottombar(Routes.Home.route, Icons.Default.Home, "Home")
    object Downloads : Bottombar("downloads", Icons.Default.Download, "Downloads")
    object Browser : Bottombar(Routes.Browser.route, Icons.Default.Public, "Browser")
    object Settings : Bottombar("settings", Icons.Default.Settings, "Settings")

    companion object {
        val bottomBarItems = listOf(
            Bottombar.Home,
            Bottombar.Downloads,
            Bottombar.Browser,
            Bottombar.Settings
        )
    }
}