package dev.korryr.tubefetch.application.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import dev.korryr.tubefetch.navigation.MainNav
import dev.korryr.tubefetch.ui.theme.TubeFetchTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            TubeFetchTheme (
                darkTheme = true
            ) {

                MainNav(
                    navController = navController,
                    modifier = Modifier
                )
            }
        }
    }
}
