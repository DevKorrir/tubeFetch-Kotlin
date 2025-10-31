package dev.korryr.tubefetch.ui.features.home.uiElements

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.korryr.tubefetch.data.remote.DownloadFilter

@Composable
fun EmptyDownloadsState(
    filter: DownloadFilter,
    modifier: Modifier = Modifier
) {
    // TODO: Implement EmptyDownloadsState
    Text("Empty State - To be implemented", modifier = modifier)
}