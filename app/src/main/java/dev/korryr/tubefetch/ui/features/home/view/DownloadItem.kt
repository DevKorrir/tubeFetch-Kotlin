@file:OptIn(ExperimentalMaterial3Api::class)

package dev.korryr.tubefetch.ui.features.home.view

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

// Enhanced Data Models
data class DownloadItem(
    val id: String,
    val title: String,
    val duration: String,
    val thumbnail: String,
    val status: DownloadStatus,
    val progress: Float = 0f,
    val fileSize: String = "",
    val downloadSpeed: String = "",
    val quality: VideoQuality = VideoQuality.HD720,
    val format: DownloadFormat = DownloadFormat.MP4,
    val url: String = "",
    val channelName: String = "",
    val viewCount: String = "",
    val uploadDate: String = "",
    val downloadPath: String = ""
)

enum class DownloadStatus {
    PENDING, DOWNLOADING, COMPLETED, FAILED, PAUSED, QUEUED, PROCESSING
}

enum class VideoQuality(val displayName: String, val resolution: String) {
    AUTO("Auto", "Best Available"),
    SD360("360p", "640×360"),
    SD480("480p", "854×480"),
    HD720("720p", "1280×720"),
    HD1080("1080p", "1920×1080"),
    QHD1440("1440p", "2560×1440"),
    UHD2160("2160p", "3840×2160"),
    UHD4K("4K", "4096×2160")
}

enum class DownloadFormat(val displayName: String, val extension: String, val icon: ImageVector) {
    MP4("Video (MP4)", "mp4", Icons.Rounded.VideoFile),
    MP3("Audio (MP3)", "mp3", Icons.Rounded.AudioFile),
    WEBM("Video (WebM)", "webm", Icons.Rounded.VideoFile),
    M4A("Audio (M4A)", "m4a", Icons.Rounded.AudioFile),
    WAV("Audio (WAV)", "wav", Icons.Rounded.AudioFile)
}

data class VideoInfo(
    val title: String,
    val duration: String,
    val thumbnail: String,
    val channelName: String,
    val viewCount: String,
    val uploadDate: String,
    val description: String,
    val availableQualities: List<VideoQuality>
)

data class DownloadStats(
    val totalDownloads: Int,
    val completedDownloads: Int,
    val totalSize: String,
    val activeDownloads: Int
)

// Enhanced ViewModel
@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    private val _uiState = mutableStateOf(HomeUiState())
    val uiState: State<HomeUiState> = _uiState

    private val _urlInput = mutableStateOf("")
    val urlInput: State<String> = _urlInput

    private val _selectedQuality = mutableStateOf(VideoQuality.AUTO)
    val selectedQuality: State<VideoQuality> = _selectedQuality

    private val _selectedFormat = mutableStateOf(DownloadFormat.MP4)
    val selectedFormat: State<DownloadFormat> = _selectedFormat

    private val _showQualitySheet = mutableStateOf(false)
    val showQualitySheet: State<Boolean> = _showQualitySheet

    private val _showFormatSheet = mutableStateOf(false)
    val showFormatSheet: State<Boolean> = _showFormatSheet

    private val _videoInfo = mutableStateOf<VideoInfo?>(null)
    val videoInfo: State<VideoInfo?> = _videoInfo

    private val _isAnalyzing = mutableStateOf(false)
    val isAnalyzing: State<Boolean> = _isAnalyzing

    private val _downloadStats = mutableStateOf(DownloadStats(0, 0, "0 MB", 0))
    val downloadStats: State<DownloadStats> = _downloadStats

    fun onUrlInputChanged(url: String) {
        _urlInput.value = url
        if (url.isNotEmpty() && isValidYouTubeUrl(url)) {
            analyzeVideo(url)
        } else {
            _videoInfo.value = null
        }
    }

    fun onQualitySelected(quality: VideoQuality) {
        _selectedQuality.value = quality
        _showQualitySheet.value = false
    }

    fun onFormatSelected(format: DownloadFormat) {
        _selectedFormat.value = format
        _showFormatSheet.value = false
    }

    fun showQualitySheet() {
        _showQualitySheet.value = true
    }

    fun hideQualitySheet() {
        _showQualitySheet.value = false
    }

    fun showFormatSheet() {
        _showFormatSheet.value = true
    }

    fun hideFormatSheet() {
        _showFormatSheet.value = false
    }

    fun onDownloadClick() {
        if (_urlInput.value.isNotBlank()) {
            startDownload(_urlInput.value)
            _urlInput.value = ""
            _videoInfo.value = null
        }
    }

    fun onPauseDownload(downloadId: String) {
        val currentDownloads = _uiState.value.downloads.toMutableList()
        val index = currentDownloads.indexOfFirst { it.id == downloadId }
        if (index != -1) {
            currentDownloads[index] = currentDownloads[index].copy(
                status = if (currentDownloads[index].status == DownloadStatus.DOWNLOADING)
                    DownloadStatus.PAUSED else DownloadStatus.DOWNLOADING
            )
            _uiState.value = _uiState.value.copy(downloads = currentDownloads)
        }
    }

    fun onRetryDownload(downloadId: String) {
        val currentDownloads = _uiState.value.downloads.toMutableList()
        val index = currentDownloads.indexOfFirst { it.id == downloadId }
        if (index != -1) {
            currentDownloads[index] = currentDownloads[index].copy(
                status = DownloadStatus.QUEUED,
                progress = 0f
            )
            _uiState.value = _uiState.value.copy(downloads = currentDownloads)
        }
    }

    fun onDeleteDownload(downloadId: String) {
        val currentDownloads = _uiState.value.downloads.toMutableList()
        currentDownloads.removeIf { it.id == downloadId }
        _uiState.value = _uiState.value.copy(downloads = currentDownloads)
        updateDownloadStats()
    }

    fun onClearDownloads() {
        _uiState.value = _uiState.value.copy(downloads = emptyList())
        updateDownloadStats()
    }

    fun onClearCompleted() {
        val currentDownloads = _uiState.value.downloads.toMutableList()
        currentDownloads.removeIf { it.status == DownloadStatus.COMPLETED }
        _uiState.value = _uiState.value.copy(downloads = currentDownloads)
        updateDownloadStats()
    }

    private fun analyzeVideo(url: String) {
        _isAnalyzing.value = true
        // Simulate video analysis
        // In real implementation, use your repository/use case here
    }

    private fun startDownload(url: String) {
        val newDownload = DownloadItem(
            id = System.currentTimeMillis().toString(),
            title = _videoInfo.value?.title ?: "Downloaded Video",
            duration = _videoInfo.value?.duration ?: "Unknown",
            thumbnail = _videoInfo.value?.thumbnail ?: "",
            status = DownloadStatus.QUEUED,
            quality = _selectedQuality.value,
            format = _selectedFormat.value,
            url = url,
            channelName = _videoInfo.value?.channelName ?: "",
            viewCount = _videoInfo.value?.viewCount ?: "",
            uploadDate = _videoInfo.value?.uploadDate ?: ""
        )

        val currentDownloads = _uiState.value.downloads.toMutableList()
        currentDownloads.add(0, newDownload)
        _uiState.value = _uiState.value.copy(downloads = currentDownloads)
        updateDownloadStats()
    }

    private fun updateDownloadStats() {
        val downloads = _uiState.value.downloads
        _downloadStats.value = DownloadStats(
            totalDownloads = downloads.size,
            completedDownloads = downloads.count { it.status == DownloadStatus.COMPLETED },
            totalSize = "${downloads.size * 45} MB", // Mock calculation
            activeDownloads = downloads.count {
                it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
            }
        )
    }

    private fun isValidYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com") || url.contains("youtu.be")
    }
}

data class HomeUiState(
    val downloads: List<DownloadItem> = enhancedSampleDownloads,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFilter: DownloadFilter = DownloadFilter.ALL
)

enum class DownloadFilter(val displayName: String) {
    ALL("All"),
    DOWNLOADING("Downloading"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    AUDIO("Audio Only"),
    VIDEO("Video Only")
}

// Enhanced sample data
val enhancedSampleDownloads = listOf(
    DownloadItem(
        "1", "Amazing 4K Nature Documentary - Wildlife Adventures", "45:30", "",
        DownloadStatus.COMPLETED, fileSize = "2.1 GB", quality = VideoQuality.UHD2160,
        format = DownloadFormat.MP4, channelName = "Nature Channel", viewCount = "2.3M views",
        uploadDate = "2 days ago"
    ),
    DownloadItem(
        "2", "Learn Kotlin Coroutines - Advanced Android Development", "28:45", "",
        DownloadStatus.DOWNLOADING, 0.65f, "450 MB", "2.3 MB/s", VideoQuality.HD1080,
        format = DownloadFormat.MP4, channelName = "Android Developers", viewCount = "156K views",
        uploadDate = "1 week ago"
    ),
    DownloadItem(
        "3", "Best Relaxing Music Mix 2024 - 3 Hours", "3:15:20", "",
        DownloadStatus.QUEUED, fileSize = "180 MB", format = DownloadFormat.MP3,
        channelName = "Music Paradise", viewCount = "5.2M views", uploadDate = "3 days ago"
    ),
    DownloadItem(
        "4", "Flutter vs React Native - Complete Comparison", "35:15", "",
        DownloadStatus.FAILED, quality = VideoQuality.HD720, format = DownloadFormat.MP4,
        channelName = "Tech Reviews", viewCount = "890K views", uploadDate = "5 days ago"
    ),
    DownloadItem(
        "5", "Morning Meditation - Peaceful Sounds", "20:00", "",
        DownloadStatus.PAUSED, 0.35f, "95 MB", format = DownloadFormat.MP3,
        channelName = "Wellness Studio", viewCount = "234K views", uploadDate = "1 day ago"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedHomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState
    val urlInput by viewModel.urlInput
    val selectedQuality by viewModel.selectedQuality
    val selectedFormat by viewModel.selectedFormat
    val showQualitySheet by viewModel.showQualitySheet
    val showFormatSheet by viewModel.showFormatSheet
    val videoInfo by viewModel.videoInfo
    val isAnalyzing by viewModel.isAnalyzing
    val downloadStats by viewModel.downloadStats

    val haptic = LocalHapticFeedback.current
    var selectedFilter by remember { mutableStateOf(DownloadFilter.ALL) }

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
                    stats = downloadStats,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Smart URL Input with Video Preview
            item {
                SmartUrlInputSection(
                    url = urlInput,
                    onUrlChange = viewModel::onUrlInputChanged,
                    onDownloadClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onDownloadClick()
                    },
                    selectedQuality = selectedQuality,
                    selectedFormat = selectedFormat,
                    onQualityClick = { viewModel.showQualitySheet() },
                    onFormatClick = { viewModel.showFormatSheet() },
                    videoInfo = videoInfo,
                    isAnalyzing = isAnalyzing,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Quick Actions Grid
            item {
                QuickActionsGrid(
                    onClearDownloads = viewModel::onClearDownloads,
                    onClearCompleted = viewModel::onClearCompleted,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Download Filters
            item {
                DownloadFilters(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
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
                        onPause = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.onPauseDownload(it)
                        },
                        onRetry = { viewModel.onRetryDownload(it) },
                        onDelete = { viewModel.onDeleteDownload(it) },
                        modifier = Modifier
                            //.animateItemPlacement()
                            .padding(horizontal = 16.dp, vertical = 4.dp)

                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // Quality Selection Sheet
        if (showQualitySheet) {
            QualitySelectionSheet(
                selectedQuality = selectedQuality,
                availableQualities = VideoQuality.values().toList(),
                onQualitySelected = viewModel::onQualitySelected,
                onDismiss = viewModel::hideQualitySheet
            )
        }

        // Format Selection Sheet
        if (showFormatSheet) {
            FormatSelectionSheet(
                selectedFormat = selectedFormat,
                availableFormats = DownloadFormat.values().toList(),
                onFormatSelected = viewModel::onFormatSelected,
                onDismiss = viewModel::hideFormatSheet
            )
        }
    }
}

@Composable
private fun EnhancedHomeHeader(
    stats: DownloadStats,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        // Gradient Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Decorative Elements
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val circleRadius = size.width * 0.3f
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = circleRadius,
                center = Offset(size.width * 0.8f, -circleRadius * 0.3f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.03f),
                radius = circleRadius * 0.6f,
                center = Offset(size.width * 1.1f, size.height * 0.3f)
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row - Title and Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TubeFetch Pro",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Text(
                        text = "Smart YouTube Downloader",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GetApp,
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatsItem(
                    value = stats.totalDownloads.toString(),
                    label = "Total",
                    icon = Icons.Rounded.Download,
                    modifier = Modifier.weight(1f)
                )
                StatsItem(
                    value = stats.completedDownloads.toString(),
                    label = "Completed",
                    icon = Icons.Rounded.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                StatsItem(
                    value = stats.totalSize,
                    label = "Size",
                    icon = Icons.Rounded.Storage,
                    modifier = Modifier.weight(1f)
                )
                StatsItem(
                    value = stats.activeDownloads.toString(),
                    label = "Active",
                    icon = Icons.Rounded.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatsItem(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartUrlInputSection(
    url: String,
    onUrlChange: (String) -> Unit,
    onDownloadClick: () -> Unit,
    selectedQuality: VideoQuality,
    selectedFormat: DownloadFormat,
    onQualityClick: () -> Unit,
    onFormatClick: () -> Unit,
    videoInfo: VideoInfo?,
    isAnalyzing: Boolean,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showVideoPreview by remember { mutableStateOf(false) }

    LaunchedEffect(videoInfo) {
        showVideoPreview = videoInfo != null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = (-28).dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "YouTube URL",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "Paste or search for videos",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // URL Input with Smart Features
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                placeholder = {
                    Text(
                        text = "https://youtube.com/watch?v=... or search",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                leadingIcon = {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                trailingIcon = {
                    Row {
                        if (url.isNotEmpty()) {
                            IconButton(onClick = { onUrlChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { text ->
                                    onUrlChange(text)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentPaste,
                                contentDescription = "Paste",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (url.isNotBlank()) onDownloadClick() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            // Video Preview Card (when video info is available)
            AnimatedVisibility(
                visible = showVideoPreview && videoInfo != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                videoInfo?.let { info ->
                    VideoPreviewCard(
                        videoInfo = info,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Download Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quality Selection
                OptionCard(
                    title = "Quality",
                    value = selectedQuality.displayName,
                    icon = Icons.Rounded.HighQuality,
                    onClick = onQualityClick,
                    modifier = Modifier.weight(1f)
                )

                // Format Selection
                OptionCard(
                    title = "Format",
                    value = selectedFormat.displayName.split(" ")[0],
                    icon = selectedFormat.icon,
                    onClick = onFormatClick,
                    modifier = Modifier.weight(1f)
                )
            }

            // Download Button with Enhanced States
            Button(
                onClick = onDownloadClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = url.isNotBlank() && !isAnalyzing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Analyzing...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Start Download",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPreviewCard(
    videoInfo: VideoInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(80.dp, 60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = videoInfo.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = videoInfo.channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = videoInfo.duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = videoInfo.viewCount,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionCard(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickActionsGrid(
    onClearDownloads: () -> Unit,
    onClearCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            QuickActionCard(
                icon = Icons.Rounded.Folder,
                label = "Downloads",
                onClick = { },
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        item {
            QuickActionCard(
                icon = Icons.Rounded.Settings,
                label = "Settings",
                onClick = { },
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        item {
            QuickActionCard(
                icon = Icons.Rounded.CleaningServices,
                label = "Clear Done",
                onClick = onClearCompleted,
                backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        item {
            QuickActionCard(
                icon = Icons.Rounded.DeleteSweep,
                label = "Clear All",
                onClick = onClearDownloads,
                backgroundColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DownloadFilters(
    selectedFilter: DownloadFilter,
    onFilterSelected: (DownloadFilter) -> Unit,
    downloadCounts: Map<DownloadFilter, Int>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(DownloadFilter.values()) { filter ->
            FilterChip(
                onClick = { onFilterSelected(filter) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(filter.displayName)
                        downloadCounts[filter]?.let { count ->
                            if (count > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedFilter == filter)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = count.toString(),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selectedFilter == filter)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                },
                selected = selectedFilter == filter,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun EnhancedDownloadsHeader(
    totalCount: Int,
    filter: DownloadFilter,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = when (filter) {
                    DownloadFilter.ALL -> "All Downloads"
                    else -> "${filter.displayName} Downloads"
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "$totalCount items",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { /* Sort options */ }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sort,
                    contentDescription = "Sort",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(
                onClick = { /* View options */ }
            ) {
                Icon(
                    imageVector = Icons.Rounded.ViewList,
                    contentDescription = "View options",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun EmptyDownloadsState(
    filter: DownloadFilter,
    modifier: Modifier = Modifier
) {
    val (icon, title, description) = when (filter) {
        DownloadFilter.ALL -> Triple(
            Icons.Rounded.VideoLibrary,
            "No downloads yet",
            "Start by pasting a YouTube URL above"
        )
        DownloadFilter.DOWNLOADING -> Triple(
            Icons.Rounded.CloudDownload,
            "No active downloads",
            "All downloads are completed or paused"
        )
        DownloadFilter.COMPLETED -> Triple(
            Icons.Rounded.CheckCircle,
            "No completed downloads",
            "Downloads will appear here when finished"
        )
        DownloadFilter.FAILED -> Triple(
            Icons.Rounded.Error,
            "No failed downloads",
            "Great! All your downloads were successful"
        )
        DownloadFilter.AUDIO -> Triple(
            Icons.Rounded.AudioFile,
            "No audio downloads",
            "Audio-only downloads will appear here"
        )
        DownloadFilter.VIDEO -> Triple(
            Icons.Rounded.VideoFile,
            "No video downloads",
            "Video downloads will appear here"
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedDownloadItemCard(
    download: DownloadItem,
    onPause: (String) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Thumbnail and Info
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Thumbnail
                    Box(
                        modifier = Modifier
                            .size(60.dp, 45.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = download.format.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Info Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = download.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (download.channelName.isNotEmpty()) {
                            Text(
                                text = download.channelName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = download.duration,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            if (download.fileSize.isNotEmpty()) {
                                Text(text = "•", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = download.fileSize,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Status and Menu
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EnhancedStatusChip(status = download.status)

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            when (download.status) {
                                DownloadStatus.DOWNLOADING -> {
                                    DropdownMenuItem(
                                        text = { Text("Pause") },
                                        onClick = {
                                            onPause(download.id)
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Pause, contentDescription = null)
                                        }
                                    )
                                }
                                DownloadStatus.PAUSED -> {
                                    DropdownMenuItem(
                                        text = { Text("Resume") },
                                        onClick = {
                                            onPause(download.id)
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        }
                                    )
                                }
                                DownloadStatus.FAILED -> {
                                    DropdownMenuItem(
                                        text = { Text("Retry") },
                                        onClick = {
                                            onRetry(download.id)
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Refresh, contentDescription = null)
                                        }
                                    )
                                }
                                else -> {}
                            }

                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    onDelete(download.id)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Progress Section
            AnimatedVisibility(
                visible = download.status in listOf(
                    DownloadStatus.DOWNLOADING,
                    DownloadStatus.PROCESSING
                ),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { download.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when (download.status) {
                            DownloadStatus.PROCESSING -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when (download.status) {
                                DownloadStatus.PROCESSING -> "Processing..."
                                else -> "${(download.progress * 100).toInt()}%"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (download.downloadSpeed.isNotEmpty()) {
                            Text(
                                text = download.downloadSpeed,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Quality and Format Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = download.quality.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = download.format.extension.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                if (download.viewCount.isNotEmpty()) {
                    Text(
                        text = download.viewCount,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedStatusChip(
    status: DownloadStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, contentColor, text, icon) = when (status) {
        DownloadStatus.COMPLETED -> StatusDisplayProperties(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Completed",
            Icons.Rounded.CheckCircle
        )
        DownloadStatus.DOWNLOADING -> StatusDisplayProperties(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Downloading",
            Icons.Rounded.CloudDownload
        )
        DownloadStatus.PROCESSING -> StatusDisplayProperties(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Processing",
            Icons.Rounded.Settings
        )
        DownloadStatus.PAUSED -> StatusDisplayProperties(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Paused",
            Icons.Rounded.Pause
        )
        DownloadStatus.QUEUED -> StatusDisplayProperties(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Queued",
            Icons.Rounded.Schedule
        )
        DownloadStatus.PENDING -> StatusDisplayProperties(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Pending",
            Icons.Rounded.HourglassEmpty
        )
        DownloadStatus.FAILED -> StatusDisplayProperties(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Failed",
            Icons.Rounded.Error
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = contentColor
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = contentColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySelectionSheet(
    selectedQuality: VideoQuality,
    availableQualities: List<VideoQuality>,
    onQualitySelected: (VideoQuality) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .width(32.dp)
                    .height(4.dp),
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Select Video Quality",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 20.dp)
            )

            availableQualities.forEach { quality ->
                QualityOptionCard(
                    quality = quality,
                    isSelected = quality == selectedQuality,
                    onClick = { onQualitySelected(quality) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelectionSheet(
    selectedFormat: DownloadFormat,
    availableFormats: List<DownloadFormat>,
    onFormatSelected: (DownloadFormat) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .width(32.dp)
                    .height(4.dp),
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Select Download Format",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 20.dp)
            )

            availableFormats.forEach { format ->
                FormatOptionCard(
                    format = format,
                    isSelected = format == selectedFormat,
                    onClick = { onFormatSelected(format) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun QualityOptionCard(
    quality: VideoQuality,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                Color.Transparent
        ),
        border = if (isSelected) null else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quality.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                )
                if (quality != VideoQuality.AUTO) {
                    Text(
                        text = quality.resolution,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            if (quality == VideoQuality.UHD4K || quality == VideoQuality.UHD2160) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Premium",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatOptionCard(
    format: DownloadFormat,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                Color.Transparent
        ),
        border = if (isSelected) null else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )

            Icon(
                imageVector = format.icon,
                contentDescription = null,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = format.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = ".${format.extension} file",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }

            // Show recommended badge for popular formats
            if (format == DownloadFormat.MP4 || format == DownloadFormat.MP3) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Popular",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

// Helper data class for destructuring (already exists in your code)
// data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// Additional Composables for Settings Screen (Optional Enhancement)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsHeader()
        }

        item {
            SettingsSection(
                title = "Download Settings",
                items = listOf(
                    SettingsItem.Switch(
                        title = "Auto-start downloads",
                        subtitle = "Start downloads immediately after adding",
                        checked = true,
                        onCheckedChange = { }
                    ),
                    SettingsItem.Option(
                        title = "Default quality",
                        subtitle = "Auto",
                        onClick = { }
                    ),
                    SettingsItem.Option(
                        title = "Download location",
                        subtitle = "/storage/emulated/0/Download/TubeFetch",
                        onClick = { }
                    )
                )
            )
        }

        item {
            SettingsSection(
                title = "App Settings",
                items = listOf(
                    SettingsItem.Switch(
                        title = "Dark mode",
                        subtitle = "Follow system setting",
                        checked = false,
                        onCheckedChange = { }
                    ),
                    SettingsItem.Option(
                        title = "Language",
                        subtitle = "English",
                        onClick = { }
                    ),
                    SettingsItem.Action(
                        title = "Clear cache",
                        subtitle = "Free up 45.2 MB",
                        onClick = { }
                    )
                )
            )
        }
    }
}

@Composable
private fun SettingsHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = "Customize your TubeFetch experience",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    items: List<SettingsItem>
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
            )

            items.forEachIndexed { index, item ->
                when (item) {
                    is SettingsItem.Switch -> {
                        SettingsSwitchItem(
                            title = item.title,
                            subtitle = item.subtitle,
                            checked = item.checked,
                            onCheckedChange = item.onCheckedChange
                        )
                    }
                    is SettingsItem.Option -> {
                        SettingsOptionItem(
                            title = item.title,
                            subtitle = item.subtitle,
                            onClick = item.onClick
                        )
                    }
                    is SettingsItem.Action -> {
                        SettingsActionItem(
                            title = item.title,
                            subtitle = item.subtitle,
                            onClick = item.onClick
                        )
                    }
                }

                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsOptionItem(
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsActionItem(
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

sealed class SettingsItem {
    data class Switch(
        val title: String,
        val subtitle: String?,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit
    ) : SettingsItem()

    data class Option(
        val title: String,
        val subtitle: String?,
        val onClick: () -> Unit
    ) : SettingsItem()

    data class Action(
        val title: String,
        val subtitle: String?,
        val onClick: () -> Unit
    ) : SettingsItem()
}

data class StatusDisplayProperties(
    val backgroundColor: Color,
    val contentColor: Color,
    val text: String,
    val icon: ImageVector
)