package dev.korryr.tubefetch.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Light Color Scheme - Clean and vibrant
private val LightColorScheme = lightColorScheme(
    // Primary colors
    primary = RedPrimary40,
    onPrimary = Color.White,
    primaryContainer = RedPrimary90,
    onPrimaryContainer = Color(0xFF8B0000),

    // Secondary colors
    secondary = Orange40,
    onSecondary = Color.White,
    secondaryContainer = Orange90,
    onSecondaryContainer = Color(0xFF8B4513),

    // Tertiary colors
    tertiary = Purple40,
    onTertiary = Color.White,
    tertiaryContainer = Purple90,
    onTertiaryContainer = Color(0xFF4A148C),

    // Error colors
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Color(0xFF8B0000),

    // Background colors
    background = Grey99,
    onBackground = Grey10,

    // Surface colors
    surface = Color.White,
    onSurface = Grey10,
    surfaceVariant = Grey90,
    onSurfaceVariant = Grey40,

    // Surface containers
    surfaceContainer = Grey95,
    surfaceContainerHigh = Grey90,
    surfaceContainerHighest = Grey80,
    surfaceContainerLow = Grey99,
    surfaceContainerLowest = Color.White,

    // Outline colors
    outline = Grey60,
    outlineVariant = Grey80,

    // Other colors
    scrim = Color.Black,
    inverseSurface = Grey20,
    inverseOnSurface = Grey90,
    inversePrimary = RedPrimary80,

    // Surface tints
    surfaceTint = RedPrimary40
)

// Dark Color Scheme - Elegant and modern
private val DarkColorScheme = darkColorScheme(
    // Primary colors
    primary = RedPrimary80,
    onPrimary = Color(0xFF2D0A0A),
    primaryContainer = Color(0xFF8B2635),
    onPrimaryContainer = RedPrimary90,

    // Secondary colors
    secondary = Orange80,
    onSecondary = Color(0xFF2D1A0A),
    secondaryContainer = Color(0xFFB8621F),
    onSecondaryContainer = Orange90,

    // Tertiary colors
    tertiary = Purple80,
    onTertiary = Color(0xFF2D0A2D),
    tertiaryContainer = Color(0xFF6A1B9A),
    onTertiaryContainer = Purple90,

    // Error colors
    error = Red80,
    onError = Color(0xFF2D0A0A),
    errorContainer = Color(0xFFB71C1C),
    onErrorContainer = Red90,

    // Background colors
    background = Grey10,
    onBackground = Grey90,

    // Surface colors
    surface = Grey20,
    onSurface = Grey90,
    surfaceVariant = Grey30,
    onSurfaceVariant = Grey70,

    // Surface containers
    surfaceContainer = Grey20,
    surfaceContainerHigh = Grey30,
    surfaceContainerHighest = Grey40,
    surfaceContainerLow = Surface10,
    surfaceContainerLowest = Surface10,

    // Outline colors
    outline = Grey60,
    outlineVariant = Grey40,

    // Other colors
    scrim = Color.Black,
    inverseSurface = Grey90,
    inverseOnSurface = Grey20,
    inversePrimary = RedPrimary40,

    // Surface tints
    surfaceTint = RedPrimary80
)

// Custom colors for specific use cases in TubeFetch
object TubeFetchColors {
    // Download status colors for light mode
    val downloadingLight = Color(0xFF2196F3)      // Blue for downloading
    val completedLight = Green40                   // Green for completed
    val pendingLight = Amber40                     // Amber for pending
    val failedLight = Red40                        // Red for failed

    // Download status colors for dark mode
    val downloadingDark = Color(0xFF64B5F6)       // Light blue for downloading
    val completedDark = Green80                    // Light green for completed
    val pendingDark = Amber80                      // Light amber for pending
    val failedDark = Red80                         // Light red for failed

    // Progress colors
    val progressLight = RedPrimary40
    val progressDark = RedPrimary80

    // Accent colors for special elements
    val accentLight = Color(0xFFE91E63)           // Pink accent
    val accentDark = Color(0xFFFF4081)            // Light pink accent

    // Video thumbnail placeholder colors
    val thumbnailLight = Grey80
    val thumbnailDark = Grey40

    // Gradient colors for headers and special sections
    val gradientStart = RedPrimary40
    val gradientEnd = Orange40
    val gradientStartDark = RedPrimary80
    val gradientEndDark = Orange80
}

// Extension functions to get theme-aware colors
@androidx.compose.runtime.Composable
fun getDownloadStatusColor(status: String, isDark: Boolean = androidx.compose.foundation.isSystemInDarkTheme()): Color {
    return when (status.lowercase()) {
        "downloading" -> if (isDark) TubeFetchColors.downloadingDark else TubeFetchColors.downloadingLight
        "completed" -> if (isDark) TubeFetchColors.completedDark else TubeFetchColors.completedLight
        "pending" -> if (isDark) TubeFetchColors.pendingDark else TubeFetchColors.pendingLight
        "failed" -> if (isDark) TubeFetchColors.failedDark else TubeFetchColors.failedLight
        else -> if (isDark) Grey60 else Grey40
    }
}

@androidx.compose.runtime.Composable
fun getGradientColors(isDark: Boolean = androidx.compose.foundation.isSystemInDarkTheme()): Pair<Color, Color> {
    return if (isDark) {
        Pair(TubeFetchColors.gradientStartDark, TubeFetchColors.gradientEndDark)
    } else {
        Pair(TubeFetchColors.gradientStart, TubeFetchColors.gradientEnd)
    }
}

// Export the color schemes
val tubeFetchLightColorScheme = LightColorScheme
val tubeFetchDarkColorScheme = DarkColorScheme

@Composable
fun TubeFetchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}