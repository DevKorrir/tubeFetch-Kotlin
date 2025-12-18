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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.korryr.tubefetch.data.remote.DownloadFilter

@Composable
fun EmptyDownloadsState(
    filter: DownloadFilter,
    modifier: Modifier = Modifier
) {
    // Cute bounce animation
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val (icon, title, subtitle) = when (filter) {
        DownloadFilter.ALL -> Triple(
            Icons.Rounded.CloudDownload,
            "No Downloads Yet!",
            "Start downloading your favorite videos and they'll appear here"
        )
        DownloadFilter.DOWNLOADING -> Triple(
            Icons.Rounded.Downloading,
            "No Active Downloads ⚡",
            "All your downloads are complete or paused"
        )
        DownloadFilter.COMPLETED -> Triple(
            Icons.Rounded.CheckCircle,
            "No Completed Downloads ✨",
            "Downloads that finish successfully will appear here"
        )
        DownloadFilter.FAILED -> Triple(
            Icons.Rounded.Error,
            "No Failed Downloads",
            "That's great! All your downloads are working perfectly"
        )
        DownloadFilter.AUDIO -> Triple(
            Icons.Rounded.MusicNote,
            "No Audio Files 🎵",
            "Download some music or audio to see them here"
        )
        DownloadFilter.VIDEO -> Triple(
            Icons.Rounded.VideoLibrary,
            "No Video Files 🎬",
            "Download videos to build your collection"
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Cute animated icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 300.dp)
                )
            }

            // Cute decorative elements
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.3f - (index * 0.1f)
                                )
                            )
                    )
                }
            }
        }
    }
}