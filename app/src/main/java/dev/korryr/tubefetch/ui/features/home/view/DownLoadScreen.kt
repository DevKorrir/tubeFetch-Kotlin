package dev.korryr.tubefetch.ui.features.home.view

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.korryr.tubefetch.data.remote.DownloadFilter
import dev.korryr.tubefetch.domain.model.DownloadFormat
import dev.korryr.tubefetch.domain.model.DownloadStatus
import dev.korryr.tubefetch.domain.model.VideoQuality
import dev.korryr.tubefetch.ui.features.home.uiElements.DownloadFilters
import dev.korryr.tubefetch.ui.features.home.uiElements.EmptyDownloadsState
import dev.korryr.tubefetch.ui.features.home.uiElements.EnhancedDownloadItemCard
import dev.korryr.tubefetch.ui.features.home.uiElements.EnhancedDownloadsHeader
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: androidx.navigation.NavController,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.state
    val permissionState by viewModel.permissionState
    val sideEffects by viewModel.sideEffects.collectAsState(initial = null)
    //val sideEffects by viewModel.sideEffects.collectAsState(initial = null)

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var selectedFilter by remember { mutableStateOf(DownloadFilter.ALL) }

    // Handle side effects
    LaunchedEffect(sideEffects) {
        sideEffects?.let { effect ->
            when (effect) {
                is HomeSideEffect.ShowMessage -> {
                    // Show snackbar
                    //ScaffoldHost.showSnackbar(effect.message)
                }
                is HomeSideEffect.ShowError -> {
                    // Show error dialog
                    //ScaffoldHost.showSnackbar(effect.message, isError = true)
                }
                is HomeSideEffect.RequestStoragePermission -> {
                    // Request permission
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

    // Filter downloads based on selected filter
    val filteredDownloads = remember(uiState.downloads, selectedFilter) {
        when (selectedFilter) {
            DownloadFilter.ALL -> uiState.downloads
            DownloadFilter.DOWNLOADING -> uiState.downloads.filter {
                it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
            }
            DownloadFilter.COMPLETED -> uiState.downloads.filter { it.status == DownloadStatus.COMPLETED }
            DownloadFilter.FAILED -> uiState.downloads.filter { it.status == DownloadStatus.FAILED }
            DownloadFilter.AUDIO -> uiState.downloads.filter {
                it.format in listOf(DownloadFormat.MP3, DownloadFormat.M4A, DownloadFormat.WAV)
            }
            DownloadFilter.VIDEO -> uiState.downloads.filter {
                it.format in listOf(DownloadFormat.MP4, DownloadFormat.WEBM)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Enhanced Header with Stats
            item {
                EnhancedHomeHeader(
                    stats = uiState.downloadStats,
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

            // Quick Actions Grid
            item {
                QuickActionsGrid(
                    onClearDownloads = { viewModel.onEvent(HomeEvent.ClearDownloads) },
                    onClearCompleted = { viewModel.onEvent(HomeEvent.ClearCompleted) },
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Download Filters
            item {
                DownloadFilters(
                    selectedFilter = selectedFilter,
                    onFilterSelected = {
                        selectedFilter = it
                        viewModel.onEvent(HomeEvent.FilterChanged(it))
                    },
                    downloadCounts = mapOf(
                        DownloadFilter.ALL to uiState.downloads.size,
                        DownloadFilter.DOWNLOADING to uiState.downloads.count {
                            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
                        },
                        DownloadFilter.COMPLETED to uiState.downloads.count { it.status == DownloadStatus.COMPLETED },
                        DownloadFilter.FAILED to uiState.downloads.count { it.status == DownloadStatus.FAILED }
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Enhanced Downloads Section
            item {
                EnhancedDownloadsHeader(
                    totalCount = filteredDownloads.size,
                    filter = selectedFilter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (filteredDownloads.isEmpty()) {
                item {
                    EmptyDownloadsState(
                        filter = selectedFilter,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                items(filteredDownloads, key = { it.id }) { download ->
                    EnhancedDownloadItemCard(
                        download = download,
                        onPause = { viewModel.onEvent(HomeEvent.PauseDownload(it)) },
                        onResume = { viewModel.onEvent(HomeEvent.ResumeDownload(it)) },
                        onRetry = { viewModel.onEvent(HomeEvent.RetryDownload(it)) },
                        onDelete = { viewModel.onEvent(HomeEvent.DeleteDownload(it)) },
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(100.dp))
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