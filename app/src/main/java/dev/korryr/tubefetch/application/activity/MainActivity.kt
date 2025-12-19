package dev.korryr.tubefetch.application.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.korryr.tubefetch.navigation.MainNav
import dev.korryr.tubefetch.ui.theme.TubeFetchTheme
import dev.korryr.tubefetch.ui.features.settings.viewModel.SettingsViewModel
import dev.korryr.tubefetch.domain.model.ThemeMode

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            TubeFetchTheme(
                darkTheme = darkTheme
            ) {

                MainNav(
                    navController = navController,
                    modifier = Modifier
                )
            }
        }
    }
}
