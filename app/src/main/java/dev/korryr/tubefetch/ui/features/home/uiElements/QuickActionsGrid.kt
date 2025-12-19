package dev.korryr.tubefetch.ui.features.home.uiElements

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QuickActionsGrid(
    onClearDownloads: () -> Unit,
    onClearCompleted: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.List,
                    label = "View All",
                    onClick = onNavigateToDownloads,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.DoneAll,
                    label = "Clear Done",
                    onClick = onClearCompleted,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.DeleteForever,
                    label = "Clear All",
                    onClick = onClearDownloads,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}