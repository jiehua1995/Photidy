package app.photidy.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.photidy.core.model.LibrarySnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

internal val Context.libraryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "photidy_library",
)

/**
 * Persists review state and the active tidy session as a single JSON snapshot.
 * A single atomic document keeps the demo state consistent across undo, language
 * changes and process death without introducing a database for phase one.
 */
class LibraryStore(context: Context) {

    private val dataStore = context.applicationContext.libraryDataStore
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val snapshotKey = stringPreferencesKey("library_snapshot")

    val snapshot: Flow<LibrarySnapshot> = dataStore.data.map { preferences ->
        decode(preferences[snapshotKey])
    }

    suspend fun update(transform: (LibrarySnapshot) -> LibrarySnapshot) {
        dataStore.edit { preferences ->
            val current = decode(preferences[snapshotKey])
            preferences[snapshotKey] = json.encodeToString(transform(current))
        }
    }

    private fun decode(raw: String?): LibrarySnapshot {
        if (raw.isNullOrBlank()) return LibrarySnapshot()
        return runCatching { json.decodeFromString<LibrarySnapshot>(raw) }
            .getOrElse { LibrarySnapshot() }
    }
}
