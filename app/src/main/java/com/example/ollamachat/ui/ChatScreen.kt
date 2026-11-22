package com.example.ollamachat.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Terracotta = Color(0xFFD97757)
private val WarmPeach = Color(0xFFE08E6D)
private val SkyBlue = Color(0xFF8AB4F8)

private data class ChatEntry(
    val id: String,
    val role: String,
    val content: String,
    val attachmentLabel: String? = null
)

private data class SavedThread(
    val id: String,
    val title: String,
    val model: String?,
    val messages: List<ChatEntry>
)

private data class StudioConfig(
    val hostUrl: String = "http://192.168.1.50:11434",
    val cpuCores: Int = 6,
    val maxTokens: Int = 1024,
    val contextSize: Int = 4096,
    val conciseDiffMode: Boolean = true,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.08f
)

private fun loadConfig(context: Context): StudioConfig {
    val prefs = context.getSharedPreferences("studio_prefs", Context.MODE_PRIVATE)
    return StudioConfig(
        hostUrl = prefs.getString("host_url", "http://192.168.1.50:11434") ?: "http://192.168.1.50:11434",
        cpuCores = prefs.getInt("cpu_cores", 6).coerceIn(1, 32),
        maxTokens = prefs.getInt("max_tokens", 1024).coerceIn(64, 16384),
        contextSize = prefs.getInt("context_size", 4096).coerceIn(512, 32768),
        conciseDiffMode = prefs.getBoolean("concise_diff", true),
        speechRate = prefs.getFloat("speech_rate", 1.0f).coerceIn(0.6f, 1.6f),
        speechPitch = prefs.getFloat("speech_pitch", 1.08f).coerceIn(0.75f, 1.35f)
    )
}

private fun saveConfig(context: Context, cfg: StudioConfig) {
    context.getSharedPreferences("studio_prefs", Context.MODE_PRIVATE).edit()
        .putString("host_url", cfg.hostUrl.trim())
        .putInt("cpu_cores", cfg.cpuCores)
        .putInt("max_tokens", cfg.maxTokens)
        .putInt("context_size", cfg.contextSize)
        .putBoolean("concise_diff", cfg.conciseDiffMode)
        .putFloat("speech_rate", cfg.speechRate)
        .putFloat("speech_pitch", cfg.speechPitch)
        .apply()
}

private fun loadThreads(context: Context): MutableList<SavedThread> {
    val raw = context.getSharedPreferences("studio_threads", Context.MODE_PRIVATE)
        .getString("threads_json", "[]") ?: "[]"
    val out = mutableListOf<SavedThread>()
    runCatching {
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val msgArr = obj.optJSONArray("messages") ?: JSONArray()
            val msgs = mutableListOf<ChatEntry>()
            for (j in 0 until msgArr.length()) {
                val m = msgArr.getJSONObject(j)
                msgs.add(
                    ChatEntry(
                        id = m.optString("id", UUID.randomUUID().toString()),
                        role = m.optString("role", "user"),
                        content = m.optString("content", ""),
                        attachmentLabel = m.optString("attachment", "").ifBlank { null }
                    )
                )
            }
            out.add(
                SavedThread(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    title = obj.optString("title", "Saved Chat"),
                    model = obj.optString("model", "").ifBlank { null },
                    messages = msgs
                )
            )
        }
    }
    return out
}

private fun persistThreads(context: Context, threads: List<SavedThread>) {
    val arr = JSONArray()
    threads.forEach { t ->
        val obj = JSONObject()
        obj.put("id", t.id)
        obj.put("title", t.title)
        obj.put("model", t.model ?: "")
        val msgArr = JSONArray()
        t.messages.forEach { m ->
            val mo = JSONObject()
            mo.put("id", m.id)
            mo.put("role", m.role)
            mo.put("content", m.content)
            mo.put("attachment", m.attachmentLabel ?: "")
            msgArr.put(mo)
        }
        obj.put("messages", msgArr)
        arr.put(obj)
    }
    context.getSharedPreferences("studio_threads", Context.MODE_PRIVATE).edit()
        .putString("threads_json", arr.toString())
        .apply()
}

private fun normalizeHost(raw: String): String {
    val trimmed = raw.trim().removeSuffix("/")
    if (trimmed.isEmpty()) return "http://127.0.0.1:11434"
    val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "http://$trimmed"
    }
    return if (Regex(":\\d+$").containsMatchIn(withScheme)) withScheme else "$withScheme:11434"
}

private suspend fun fetchModelsFromHost(baseUrl: String): List<String> = withContext(Dispatchers.IO) {
    val url = URL("${normalizeHost(baseUrl)}/api/tags")
    val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 2500
        readTimeout = 4000
    }
    try {
        if (conn.responseCode !in 200..299) return@withContext emptyList()
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val arr = json.optJSONArray("models") ?: JSONArray()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i)
            val name = item?.optString("name") ?: item?.optString("model")
            if (!name.isNullOrBlank()) list.add(name)
        }
        list
    } finally {
        conn.disconnect()
    }
}

@Suppress("DEPRECATION")
private fun wifiSubnetPrefix(context: Context): String? {
    return runCatching {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val ipInt = wm?.dhcpInfo?.ipAddress ?: 0
        if (ipInt == 0) return@runCatching null
        val b1 = ipInt and 0xff
        val b2 = (ipInt shr 8) and 0xff
        val b3 = (ipInt shr 16) and 0xff
        "$b1.$b2.$b3"
    }.getOrNull()
}

private suspend fun discoverOllamaOnLan(context: Context, currentHost: String): String? =
    withContext(Dispatchers.IO) {
        val candidates = linkedSetOf<String>()
        val currentStripped = normalizeHost(currentHost)
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore(":")
        if (currentStripped.isNotBlank()) candidates.add(currentStripped)

        val prefix = wifiSubnetPrefix(context)
        if (prefix != null && prefix != "0.0.0") {
            for (hostLast in 1..254) {
                candidates.add("$prefix.$hostLast")
            }
        } else {
            listOf("192.168.1", "192.168.0", "10.0.0").forEach { p ->
                for (hostLast in 1..254) candidates.add("$p.$hostLast")
            }
        }

        val executor = Executors.newFixedThreadPool(24)
        var foundIp: String? = null
        val lock = Any()

        try {
            val futures = candidates.map { ip ->
                executor.submit {
                    synchronized(lock) { if (foundIp != null) return@submit }
                    val open = runCatching {
                        Socket().use { sock ->
                            sock.connect(InetSocketAddress(ip, 11434), 180)
                            true
                        }
                    }.getOrDefault(false)
                    if (open) {
                        val ok = runCatching {
                            val c = (URL("http://$ip:11434/api/tags").openConnection() as HttpURLConnection).apply {
                                connectTimeout = 500
                                readTimeout = 800
                            }
                            try {
                                c.responseCode in 200..299
                            } finally {
                                c.disconnect()
                            }
                        }.getOrDefault(false)
                        if (ok) {
                            synchronized(lock) {
                                if (foundIp == null) foundIp = "http://$ip:11434"
                            }
                        }
                    }
                }
            }
            futures.forEach { runCatching { it.get() } }
        } finally {
            executor.shutdownNow()
        }
        foundIp
    }

private fun readAttachmentFromUri(context: Context, uri: Uri): Pair<String, String?> {
    var displayName = "attachment"
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    val n = cursor.getString(idx)
                    if (!n.isNullOrBlank()) displayName = n
                }
            }
        }

    val mime = context.contentResolver.getType(uri) ?: ""
    if (mime.startsWith("image/")) {
        return displayName to "[Attached image: $displayName]"
    }

    val textSnippet = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                val sb = StringBuilder()
                val maxChars = 60_000
                var total = 0
                var line: String?
                while (reader.readLine().also { line = it } != null && total < maxChars) {
                    val l = line ?: continue
                    sb.append(l).append('\n')
                    total += l.length + 1
                }
                if (reader.readLine() != null) {
                    sb.append("\n[File truncated at 60,000 characters for safe context handling]")
                }
                sb.toString()
            }
        }
    }.getOrNull()

    return if (textSnippet != null) {
        displayName to "\n--- File: $displayName ---\n$textSnippet\n--- End File ---\n"
    } else {
        displayName to "[Attached file: $displayName]"
    }
}

private fun selectFemaleVoice(tts: TextToSpeech): Voice? {
    val voices = runCatching { tts.voices }.getOrNull() ?: return null
    val femaleHints = listOf("female", "woman", "en-us-x-sfg", "en-us-x-tpf", "en-gb-x-gba", "en-gb-x-gbc")
    return voices.firstOrNull { v ->
        !v.isNetworkConnectionRequired &&
            v.locale.language == Locale.ENGLISH.language &&
            femaleHints.any { hint -> v.name.lowercase(Locale.ROOT).contains(hint) }
    } ?: voices.firstOrNull { v ->
        v.locale.language == Locale.ENGLISH.language &&
            femaleHints.any { hint -> v.name.lowercase(Locale.ROOT).contains(hint) }
    } ?: voices.firstOrNull { v ->
        v.locale.language == Locale.ENGLISH.language && !v.isNetworkConnectionRequired
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var config by remember { mutableStateOf(loadConfig(context)) }
    val savedThreads = remember { mutableStateListOf<SavedThread>().apply { addAll(loadThreads(context)) } }
    var activeThreadId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }

    val models = remember { mutableStateListOf<String>() }
    var selectedModel by rememberSaveable { mutableStateOf<String?>(null) }
    val messages = remember { mutableStateListOf<ChatEntry>() }

    var input by rememberSaveable { mutableStateOf("") }
    var pendingAttachmentName by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingAttachmentContext by rememberSaveable { mutableStateOf<String?>(null) }

    var streaming by remember { mutableStateOf(false) }
    var streamJob by remember { mutableStateOf<Job?>(null) }
    var bannerError by remember { mutableStateOf<String?>(null) }
    var statusNote by remember { mutableStateOf<String?>(null) }
    var scanningLan by remember { mutableStateOf(false) }

    var settingsOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var speakingMessageId by remember { mutableStateOf<String?>(null) }

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.US
                selectFemaleVoice(instance!!)?.let { instance?.voice = it }
                instance?.setSpeechRate(config.speechRate)
                instance?.setPitch(config.speechPitch)
                ttsReady = true
            }
        }
        instance
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                tts.stop()
                tts.shutdown()
            }
        }
    }

    fun refreshModels() {
        scope.launch {
            bannerError = null
            runCatching { fetchModelsFromHost(config.hostUrl) }
                .onSuccess { list ->
                    models.clear()
                    models.addAll(list)
                    if (selectedModel == null || selectedModel !in models) {
                        selectedModel = models.firstOrNull()
                    }
                    statusNote = if (list.isEmpty()) "Connected, no models listed" else "Loaded ${list.size} models"
                }
                .onFailure {
                    bannerError = "Cannot reach Ollama at ${normalizeHost(config.hostUrl)}"
                }
        }
    }

    fun scanForLaptop() {
        scope.launch {
            scanningLan = true
            bannerError = null
            statusNote = "Scanning Wi-Fi for Ollama (:11434)..."
            val found = discoverOllamaOnLan(context, config.hostUrl)
            scanningLan = false
            if (found != null) {
                config = config.copy(hostUrl = found)
                saveConfig(context, config)
                statusNote = "Detected Ollama at $found"
                refreshModels()
            } else {
                statusNote = null
                bannerError = "No Ollama server found on Wi-Fi port 11434"
            }
        }
    }

    fun saveCurrentThreadSnapshot() {
        if (messages.isEmpty()) return
        val firstUser = messages.firstOrNull { it.role == "user" }?.content?.take(36)?.ifBlank { "Chat" } ?: "Chat"
        val updated = SavedThread(
            id = activeThreadId,
            title = firstUser,
            model = selectedModel,
            messages = messages.toList()
        )
        val idx = savedThreads.indexOfFirst { it.id == activeThreadId }
        if (idx >= 0) savedThreads[idx] = updated else savedThreads.add(0, updated)
        persistThreads(context, savedThreads)
    }

    fun startNewThread() {
        saveCurrentThreadSnapshot()
        streamJob?.cancel()
        streaming = false
        activeThreadId = UUID.randomUUID().toString()
        messages.clear()
        pendingAttachmentName = null
        pendingAttachmentContext = null
        bannerError = null
    }

    fun openSavedThread(thread: SavedThread) {
        saveCurrentThreadSnapshot()
        streamJob?.cancel()
        streaming = false
        activeThreadId = thread.id
        messages.clear()
        messages.addAll(thread.messages)
        if (thread.model != null) selectedModel = thread.model
        historyOpen = false
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val (name, extracted) = readAttachmentFromUri(context, uri)
            pendingAttachmentName = name
            pendingAttachmentContext = extracted
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val (name, extracted) = readAttachmentFromUri(context, uri)
            pendingAttachmentName = name
            pendingAttachmentContext = extracted
        }
    }

    LaunchedEffect(Unit) {
        val auto = discoverOllamaOnLan(context, config.hostUrl)
        if (auto != null && auto != config.hostUrl) {
            config = config.copy(hostUrl = auto)
            saveConfig(context, config)
            statusNote = "Auto-detected $auto"
        }
        refreshModels()
    }

    val listState = rememberLazyListState()
    val tailLen = messages.lastOrNull()?.content?.length ?: 0
    LaunchedEffect(messages.size, tailLen) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }

    fun sendPrompt() {
        val rawText = input.trim()
        val attachText = pendingAttachmentContext.orEmpty()
        if (rawText.isEmpty() && attachText.isEmpty()) return

        val modelToUse = selectedModel ?: models.firstOrNull() ?: "llama3.2"
        val combinedPrompt = buildString {
            if (config.conciseDiffMode) {
                append("[Instruction: Name the exact target file and output ONLY changed or relevant lines to save tokens; do not reprint whole unchanged files.]\n\n")
            }
            if (rawText.isNotEmpty()) append(rawText)
            if (attachText.isNotEmpty()) {
                if (rawText.isNotEmpty()) append("\n")
                append(attachText)
            }
        }

        val displayMsg = if (rawText.isNotEmpty()) rawText else "Shared ${pendingAttachmentName ?: "file"}"
        messages.add(
            ChatEntry(
                id = UUID.randomUUID().toString(),
                role = "user",
                content = displayMsg,
                attachmentLabel = pendingAttachmentName
            )
        )
        val assistantIdx = messages.size
        val assistantId = UUID.randomUUID().toString()
        messages.add(ChatEntry(id = assistantId, role = "assistant", content = ""))

        input = ""
        pendingAttachmentName = null
        pendingAttachmentContext = null
        bannerError = null
        streaming = true
        saveCurrentThreadSnapshot()

        streamJob = scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val endpoint = URL("${normalizeHost(config.hostUrl)}/api/generate")
                    val optionsJson = JSONObject().apply {
                        put("num_thread", config.cpuCores)
                        put("num_predict", config.maxTokens)
                        put("num_ctx", config.contextSize)
                    }
                    val payload = JSONObject().apply {
                        put("model", modelToUse)
                        put("prompt", combinedPrompt)
                        put("stream", true)
                        put("options", optionsJson)
                    }

                    val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        connectTimeout = 5000
                        readTimeout = 0
                        setRequestProperty("Content-Type", "application/json")
                    }

                    try {
                        OutputStreamWriter(conn.outputStream).use { writer ->
                            writer.write(payload.toString())
                            writer.flush()
                        }

                        if (conn.responseCode !in 200..299) {
                            val errText = conn.errorStream?.bufferedReader()?.use { it.readText() }
                                ?: "HTTP ${conn.responseCode}"
                            withContext(Dispatchers.Main) {
                                bannerError = errText
                            }
                            return@withContext
                        }

                        val acc = StringBuilder()
                        BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                val trimmed = line?.trim().orEmpty()
                                if (trimmed.isEmpty()) continue
                                val chunk = runCatching { JSONObject(trimmed) }.getOrNull() ?: continue
                                val token = chunk.optString("response", "")
                                if (token.isNotEmpty()) {
                                    acc.append(token)
                                    val snapshot = acc.toString()
                                    withContext(Dispatchers.Main) {
                                        if (assistantIdx in messages.indices) {
                                            messages[assistantIdx] = messages[assistantIdx].copy(content = snapshot)
                                        }
                                    }
                                }
                                if (chunk.optBoolean("done", false)) break
                            }
                        }
                    } finally {
                        conn.disconnect()
                    }
                }
            }.onFailure { err ->
                if (err !is kotlinx.coroutines.CancellationException) {
                    bannerError = err.message ?: "Generation failed"
                }
            }

            streaming = false
            saveCurrentThreadSnapshot()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(WarmPeach)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Studio",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { historyOpen = true }) {
                        Icon(Icons.Default.History, contentDescription = "Chat history")
                    }
                    IconButton(onClick = { refreshModels() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh models")
                    }
                    IconButton(onClick = { startNewThread() }) {
                        Icon(Icons.Default.Delete, contentDescription = "New chat")
                    }
                    IconButton(onClick = { settingsOpen = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ModelDropdownPill(
                    models = models,
                    selected = selectedModel,
                    onSelect = { selectedModel = it }
                )

                TextButton(
                    onClick = { scanForLaptop() },
                    enabled = !scanningLan
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = SkyBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (scanningLan) "Scanning..." else "Auto-IP",
                        color = SkyBlue,
                        maxLines = 1
                    )
                }
            }

            if (statusNote != null) {
                Text(
                    text = statusNote.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            if (bannerError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bannerError.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { bannerError = null }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            if (messages.isEmpty()) {
                StarterDeck(
                    onPickStarter = { input = it },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isSpeakingThis = speakingMessageId == msg.id
                        BubbleCard(
                            entry = msg,
                            isSpeaking = isSpeakingThis,
                            onCopy = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("message", msg.content))
                                statusNote = "Copied message"
                            },
                            onToggleSpeak = {
                                if (isSpeakingThis) {
                                    tts.stop()
                                    speakingMessageId = null
                                } else if (ttsReady && msg.content.isNotBlank()) {
                                    selectFemaleVoice(tts)?.let { tts.voice = it }
                                    tts.setSpeechRate(config.speechRate)
                                    tts.setPitch(config.speechPitch)
                                    tts.speak(msg.content, TextToSpeech.QUEUE_FLUSH, null, msg.id)
                                    speakingMessageId = msg.id
                                }
                            }
                        )
                    }
                }
            }

            if (pendingAttachmentName != null) {
                Surface(
                    color = Color(0xFF1A2232),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .border(1.dp, Color(0xFF344B73), RoundedCornerShape(999.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Attached: ${pendingAttachmentName.orEmpty()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SkyBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove attachment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    pendingAttachmentName = null
                                    pendingAttachmentContext = null
                                }
                        )
                    }
                }
            }

            Surface(
                color = Color(0xFF161922),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .border(1.dp, Color(0xFF3A3544), RoundedCornerShape(28.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Attach file",
                            tint = SkyBlue
                        )
                    }
                    IconButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Attach photo",
                            tint = WarmPeach
                        )
                    }

                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "Ask anything...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = WarmPeach,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    if (streaming) {
                        IconButton(
                            onClick = {
                                streamJob?.cancel()
                                streaming = false
                                saveCurrentThreadSnapshot()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3E241C))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Stop",
                                tint = WarmPeach
                            )
                        }
                    } else {
                        val ready = input.isNotBlank() || pendingAttachmentContext != null
                        IconButton(
                            onClick = { sendPrompt() },
                            enabled = ready,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (ready) Terracotta else Color(0xFF1F2330))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (ready) Color(0xFF140B07)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (historyOpen) {
        AlertDialog(
            onDismissRequest = { historyOpen = false },
            title = { Text("Saved Chats", fontFamily = FontFamily.Serif) },
            text = {
                if (savedThreads.isEmpty()) {
                    Text("No saved chats yet. Send a message to save this conversation.")
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        savedThreads.forEach { thread ->
                            Surface(
                                color = Color(0xFF1B1F2C),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openSavedThread(thread) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = thread.title,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${thread.messages.size} messages • ${thread.model ?: "default"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = {
                                        savedThreads.remove(thread)
                                        persistThreads(context, savedThreads)
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete chat",
                                            tint = Terracotta
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    startNewThread()
                    historyOpen = false
                }) {
                    Text("New Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { historyOpen = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (settingsOpen) {
        var hostDraft by rememberSaveable { mutableStateOf(config.hostUrl) }
        var coresVal by rememberSaveable { mutableStateOf(config.cpuCores.toFloat()) }
        var maxTokensText by rememberSaveable { mutableStateOf(config.maxTokens.toString()) }
        var ctxText by rememberSaveable { mutableStateOf(config.contextSize.toString()) }
        var conciseFlag by rememberSaveable { mutableStateOf(config.conciseDiffMode) }
        var rateVal by rememberSaveable { mutableStateOf(config.speechRate) }
        var pitchVal by rememberSaveable { mutableStateOf(config.speechPitch) }

        AlertDialog(
            onDismissRequest = { settingsOpen = false },
            title = { Text("Studio & Model Settings", fontFamily = FontFamily.Serif) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = hostDraft,
                        onValueChange = { hostDraft = it },
                        label = { Text("Laptop Ollama Host / IP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = { scanForLaptop() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (scanningLan) "Scanning Wi-Fi..." else "Auto-Detect Laptop on Wi-Fi")
                    }

                    HorizontalDivider()

                    Text(
                        text = "Requested CPU Cores / Threads: ${coresVal.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = coresVal,
                        onValueChange = { coresVal = it },
                        valueRange = 1f..16f,
                        steps = 14
                    )

                    OutlinedTextField(
                        value = maxTokensText,
                        onValueChange = { maxTokensText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Max Tokens (num_predict)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ctxText,
                        onValueChange = { ctxText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Context Window (num_ctx)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Changed Lines Only Mode", fontWeight = FontWeight.Medium)
                            Text(
                                "Names target file & outputs only needed lines to save tokens",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = conciseFlag, onCheckedChange = { conciseFlag = it })
                    }

                    HorizontalDivider()

                    Text(
                        text = "Female Voice Speed: ${"%.1f".format(rateVal)}x",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = rateVal,
                        onValueChange = { rateVal = it },
                        valueRange = 0.7f..1.5f
                    )

                    Text(
                        text = "Female Voice Pitch: ${"%.2f".format(pitchVal)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = pitchVal,
                        onValueChange = { pitchVal = it },
                        valueRange = 0.85f..1.30f
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedMax = maxTokensText.toIntOrNull()?.coerceIn(64, 32768) ?: config.maxTokens
                        val parsedCtx = ctxText.toIntOrNull()?.coerceIn(512, 65536) ?: config.contextSize
                        config = StudioConfig(
                            hostUrl = hostDraft.trim().ifBlank { config.hostUrl },
                            cpuCores = coresVal.toInt().coerceIn(1, 32),
                            maxTokens = parsedMax,
                            contextSize = parsedCtx,
                            conciseDiffMode = conciseFlag,
                            speechRate = rateVal,
                            speechPitch = pitchVal
                        )
                        saveConfig(context, config)
                        settingsOpen = false
                        refreshModels()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta, contentColor = Color(0xFF140B07))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { settingsOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ModelDropdownPill(
    models: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        Surface(
            color = Color(0xFF191D29),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.border(1.dp, Color(0xFF31394D), RoundedCornerShape(999.dp))
        ) {
            TextButton(
                onClick = { open = true },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (selected != null) WarmPeach else SkyBlue)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selected ?: "Select model",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.widthIn(max = 220.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (models.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No models found") },
                    onClick = { open = false },
                    enabled = false
                )
            }
            models.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m) },
                    onClick = {
                        onSelect(m)
                        open = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StarterDeck(
    onPickStarter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val starters = listOf(
        "Break down a tricky problem step by step",
        "Write clean code and explain only changed lines",
        "Summarize a file and highlight key risks"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "What are we building?",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Pick a prompt, attach a file or photo, or type below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            starters.forEachIndexed { i, text ->
                FilterChip(
                    selected = false,
                    onClick = { onPickStarter(text) },
                    label = { Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
    }
}

@Composable
private fun BubbleCard(
    entry: ChatEntry,
    isSpeaking: Boolean,
    onCopy: () -> Unit,
    onToggleSpeak: () -> Unit
) {
    val isUser = entry.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            shape = if (isUser) {
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp)
            } else {
                RoundedCornerShape(16.dp)
            },
            modifier = Modifier
                .widthIn(max = 330.dp)
                .border(
                    width = 1.dp,
                    color = if (isUser) Color(0xFF33578A) else Color(0xFF3E2B25),
                    shape = if (isUser) {
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp)
                    } else {
                        RoundedCornerShape(16.dp)
                    }
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp)) {
                if (entry.attachmentLabel != null) {
                    Text(
                        text = "Attachment: ${entry.attachmentLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = entry.content.ifEmpty { "…" },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (entry.content.isNotBlank()) {
            Row(
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCopy,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(
                    onClick = onToggleSpeak,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isSpeaking) "Stop TTS" else "Listen",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
