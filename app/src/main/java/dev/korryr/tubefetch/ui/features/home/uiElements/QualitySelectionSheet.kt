package dev.korryr.tubefetch.ui.features.home.uiElements

import androidx.compose.runtime.Composable

@Composable
fun QualitySelectionSheet(
    selectedQuality: dev.korryr.tubefetch.domain.model.VideoQuality,
    availableQualities: List<dev.korryr.tubefetch.domain.model.VideoQuality>,
    onQualitySelected: (dev.korryr.tubefetch.domain.model.VideoQuality) -> Unit,
    onDismiss: () -> Unit
) {
    // TODO: Implement QualitySelectionSheet
}