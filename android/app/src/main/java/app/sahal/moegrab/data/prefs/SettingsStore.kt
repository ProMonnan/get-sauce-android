package app.sahal.moegrab.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * User-adjustable settings. Backed by DataStore Preferences (simpler than Proto
 * for this many fields; migrate later if the surface grows). Every value has a
 * safe default so the app is usable on first launch without opening Settings.
 */
class SettingsStore(private val ctx: Context) {

    data class Snapshot(
        val outputTreeUri: String? = null,
        val workers: Int = 4,
        val timeoutMinutes: Int = 15,
        val proxy: String = "",
        val userHeaders: String = "",
        val truncate: Boolean = false,
    )

    val flow: Flow<Snapshot> = ctx.dataStore.data.map { p ->
        Snapshot(
            outputTreeUri = p[K_OUTPUT_TREE],
            workers = p[K_WORKERS] ?: 4,
            timeoutMinutes = p[K_TIMEOUT] ?: 15,
            proxy = p[K_PROXY].orEmpty(),
            userHeaders = p[K_HEADERS].orEmpty(),
            truncate = p[K_TRUNCATE] ?: false,
        )
    }

    suspend fun setOutputTree(uri: String?) = ctx.dataStore.edit {
        if (uri.isNullOrBlank()) it.remove(K_OUTPUT_TREE) else it[K_OUTPUT_TREE] = uri
    }
    suspend fun setWorkers(n: Int) = ctx.dataStore.edit { it[K_WORKERS] = n.coerceIn(1, 16) }
    suspend fun setTimeout(n: Int) = ctx.dataStore.edit { it[K_TIMEOUT] = n.coerceIn(1, 120) }
    suspend fun setProxy(v: String) = ctx.dataStore.edit { it[K_PROXY] = v.trim() }
    suspend fun setUserHeaders(v: String) = ctx.dataStore.edit { it[K_HEADERS] = v }
    suspend fun setTruncate(v: Boolean) = ctx.dataStore.edit { it[K_TRUNCATE] = v }

    private companion object {
        val K_OUTPUT_TREE: Preferences.Key<String> = stringPreferencesKey("output_tree_uri")
        val K_WORKERS: Preferences.Key<Int> = intPreferencesKey("workers")
        val K_TIMEOUT: Preferences.Key<Int> = intPreferencesKey("timeout_min")
        val K_PROXY: Preferences.Key<String> = stringPreferencesKey("proxy")
        val K_HEADERS: Preferences.Key<String> = stringPreferencesKey("user_headers")
        val K_TRUNCATE: Preferences.Key<Boolean> = booleanPreferencesKey("truncate")
    }
}
