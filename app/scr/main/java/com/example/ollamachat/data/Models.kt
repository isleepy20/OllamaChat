package com.example.ollamachat.data

data class ChatMessage(
    val id: Long,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList(),
    val model: String? = null
)

data class ChatChunk(val content: String, val done: Boolean)

data class PullProgress(val status: String, val completed: Long, val total: Long) {
    val fraction: Float?
        get() = if (total > 0) (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else null
}

class OllamaException(message: String) : Exception(message)
