package dev.korryr.tubefetch.ui.features.home.uiElements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun QuickActionsGrid(
    onClearDownloads: () -> Unit,
    onClearCompleted: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(
            icon = Icons.Default.List,
            label = "View All",
            onClick = onNavigateToDownloads
        )
        QuickActionButton(
            icon = Icons.Default.DeleteForever,
            label = "Clear All",
            onClick = onClearDownloads
        )
        QuickActionButton(
            icon = Icons.Default.DoneAll,
            label = "Clear Done",
            onClick = onClearCompleted
        )
    }
}
