package dev.korryr.tubefetch.ui.features.home.view

// TubeFetch - Modern YouTube Downloader UI
// Built with Jetpack Compose, Material 3 Design System

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TubeFetchHomeScreen(
    modifier: Modifier = Modifier
) {
    var urlText by remember { mutableStateOf("") }
    var searchText by remember { mutableStateOf("") }
    var selectedQuality by remember { mutableStateOf("Auto") }
    var downloadType by remember { mutableStateOf(DownloadType.MP4) }
    var isDownloading by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = downloadProgress,
        animationSpec = tween(1000)
    )

    // Animate download progress for demo
    LaunchedEffect(isDownloading) {
        if (isDownloading) {
            for (i in 0..100 step 5) {
                downloadProgress = i / 100f
                kotlinx.coroutines.delay(150)
            }
            isDownloading = false
            downloadProgress = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Custom Top App Bar
            TubeFetchTopBar()

            Spacer(modifier = Modifier.height(32.dp))

            // Welcome Section
            WelcomeSection()

            Spacer(modifier = Modifier.height(32.dp))

            // Search Section
            SearchSection(
                searchText = searchText,
                onSearchTextChange = { searchText = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // URL Input Section
            UrlInputSection(
                urlText = urlText,
                onUrlTextChange = { urlText = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Download Options
            DownloadOptionsSection(
                selectedQuality = selectedQuality,
                downloadType = downloadType,
                onQualityClick = { showQualitySheet = true },
                onDownloadTypeChange = { downloadType = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Download Button
            DownloadButton(
                isDownloading = isDownloading,
                animatedProgress = animatedProgress,
                onClick = {
                    if (urlText.isNotEmpty()) {
                        isDownloading = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Features
            QuickFeaturesSection()

            Spacer(modifier = Modifier.weight(1f))
        }

        // Quality Selection Bottom Sheet
        if (showQualitySheet) {
            QualitySelectionSheet(
                selectedQuality = selectedQuality,
                onQualitySelected = { quality ->
                    selectedQuality = quality
                    showQualitySheet = false
                },
                onDismiss = { showQualitySheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TubeFetchTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.GetApp,
                    contentDescription = "TubeFetch",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "TubeFetch",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "Smart Video Downloader",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }

        Row {
            IconButton(
                onClick = { /* Settings */ },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun WelcomeSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Download Anything! 🚀",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "High quality videos & audio from YouTube",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSection(
    searchText: String,
    onSearchTextChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            placeholder = {
                Text(
                    text = "Search for videos...",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = if (searchText.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchTextChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlInputSection(
    urlText: String,
    onUrlTextChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        OutlinedTextField(
            value = urlText,
            onValueChange = onUrlTextChange,
            placeholder = {
                Text(
                    text = "Paste YouTube URL here...",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = "URL",
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = { /* Paste from clipboard */ }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentPaste,
                        contentDescription = "Paste",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
    }
}

@Composable
fun DownloadOptionsSection(
    selectedQuality: String,
    downloadType: DownloadType,
    onQualityClick: () -> Unit,
    onDownloadTypeChange: (DownloadType) -> Unit
) {
    Column {
        Text(
            text = "Download Options",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Download Type Selection
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Format",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row {
                        FilterChip(
                            onClick = { onDownloadTypeChange(DownloadType.MP4) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Videocam,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("MP4")
                                }
                            },
                            selected = downloadType == DownloadType.MP4,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        FilterChip(
                            onClick = { onDownloadTypeChange(DownloadType.MP3) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.AudioFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("MP3")
                                }
                            },
                            selected = downloadType == DownloadType.MP3
                        )
                    }
                }
            }

            // Quality Selection
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onQualityClick() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Quality",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HighQuality,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedQuality,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadButton(
    isDownloading: Boolean,
    animatedProgress: Float,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = !isDownloading) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isDownloading)
                MaterialTheme.colorScheme.secondary
            else
                MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDownloading) 8.dp else 4.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Progress background
            if (isDownloading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                        .align(Alignment.CenterStart)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onSecondary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Downloading... ${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Start Download",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun QuickFeaturesSection() {
    val features = listOf(
        QuickFeature(
            icon = Icons.Rounded.Speed,
            title = "Ultra Fast",
            description = "Lightning speed downloads",
            color = MaterialTheme.colorScheme.primary
        ),
        QuickFeature(
            icon = Icons.Rounded.Security,
            title = "Secure",
            description = "Safe & private downloads",
            color = MaterialTheme.colorScheme.secondary
        ),
        QuickFeature(
            icon = Icons.Rounded.Star,
            title = "Premium",
            description = "Best quality guaranteed",
            color = MaterialTheme.colorScheme.tertiary
        )
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(features) { feature ->
            QuickFeatureCard(feature = feature)
        }
    }
}

@Composable
fun QuickFeatureCard(feature: QuickFeature) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = feature.color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(feature.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = feature.color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySelectionSheet(
    selectedQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val qualities = listOf("Auto", "360p", "720p", "1080p", "2K")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            )
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

            qualities.forEach { quality ->
                QualityOptionItem(
                    quality = quality,
                    isSelected = quality == selectedQuality,
                    onClick = { onQualitySelected(quality) }
                )
            }
        }
    }
}

@Composable
fun QualityOptionItem(
    quality: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = quality,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                )
                if (quality != "Auto") {
                    Text(
                        text = getQualityDescription(quality),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    }
}

// Data Classes and Enums
enum class DownloadType {
    MP4, MP3
}

data class QuickFeature(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

fun getQualityDescription(quality: String): String {
    return when (quality) {
        "360p" -> "Standard quality, smaller file size"
        "720p" -> "HD quality, balanced size and quality"
        "1080p" -> "Full HD quality, larger file size"
        "2K" -> "Ultra HD quality, premium experience"
        else -> "Best available quality"
    }
}