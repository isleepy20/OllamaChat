package com.example.ollamachat.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ollama_settings")

/** Persists the server URL, selected model and system prompt locally. Nothing else is stored. */
class SettingsStore(context: Context) {
    private val ds = context.applicationContext.dataStore

    val baseUrl: Flow<String> = ds.data.map { it[KEY_URL] ?: UrlUtils.DEFAULT_BASE_URL }
    val selectedModel: Flow<String?> = ds.data.map { it[KEY_MODEL] }
    val systemPrompt: Flow<String> = ds.data.map { it[KEY_SYSTEM] ?: "" }

    suspend fun setBaseUrl(value: String) {
        ds.edit { it[KEY_URL] = value }
    }

    suspend fun setSelectedModel(value: String?) {
        ds.edit { if (value == null) it.remove(KEY_MODEL) else it[KEY_MODEL] = value }
    }

    suspend fun setSystemPrompt(value: String) {
        ds.edit { it[KEY_SYSTEM] = value }
    }

    private companion object {
        val KEY_URL = stringPreferencesKey("base_url")
        val KEY_MODEL = stringPreferencesKey("selected_model")
        val KEY_SYSTEM = stringPreferencesKey("system_prompt")
    }
}
