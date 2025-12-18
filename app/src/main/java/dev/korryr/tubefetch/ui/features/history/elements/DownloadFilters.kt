package dev.korryr.tubefetch.ui.features.history.elements

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.korryr.tubefetch.data.remote.DownloadFilter

@Composable
fun DownloadFilters(
    selectedFilter: DownloadFilter,
    onFilterSelected: (DownloadFilter) -> Unit,
    downloadCounts: Map<DownloadFilter, Int>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(DownloadFilter.values()) { filter ->
            CuteFilterChip(
                filter = filter,
                isSelected = selectedFilter == filter,
                count = downloadCounts[filter] ?: 0,
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}

@Composable
private fun CuteFilterChip(
    filter: DownloadFilter,
    isSelected: Boolean,
    count: Int,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            isSelected -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(300),
        label = "container_color"
    )

    ElevatedCard(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier.scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (filter) {
                    DownloadFilter.ALL -> "All"
                    DownloadFilter.DOWNLOADING -> "Active"
                    DownloadFilter.COMPLETED -> "Done"
                    DownloadFilter.FAILED -> "Failed"
                    DownloadFilter.AUDIO -> "Audio"
                    DownloadFilter.VIDEO -> "Video"
                },
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (count > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = count.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(150)
            isPressed = false
        }
    }
}