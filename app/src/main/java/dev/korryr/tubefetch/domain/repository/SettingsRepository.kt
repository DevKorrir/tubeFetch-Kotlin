package dev.korryr.tubefetch.domain.repository

import dev.korryr.tubefetch.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
