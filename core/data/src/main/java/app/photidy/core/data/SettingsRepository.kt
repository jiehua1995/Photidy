package app.photidy.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.photidy.core.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

internal val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "photidy_settings",
)

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val settingsKey = stringPreferencesKey("app_settings")

    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        preferences[settingsKey]?.let { raw ->
            runCatching { json.decodeFromString<AppSettings>(raw) }.getOrNull()
        } ?: AppSettings()
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            val current = preferences[settingsKey]?.let { raw ->
                runCatching { json.decodeFromString<AppSettings>(raw) }.getOrNull()
            } ?: AppSettings()
            preferences[settingsKey] = json.encodeToString(transform(current))
        }
    }
}
