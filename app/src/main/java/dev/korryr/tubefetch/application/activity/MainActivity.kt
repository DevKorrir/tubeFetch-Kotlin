package dev.korryr.tubefetch.application.activity

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.korryr.tubefetch.domain.model.ThemeMode
import dev.korryr.tubefetch.navigation.MainNav
import dev.korryr.tubefetch.ui.features.home.viewModel.HomeViewModel
import dev.korryr.tubefetch.ui.features.settings.viewModel.SettingsViewModel
import dev.korryr.tubefetch.ui.theme.TubeFetchTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == HomeViewModel.REQUEST_STORAGE_PERMISSION) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults.getOrNull(1) == PackageManager.PERMISSION_GRANTED
            homeViewModel.onPermissionResult(granted)
        }
    }
}
