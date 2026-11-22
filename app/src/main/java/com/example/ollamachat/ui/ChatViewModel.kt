package com.example.ollamachat.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ollamachat.data.ChatMessage
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
    val models: List<String> = emptyList(),
    val selectedModel: String? = null,
    val loadingModels: Boolean = false,
    val connectionError: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val streaming: Boolean = false,
    val chatError: String? = null,
    val pull: PullUiState = PullUiState(),
    val settingsOpen: Boolean = false
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SettingsStore(app)
    private val api = OllamaApi()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private var chatJob: Job? = null
    private var pullJob: Job? = null
    private var nextId = 1L

    init {
        viewModelScope.launch {
            val url = store.baseUrl.first()
            val model = store.selectedModel.first()
            val prompt = store.systemPrompt.first()
            _state.update { it.copy(baseUrl = url, selectedModel = model, systemPrompt = prompt) }
            refreshModels()
        }
    }

    // ---- Settings UI ----

    fun openSettings() = _state.update { it.copy(settingsOpen = true) }
    fun closeSettings() = _state.update { it.copy(settingsOpen = false) }

    /** Saves the base URL (normalized) and reloads models. Returns an error message, or null on success. */
    fun saveBaseUrl(raw: String): String? {
        val normalized = UrlUtils.normalizeBaseUrl(raw)
            ?: return "Enter a valid address, e.g. http://192.168.1.20:11434"
        _state.update { it.copy(baseUrl = normalized) }
        viewModelScope.launch { store.setBaseUrl(normalized) }
        refreshModels()
        return null
    }

    fun saveSystemPrompt(text: String) {
        _state.update { it.copy(systemPrompt = text) }
        viewModelScope.launch { store.setSystemPrompt(text) }
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
                            "Connected, but no models are installed yet. Download one in Settings."
                        } else {
                            null
                        }
                    )
                }
                store.setSelectedModel(chosen)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loadingModels = false, connectionError = e.message ?: "Could not reach the server.")
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
    }

    // ---- Pull: POST /api/pull ----

    fun pullModel(raw: String) {
        val name = raw.trim()
        if (name.isEmpty()) {
            _state.update {
                it.copy(pull = it.pull.copy(error = "Enter a model name, e.g. llama3.2 or qwen2.5:14b"))
            }
            return
        }
        if (_state.value.pull.active) return
        _state.update { it.copy(pull = PullUiState(active = true, model = name, status = "Starting…")) }
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
                _state.update { it.copy(pull = PullUiState(model = name, status = "Download complete: $name")) }
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

    fun cancelPull() {
        pullJob?.cancel()
    }

    // ---- Chat: POST /api/chat ----

    /** Returns true if the message was accepted (so the UI can clear the input box). */
    fun sendMessage(text: String): Boolean {
        val content = text.trim()
        if (content.isEmpty()) return false
        val s = _state.value
        if (s.streaming) return false
        val model = s.selectedModel
        if (model.isNullOrBlank()) {
            _state.update {
                it.copy(chatError = "No model selected. Open Settings to refresh or download a model.")
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
        _state.update {
            it.copy(messages = it.messages + userMsg + assistantMsg, streaming = true, chatError = null)
        }
        chatJob = viewModelScope.launch {
            try {
                api.chat(s.baseUrl, model, history).collect { chunk ->
                    if (chunk.content.isNotEmpty()) appendToMessage(assistantMsg.id, chunk.content)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(chatError = e.message ?: "Chat request failed.") }
            } finally {
                _state.update { st ->
                    st.copy(
                        streaming = false,
                        messages = st.messages.filterNot {
                            it.id == assistantMsg.id && it.content.isEmpty()
                        }
                    )
                }
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

    fun stopStreaming() {
        chatJob?.cancel()
    }

    fun clearChat() {
        chatJob?.cancel()
        _state.update { it.copy(messages = emptyList(), chatError = null) }
    }

    fun dismissChatError() = _state.update { it.copy(chatError = null) }
}
