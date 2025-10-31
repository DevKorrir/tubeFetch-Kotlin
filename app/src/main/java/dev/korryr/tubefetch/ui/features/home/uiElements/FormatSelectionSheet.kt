package dev.korryr.tubefetch.ui.features.home.uiElements

import androidx.compose.runtime.Composable
import dev.korryr.tubefetch.domain.model.DownloadFormat

@Composable
fun FormatSelectionSheet(
    selectedFormat: DownloadFormat,
    availableFormats: List<DownloadFormat>,
    onFormatSelected: (DownloadFormat) -> Unit,
    onDismiss: () -> Unit
) {
    // TODO: Implement FormatSelectionSheet
}