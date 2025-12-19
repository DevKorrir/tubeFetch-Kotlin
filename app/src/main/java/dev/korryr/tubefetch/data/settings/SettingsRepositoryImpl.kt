package dev.korryr.tubefetch.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.korryr.tubefetch.domain.model.ThemeMode
import dev.korryr.tubefetch.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")

    override val themeMode: Flow<ThemeMode> =
        context.settingsDataStore.data.map { prefs ->
            val name = prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
            runCatching { ThemeMode.valueOf(name) }.getOrElse { ThemeMode.SYSTEM }
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }
}
