package dev.korryr.tubefetch.ui.features.history.elements

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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.korryr.tubefetch.data.remote.DownloadFilter

@Composable
fun DownloadsHeader(
    totalCount: Int,
    filter: DownloadFilter,
    modifier: Modifier = Modifier
) {
    // Subtle rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when (filter) {
                            DownloadFilter.ALL -> "My Downloads"
                            DownloadFilter.DOWNLOADING -> "Active Downloads"
                            DownloadFilter.COMPLETED -> "Completed"
                            DownloadFilter.FAILED -> "Failed Downloads"
                            DownloadFilter.AUDIO -> "Audio Files 🎵"
                            DownloadFilter.VIDEO -> "Video Files"
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "$totalCount ${if (totalCount == 1) "item" else "items"}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = if (totalCount == 0) "Nothing here yet" else "Ready to view",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // Animated Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (filter) {
                            DownloadFilter.DOWNLOADING -> Icons.Rounded.Downloading
                            DownloadFilter.COMPLETED -> Icons.Rounded.CheckCircle
                            DownloadFilter.FAILED -> Icons.Rounded.Error
                            DownloadFilter.AUDIO -> Icons.Rounded.MusicNote
                            DownloadFilter.VIDEO -> Icons.Rounded.VideoLibrary
                            else -> Icons.Rounded.FileDownload
                        },
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .rotate(if (filter == DownloadFilter.DOWNLOADING) rotation * 0.1f else 0f),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}