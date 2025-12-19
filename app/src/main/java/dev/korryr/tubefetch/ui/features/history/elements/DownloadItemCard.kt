package dev.korryr.tubefetch.ui.features.history.elements

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.korryr.tubefetch.domain.model.DownloadItem
import dev.korryr.tubefetch.domain.model.DownloadStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadItemCard(
    download: DownloadItem,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    ElevatedCard(
        onClick = {
            isPressed = true
            showMenu = true
        },
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 6.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row with Thumbnail and Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cute Thumbnail with Icon
                Box(
                    modifier = Modifier
                        .size(80.dp, 60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.tertiaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = download.format.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Info Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = download.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (download.channelName.isNotEmpty()) {
                        Text(
                            text = download.channelName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when (download.status) {
                                DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                                DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.secondaryContainer
                                DownloadStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                                DownloadStatus.PAUSED -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (download.status) {
                            DownloadStatus.COMPLETED -> Icons.Rounded.CheckCircle
                            DownloadStatus.DOWNLOADING -> Icons.Rounded.Downloading
                            DownloadStatus.FAILED -> Icons.Rounded.Error
                            DownloadStatus.PAUSED -> Icons.Rounded.Pause
                            else -> Icons.Rounded.HourglassEmpty
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = when (download.status) {
                            DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.secondary
                            DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Progress Section
            AnimatedVisibility(
                visible = download.status in listOf(
                    DownloadStatus.QUEUED,
                    DownloadStatus.DOWNLOADING,
                    DownloadStatus.PROCESSING,
                    DownloadStatus.PAUSED
                ),
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { download.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when (download.status) {
                            DownloadStatus.PROCESSING -> MaterialTheme.colorScheme.tertiary
                            DownloadStatus.PAUSED -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (download.status) {
                                    DownloadStatus.QUEUED -> "⏳ Queued..."
                                    DownloadStatus.PROCESSING -> "Processing..."
                                    DownloadStatus.PAUSED -> "⏸️ Paused"
                                    else -> "${(download.progress * 100).toInt()}%"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = when (download.status) {
                                    DownloadStatus.PAUSED -> MaterialTheme.colorScheme.outline
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )

                            if (download.downloadSpeed.isNotEmpty() && download.status != DownloadStatus.PAUSED) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = download.downloadSpeed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quality Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = download.quality.displayName,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    // Format Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = download.format.extension.uppercase(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Duration and Size
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (download.duration.isNotEmpty()) {
                        Text(
                            text = "⏱️ ${download.duration}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    if (download.fileSize.isNotEmpty()) {
                        Text(
                            text = "📦 ${download.fileSize}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Dropdown Menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            when (download.status) {
                DownloadStatus.DOWNLOADING -> {
                    DropdownMenuItem(
                        text = { Text("⏸️ Pause Download") },
                        onClick = {
                            onPause(download.id)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Pause, contentDescription = null)
                        }
                    )
                }

                DownloadStatus.PAUSED -> {
                    DropdownMenuItem(
                        text = { Text("▶️ Resume Download") },
                        onClick = {
                            onResume(download.id)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        }
                    )
                }

                DownloadStatus.FAILED -> {
                    DropdownMenuItem(
                        text = { Text("🔄 Retry Download") },
                        onClick = {
                            onRetry(download.id)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                        }
                    )
                }

                else -> {}
            }

            DropdownMenuItem(
                text = { Text("🗑️ Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    onDelete(download.id)
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(150)
            isPressed = false
        }
    }
}