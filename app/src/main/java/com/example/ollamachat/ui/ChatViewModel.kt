package com.example.ollamachat.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ollamachat.data.ChatMessage
import com.example.ollamachat.data.ChatSession
import com.example.ollamachat.data.ChatSessionManager
import com.example.ollamachat.data.OllamaApi
import com.example.ollamachat.data.SettingsStore
import com.example.ollamachat.data.UrlUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class PullUiState(
    val active: Boolean = false,
    val model: String = "",
    val status: String = "",
    val fraction: Float? = null,
    val error: String? = null
)

data class UiState(
    val baseUrl: String = UrlUtils.DEFAULT_BASE_URL,
    val systemPrompt: String = "",
    val temperature: Float = 0.7f,
    val models: List<String> = emptyList(),
    val selectedModel: String? = null,
    val loadingModels: Boolean = false,
    val connectionError: String? = null,
    val sessions: List<ChatSession> = emptyList(),
    val currentSessionId: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val streaming: Boolean = false,
    val chatError: String? = null,
    val pull: PullUiState = PullUiState(),
    val settingsOpen: Boolean = false,
    val drawerOpen: Boolean = false
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SettingsStore(app)
    private val sessionManager = ChatSessionManager(app)
    private val api = OllamaApi()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private var chatJob: Job? = null
    private var pullJob: Job? = null
    private var nextId = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            val url = store.baseUrl.first()
            val model = store.selectedModel.first()
            val prompt = store.systemPrompt.first()
            val temp = store.temperature.first()

            val savedSessions = sessionManager.loadSessions()
            val activeSession = savedSessions.firstOrNull() ?: createInitialSession()
            val allSessions = if (savedSessions.isEmpty()) listOf(activeSession) else savedSessions

            _state.update {
                it.copy(
                    baseUrl = url,
                    selectedModel = model,
                    systemPrompt = prompt,
                    temperature = temp,
                    sessions = allSessions,
                    currentSessionId = activeSession.id,
                    messages = activeSession.messages
                )
            }
            refreshModels()
        }
    }

    private fun createInitialSession(): ChatSession {
        val session = ChatSession(
            id = UUID.randomUUID().toString(),
            title = "New Chat",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        sessionManager.saveSessions(listOf(session))
        return session
    }

    // ---- Multi-Session Conversation Management ----

    fun newChat() {
        chatJob?.cancel()
        val newSession = ChatSession(
            id = UUID.randomUUID().toString(),
            title = "New Chat",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            model = _state.value.selectedModel
        )
        val updated = listOf(newSession) + _state.value.sessions
        _state.update {
            it.copy(
                sessions = updated,
                currentSessionId = newSession.id,
                messages = emptyList(),
                chatError = null,
                drawerOpen = false
            )
        }
        sessionManager.saveSessions(updated)
    }

    fun selectSession(sessionId: String) {
        if (sessionId == _state.value.currentSessionId) {
            _state.update { it.copy(drawerOpen = false) }
            return
        }
        chatJob?.cancel()
        val target = _state.value.sessions.firstOrNull { it.id == sessionId } ?: return
        _state.update {
            it.copy(
                currentSessionId = target.id,
                messages = target.messages,
                selectedModel = target.model ?: it.selectedModel,
                chatError = null,
                drawerOpen = false
            )
        }
    }

    fun deleteSession(sessionId: String) {
        val remaining = _state.value.sessions.filterNot { it.id == sessionId }
        val finalSessions = if (remaining.isEmpty()) listOf(createInitialSession()) else remaining
        val current = if (sessionId == _state.value.currentSessionId) finalSessions.first() else null

        _state.update {
            it.copy(
                sessions = finalSessions,
                currentSessionId = current?.id ?: it.currentSessionId,
                messages = current?.messages ?: it.messages
            )
        }
        sessionManager.saveSessions(finalSessions)
    }

    fun openDrawer() = _state.update { it.copy(drawerOpen = true) }
    fun closeDrawer() = _state.update { it.copy(drawerOpen = false) }

    // ---- Settings ----

    fun openSettings() = _state.update { it.copy(settingsOpen = true) }
    fun closeSettings() = _state.update { it.copy(settingsOpen = false) }

    fun saveBaseUrl(raw: String): String? {
        val normalized = UrlUtils.normalizeBaseUrl(raw)
            ?: return "Enter a valid address, e.g. http://10.0.0.134:11434"
        _state.update { it.copy(baseUrl = normalized) }
        viewModelScope.launch { store.setBaseUrl(normalized) }
        refreshModels()
        return null
    }

    fun saveSystemPrompt(text: String) {
        _state.update { it.copy(systemPrompt = text) }
        viewModelScope.launch { store.setSystemPrompt(text) }
    }

    fun setTemperature(temp: Float) {
        _state.update { it.copy(temperature = temp) }
        viewModelScope.launch { store.setTemperature(temp) }
    }

    // ---- Model list: GET /api/tags ----

    fun refreshModels(selectAfter: String? = null) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(loadingModels = true, connectionError = null) }
            try {
                val list = api.listModels(_state.value.baseUrl)
                val current = _state.value.selectedModel
                val chosen = resolveInstalled(list, selectAfter)
                    ?: current?.takeIf { it in list }
                    ?: list.firstOrNull()
                _state.update {
                    it.copy(
                        models = list,
                        selectedModel = chosen,
                        loadingModels = false,
                        connectionError = if (list.isEmpty()) {
                            "Connected to Ollama, but no models installed. Download one in Settings."
                        } else null
                    )
                }
                store.setSelectedModel(chosen)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loadingModels = false,
                        connectionError = e.message ?: "Could not connect to Ollama server."
                    )
                }
            }
        }
    }

    private fun resolveInstalled(list: List<String>, requested: String?): String? {
        if (requested.isNullOrBlank()) return null
        return list.firstOrNull { it == requested }
            ?: list.firstOrNull { it == "$requested:latest" }
    }

    fun selectModel(name: String) {
        _state.update { it.copy(selectedModel = name) }
        viewModelScope.launch { store.setSelectedModel(name) }
        persistCurrentSession()
    }

    // ---- Pull Model ----

    fun pullModel(raw: String) {
        val name = raw.trim()
        if (name.isEmpty()) {
            _state.update {
                it.copy(pull = it.pull.copy(error = "Enter a model name, e.g. qwen3.5:4b or llama3.2"))
            }
            return
        }
        if (_state.value.pull.active) return
        _state.update { it.copy(pull = PullUiState(active = true, model = name, status = "Starting download…")) }
        pullJob = viewModelScope.launch {
            try {
                api.pull(_state.value.baseUrl, name).collect { p ->
                    _state.update {
                        it.copy(
                            pull = it.pull.copy(
                                status = p.status.ifBlank { it.pull.status },
                                fraction = p.fraction
                            )
                        )
                    }
                }
                _state.update { it.copy(pull = PullUiState(model = name, status = "Ready: $name")) }
                refreshModels(selectAfter = name)
            } catch (e: CancellationException) {
                _state.update { it.copy(pull = PullUiState(model = name, status = "Download cancelled")) }
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(pull = PullUiState(model = name, error = e.message ?: "Download failed"))
                }
            }
        }
    }

    fun cancelPull() = pullJob?.cancel()

    // ---- Chat ----

    fun sendMessage(text: String): Boolean {
        val content = text.trim()
        if (content.isEmpty() || _state.value.streaming) return false
        val s = _state.value
        val model = s.selectedModel
        if (model.isNullOrBlank()) {
            _state.update {
                it.copy(chatError = "No model selected. Connect to Ollama and select an installed model.")
            }
            return false
        }
        val userMsg = ChatMessage(nextId++, "user", content)
        val assistantMsg = ChatMessage(nextId++, "assistant", "")
        val history = buildList {
            if (s.systemPrompt.isNotBlank()) add(ChatMessage(0, "system", s.systemPrompt))
            addAll(s.messages)
            add(userMsg)
        }

        val updatedMessages = s.messages + userMsg + assistantMsg
        _state.update {
            it.copy(messages = updatedMessages, streaming = true, chatError = null)
        }

        chatJob = viewModelScope.launch {
            try {
                api.chat(s.baseUrl, model, history, s.temperature).collect { chunk ->
                    if (chunk.content.isNotEmpty()) appendToMessage(assistantMsg.id, chunk.content)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(chatError = e.message ?: "Chat failed.") }
            } finally {
                _state.update { st ->
                    st.copy(
                        streaming = false,
                        messages = st.messages.filterNot { it.id == assistantMsg.id && it.content.isEmpty() }
                    )
                }
                persistCurrentSession()
            }
        }
        return true
    }

    private fun appendToMessage(id: Long, text: String) {
        _state.update { st ->
            st.copy(messages = st.messages.map {
                if (it.id == id) it.copy(content = it.content + text) else it
            })
        }
    }

    private fun persistCurrentSession() {
        val s = _state.value
        val current = s.sessions.firstOrNull { it.id == s.currentSessionId } ?: return
        val title = if (current.title == "New Chat" && s.messages.isNotEmpty()) {
            val firstUser = s.messages.firstOrNull { it.role == "user" }?.content.orEmpty()
            if (firstUser.isNotBlank()) firstUser.take(30).trim() + "…" else current.title
        } else current.title

        val updatedSession = current.copy(
            title = title,
            updatedAt = System.currentTimeMillis(),
            messages = s.messages,
            model = s.selectedModel
        )
        val list = s.sessions.map { if (it.id == updatedSession.id) updatedSession else it }
            .sortedByDescending { it.updatedAt }

        _state.update { it.copy(sessions = list) }
        sessionManager.saveSessions(list)
    }

    fun stopStreaming() = chatJob?.cancel()

    fun clearChat() {
        chatJob?.cancel()
        _state.update { it.copy(messages = emptyList(), chatError = null) }
        persistCurrentSession()
    }

    fun dismissChatError() = _state.update { it.copy(chatError = null) }
}
