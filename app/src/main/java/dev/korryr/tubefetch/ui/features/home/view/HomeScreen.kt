import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import dev.korryr.tubefetch.domain.model.DownloadFormat
import dev.korryr.tubefetch.domain.model.DownloadStats
import dev.korryr.tubefetch.domain.model.VideoQuality
import dev.korryr.tubefetch.navigation.Routes
import dev.korryr.tubefetch.ui.features.home.uiElements.EnhancedHomeHeader
import dev.korryr.tubefetch.ui.features.home.uiElements.ErrorDialog
import dev.korryr.tubefetch.ui.features.home.uiElements.FormatSelectionSheet
import dev.korryr.tubefetch.ui.features.home.uiElements.QualitySelectionSheet
import dev.korryr.tubefetch.ui.features.home.uiElements.QuickActionsGrid
import dev.korryr.tubefetch.ui.features.home.uiElements.SmartUrlInputSection
import dev.korryr.tubefetch.ui.features.home.uiElements.StoragePermissionDialog
import dev.korryr.tubefetch.ui.features.home.viewModel.HomeEvent
import dev.korryr.tubefetch.ui.features.home.viewModel.HomeSideEffect
import dev.korryr.tubefetch.ui.features.home.viewModel.HomeViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.state
    val permissionState by viewModel.permissionState
    val sideEffects by viewModel.sideEffects.collectAsState(initial = null)

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Handle side effects
    LaunchedEffect(sideEffects) {
        sideEffects?.let { effect ->
            when (effect) {
                is HomeSideEffect.ShowMessage -> {
                    // Show snackbar
                }
                is HomeSideEffect.ShowError -> {
                    // Show error dialog
                }
                is HomeSideEffect.RequestStoragePermission -> {
                    if (context is Activity) {
                        viewModel.onEvent(HomeEvent.RequestStoragePermission(context))
                    }
                }
            }
        }
    }

    // Permission Dialog
    if (!permissionState.hasStoragePermission) {
        StoragePermissionDialog(
            onRequestPermission = {
                if (context is Activity) {
                    viewModel.onEvent(HomeEvent.RequestStoragePermission(context))
                }
            },
            onDismiss = { /* Can't dismiss - permission is required */ }
        )
    }

    // Error Dialog
    uiState.error?.let { error ->
        ErrorDialog(
            error = error,
            onDismiss = { viewModel.onEvent(HomeEvent.DismissError) }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Enhanced Header with Stats (simplified - no stats or show placeholder)
            item {
                EnhancedHomeHeader(
                    stats = DownloadStats(
                        totalDownloads = 0,
                        completedDownloads = 0,
                        totalSize = "0.00 MB",
                        activeDownloads = 0
                    ),
                    hasPermission = permissionState.hasStoragePermission,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Smart URL Input with Video Preview
            item {
                SmartUrlInputSection(
                    url = uiState.urlInput,
                    onUrlChange = { viewModel.onEvent(HomeEvent.UrlChanged(it)) },
                    onDownloadClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onEvent(HomeEvent.DownloadClicked)
                    },
                    selectedQuality = uiState.selectedQuality,
                    selectedFormat = uiState.selectedFormat,
                    onQualityClick = { viewModel.onEvent(HomeEvent.ShowQualitySheet) },
                    onFormatClick = { viewModel.onEvent(HomeEvent.ShowFormatSheet) },
                    videoInfo = uiState.videoInfo,
                    isAnalyzing = uiState.isAnalyzing,
                    hasPermission = permissionState.hasStoragePermission,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Quick Actions Grid (simplified)
            item {
                QuickActionsGrid(
                    onClearDownloads = {
                        // Navigate to downloads screen
                        navController.navigate(Routes.Downloads.route)
                    },
                    onClearCompleted = {
                        // This is now in Downloads screen
                        navController.navigate(Routes.Downloads.route)
                    },
                    modifier = Modifier.padding(16.dp),
                    onNavigateToDownloads = {
                        navController.navigate(Routes.Downloads.route)
                    }
                )
            }
        }

        // Loading Overlay
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Quality Selection Sheet
        if (uiState.showQualitySheet) {
            QualitySelectionSheet(
                selectedQuality = uiState.selectedQuality,
                availableQualities = VideoQuality.values().toList(),
                onQualitySelected = { viewModel.onEvent(HomeEvent.QualitySelected(it)) },
                onDismiss = { viewModel.onEvent(HomeEvent.HideQualitySheet) }
            )
        }

        // Format Selection Sheet
        if (uiState.showFormatSheet) {
            FormatSelectionSheet(
                selectedFormat = uiState.selectedFormat,
                availableFormats = DownloadFormat.values().toList(),
                onFormatSelected = { viewModel.onEvent(HomeEvent.FormatSelected(it)) },
                onDismiss = { viewModel.onEvent(HomeEvent.HideFormatSheet) }
            )
        }
    }
}