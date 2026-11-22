package com.example.ollamachat.data

data class ChatMessage(val id: Long, val role: String, val content: String)

data class ChatChunk(val content: String, val done: Boolean)

data class PullProgress(val status: String, val completed: Long, val total: Long) {
    val fraction: Float?
        get() = if (total > 0) (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else null
}

/** An error with a message that is safe to show directly to the user. */
class OllamaException(message: String) : Exception(message)
