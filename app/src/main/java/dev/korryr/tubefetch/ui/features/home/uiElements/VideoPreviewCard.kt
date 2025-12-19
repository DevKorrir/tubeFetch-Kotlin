package dev.korryr.tubefetch.ui.features.home.uiElements

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.korryr.tubefetch.domain.model.VideoInfo

@Composable
fun VideoPreviewCard(
    videoInfo: VideoInfo,
    modifier: Modifier = Modifier
) {
    // Cute pulsing animation for play icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cute Thumbnail with overlay
            Box(
                modifier = Modifier
                    .size(100.dp, 75.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                // Thumbnail image
                AsyncImage(
                    model = videoInfo.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                // Gradient overlay for better icon visibility
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                                )
                            )
                        )
                )

                // Animated play icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Video Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(75.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title
                Text(
                    text = videoInfo.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Channel name with icon
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = videoInfo.channelName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Duration and views with cute icons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Duration
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = videoInfo.duration,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }

                        // View count
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RemoveRedEye,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = videoInfo.viewCount,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}