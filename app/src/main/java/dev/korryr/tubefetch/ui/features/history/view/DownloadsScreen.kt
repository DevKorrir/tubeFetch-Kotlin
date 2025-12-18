package dev.korryr.tubefetch.ui.features.history.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.korryr.tubefetch.data.remote.DownloadFilter
import dev.korryr.tubefetch.domain.model.DownloadStatus
import dev.korryr.tubefetch.ui.features.history.viewModel.DownloadsEvent
import dev.korryr.tubefetch.ui.features.history.viewModel.DownloadsViewModel
import dev.korryr.tubefetch.ui.features.home.uiElements.DownloadFilters
import dev.korryr.tubefetch.ui.features.home.uiElements.EmptyDownloadsState
import dev.korryr.tubefetch.ui.features.home.uiElements.EnhancedDownloadItemCard
import dev.korryr.tubefetch.ui.features.home.uiElements.EnhancedDownloadsHeader

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
    
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
        ) {
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
                                dev.korryr.tubefetch.domain.model.DownloadFormat.MP3,
                                dev.korryr.tubefetch.domain.model.DownloadFormat.M4A,
                                dev.korryr.tubefetch.domain.model.DownloadFormat.WAV
                            )
                        },
                        DownloadFilter.VIDEO to uiState.downloads.count {
                            it.format in setOf(
                                dev.korryr.tubefetch.domain.model.DownloadFormat.MP4,
                                dev.korryr.tubefetch.domain.model.DownloadFormat.WEBM
                            )
                        }
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Enhanced Downloads Header
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
                        onPause = { viewModel.onEvent(DownloadsEvent.PauseDownload(it)) },
                        onResume = { viewModel.onEvent(DownloadsEvent.ResumeDownload(it)) },
                        onRetry = { viewModel.onEvent(DownloadsEvent.RetryDownload(it)) },
                        onDelete = { viewModel.onEvent(DownloadsEvent.DeleteDownload(it)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
        
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
