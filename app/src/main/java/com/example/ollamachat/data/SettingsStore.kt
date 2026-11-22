package com.example.ollamachat.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ollama_settings")

class SettingsStore(context: Context) {
    private val ds = context.applicationContext.dataStore

    val baseUrl: Flow<String> = ds.data.map { it[KEY_URL] ?: UrlUtils.DEFAULT_BASE_URL }
    val selectedModel: Flow<String?> = ds.data.map { it[KEY_MODEL] }
    val systemPrompt: Flow<String> = ds.data.map { it[KEY_SYSTEM] ?: "" }
    val temperature: Flow<Float> = ds.data.map { it[KEY_TEMP] ?: 0.7f }
    val ttsServerUrl: Flow<String> = ds.data.map { it[KEY_TTS_URL] ?: DEFAULT_TTS_URL }
    val voiceCloneEnabled: Flow<Boolean> = ds.data.map { it[KEY_VOICE_CLONE] ?: false }
    val voiceSample: Flow<String> = ds.data.map { it[KEY_VOICE_SAMPLE] ?: "default" }

    suspend fun setBaseUrl(value: String) {
        ds.edit { it[KEY_URL] = value }
    }

    suspend fun setSelectedModel(value: String?) {
        ds.edit { if (value == null) it.remove(KEY_MODEL) else it[KEY_MODEL] = value }
    }

    suspend fun setSystemPrompt(value: String) {
        ds.edit { it[KEY_SYSTEM] = value }
    }

    suspend fun setTemperature(value: Float) {
        ds.edit { it[KEY_TEMP] = value }
    }

    suspend fun setTtsServerUrl(value: String) {
        ds.edit { it[KEY_TTS_URL] = value }
    }

    suspend fun setVoiceCloneEnabled(enabled: Boolean) {
        ds.edit { it[KEY_VOICE_CLONE] = enabled }
    }

    suspend fun setVoiceSample(value: String) {
        ds.edit { it[KEY_VOICE_SAMPLE] = value }
    }

    private companion object {
        const val DEFAULT_TTS_URL = "http://127.0.0.1:8020"

        val KEY_URL = stringPreferencesKey("base_url")
        val KEY_MODEL = stringPreferencesKey("selected_model")
        val KEY_SYSTEM = stringPreferencesKey("system_prompt")
        val KEY_TEMP = floatPreferencesKey("temperature")
        val KEY_TTS_URL = stringPreferencesKey("tts_server_url")
        val KEY_VOICE_CLONE = booleanPreferencesKey("voice_clone_enabled")
        val KEY_VOICE_SAMPLE = stringPreferencesKey("voice_sample")
    }
}
