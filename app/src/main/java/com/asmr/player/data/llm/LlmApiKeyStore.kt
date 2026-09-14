package com.asmr.player.data.llm

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.asmr.player.data.settings.settingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the LLM API key locally (app-private DataStore).
 * Ported from LizuNemuri `lib/data/repositories/llm_api_key_repository.dart`.
 * No key is bundled in the repo — user supplies it in Settings.
 */
@Singleton
class LlmApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val mutex = Mutex()
    private var cached: String? = null
    private var loaded = false

    suspend fun getApiKey(): String? {
        if (loaded) return cached
        return mutex.withLock {
            if (loaded) return cached
            val value = context.settingsDataStore.data.first()[KEY]
            cached = value
            loaded = true
            value
        }
    }

    suspend fun saveApiKey(key: String) {
        mutex.withLock {
            cached = key
            loaded = true
            context.settingsDataStore.edit { prefs ->
                prefs[KEY] = key
            }
        }
    }

    suspend fun clearApiKey() {
        mutex.withLock {
            cached = null
            loaded = true
            context.settingsDataStore.edit { prefs ->
                prefs.remove(KEY)
            }
        }
    }

    companion object {
        private val KEY = stringPreferencesKey("llm_api_key")
    }
}
