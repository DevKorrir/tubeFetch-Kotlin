package dev.korryr.tubefetch.ui.features.history.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.korryr.tubefetch.data.remote.DownloadFilter
import dev.korryr.tubefetch.domain.model.DownloadFormat
import dev.korryr.tubefetch.domain.model.DownloadStatus
import dev.korryr.tubefetch.ui.features.history.elements.DownloadFilters
import dev.korryr.tubefetch.ui.features.history.elements.DownloadItemCard
import dev.korryr.tubefetch.ui.features.history.elements.DownloadsHeader
import dev.korryr.tubefetch.ui.features.history.elements.EmptyDownloadsState
import dev.korryr.tubefetch.ui.features.history.viewModel.DownloadsEvent
import dev.korryr.tubefetch.ui.features.history.viewModel.DownloadsViewModel

@Composable
fun DownloadsScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    var selectedFilter by remember { mutableStateOf(DownloadFilter.ALL) }

    val filteredDownloads = remember(uiState.downloads, selectedFilter) {
        viewModel.getFilteredDownloads()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Enhanced Downloads Header
            item {
                DownloadsHeader(
                    totalCount = filteredDownloads.size,
                    filter = selectedFilter,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }

            // Download Filters
            item {
                DownloadFilters(
                    selectedFilter = selectedFilter,
                    onFilterSelected = {
                        selectedFilter = it
                        viewModel.onEvent(DownloadsEvent.FilterChanged(it))
                    },
                    downloadCounts = mapOf(
                        DownloadFilter.ALL to uiState.downloads.size,
                        DownloadFilter.DOWNLOADING to uiState.downloads.count {
                            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
                        },
                        DownloadFilter.COMPLETED to uiState.downloads.count {
                            it.status == DownloadStatus.COMPLETED
                        },
                        DownloadFilter.FAILED to uiState.downloads.count {
                            it.status == DownloadStatus.FAILED
                        },
                        DownloadFilter.AUDIO to uiState.downloads.count {
                            it.format in setOf(
                                DownloadFormat.MP3,
                                DownloadFormat.M4A,
                                DownloadFormat.WAV
                            )
                        },
                        DownloadFilter.VIDEO to uiState.downloads.count {
                            it.format in setOf(
                                DownloadFormat.MP4,
                                DownloadFormat.WEBM
                            )
                        }
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            if (filteredDownloads.isEmpty()) {
                item {
                    EmptyDownloadsState(
                        filter = selectedFilter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                }
            } else {
                items(
                    items = filteredDownloads,
                    key = { it.id }
                ) { download ->
                    DownloadItemCard(
                        download = download,
                        onPause = { viewModel.onEvent(DownloadsEvent.PauseDownload(it)) },
                        onResume = { viewModel.onEvent(DownloadsEvent.ResumeDownload(it)) },
                        onRetry = { viewModel.onEvent(DownloadsEvent.RetryDownload(it)) },
                        onDelete = { viewModel.onEvent(DownloadsEvent.DeleteDownload(it)) },
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
//                            .animateItemPlacement(
//                                animationSpec = spring(
//                                    dampingRatio = Spring.DampingRatioMediumBouncy,
//                                    stiffness = Spring.StiffnessLow
//                                )
//                            )
                    )
                }
            }
        }

        // Cute Loading Indicator
        AnimatedVisibility(
            visible = uiState.isLoading,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Card(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                }
            }
        }
    }
}