package com.example.ollamachat.ui

import android.content.Context
import android.media.MediaPlayer
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject

data class LockedVoiceProfile(
    val id: String,
    val name: String,
    val refAudioName: String,
    val speechRate: Float = 0.95f,
    val speechPitch: Float = 1.0f,
    val locked: Boolean = true
)

class LockedVoiceManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("locked_voice_prefs", Context.MODE_PRIVATE)
    private val cacheDir = File(context.cacheDir, "locked_voice_cache").apply { mkdirs() }

    fun saveAndLockProfile(profile: LockedVoiceProfile) {
        val lockedProfile = profile.copy(locked = true)
        val json = JSONObject().apply {
            put("id", lockedProfile.id)
            put("name", lockedProfile.name)
            put("ref_audio_name", lockedProfile.refAudioName)
            put("speech_rate", lockedProfile.speechRate.toDouble())
            put("speech_pitch", lockedProfile.speechPitch.toDouble())
            put("locked", lockedProfile.locked)
        }
        prefs.edit().putString("active_locked_profile", json.toString()).apply()
    }

    fun getLockedProfile(): LockedVoiceProfile? {
        val raw = prefs.getString("active_locked_profile", null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            LockedVoiceProfile(
                id = obj.optString("id", "default_locked"),
                name = obj.optString("name", "Locked Voice"),
                refAudioName = obj.optString("ref_audio_name", ""),
                speechRate = obj.optDouble("speech_rate", 0.95).toFloat().coerceIn(0.6f, 1.6f),
                speechPitch = obj.optDouble("speech_pitch", 1.0).toFloat().coerceIn(0.75f, 1.3f),
                locked = obj.optBoolean("locked", true)
            )
        }.getOrNull()
    }

    fun cachedAudioFile(profileId: String, text: String): File {
        val key = sha256("$profileId::${text.trim()}")
        return File(cacheDir, "voice_${key.take(16)}.wav")
    }

    fun playWavFile(
        player: MediaPlayer,
        wavFile: File,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!wavFile.exists() || wavFile.length() == 0L) {
            onError("Cached audio file is missing or empty.")
            return
        }
        runCatching {
            if (player.isPlaying) player.stop()
            player.reset()
            player.setDataSource(wavFile.absolutePath)
            player.setOnCompletionListener { onComplete() }
            player.setOnErrorListener { _, _, _ ->
                onError("MediaPlayer error while playing cached voice.")
                true
            }
            player.prepare()
            player.start()
        }.onFailure { err ->
            onError(err.message ?: "Unable to play audio.")
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
