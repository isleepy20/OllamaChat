package com.example.ollamachat.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Terracotta = Color(0xFFD97757)
private val WarmPeach = Color(0xFFE08E6D)
private val SkyBlue = Color(0xFF8AB4F8)

private data class AttachmentPreview(
    val name: String,
    val mimeType: String,
    val isImage: Boolean,
    val bitmap: Bitmap?,
    val base64Image: String?,
    val extractedText: String,
    val byteCount: Long
)

private data class ChatEntry(
    val id: String,
    val role: String,
    val content: String,
    val attachmentName: String? = null,
    val isAttachmentImage: Boolean = false,
    val changeSummary: String? = null,
    val bundleZipPath: String? = null
)

private data class SavedThread(
    val id: String,
    val title: String,
    val model: String?,
    val messages: List<ChatEntry>
)

private data class StudioConfig(
    val hostUrl: String = "http://10.0.0.134:11434",
    val cpuCores: Int = 5,
    val gpuLayers: Int = 99,
    val maxTokens: Int = 512,
    val contextSize: Int = 1536,
    val temperature: Float = 0.6f,
    val topP: Float = 0.85f,
    val keepAlive: String = "15m",
    val conciseDiffMode: Boolean = true,
    val singleHtmlAppMode: Boolean = false,
    val autoBundleZip: Boolean = true,
    val lineCheckpointTracker: Boolean = true,
    val speechRate: Float = 0.95f,
    val speechPitch: Float = 1.0f,
    val preferredVoiceName: String = "",
    val voiceServerUrl: String = "",
    val voiceClipName: String = ""
)

private fun loadConfig(context: Context): StudioConfig {
    val prefs = context.getSharedPreferences("studio_prefs", Context.MODE_PRIVATE)
    return StudioConfig(
        hostUrl = prefs.getString("host_url", "http://10.0.0.134:11434") ?: "http://10.0.0.134:11434",
        cpuCores = prefs.getInt("cpu_cores", 5).coerceIn(1, 32),
        gpuLayers = prefs.getInt("gpu_layers", 99).coerceIn(0, 200),
        maxTokens = prefs.getInt("max_tokens", 512).coerceIn(64, 32768),
        contextSize = prefs.getInt("context_size", 1536).coerceIn(512, 65536),
        temperature = prefs.getFloat("temperature", 0.6f).coerceIn(0.0f, 2.0f),
        topP = prefs.getFloat("top_p", 0.85f).coerceIn(0.1f, 1.0f),
        keepAlive = prefs.getString("keep_alive", "15m") ?: "15m",
        conciseDiffMode = prefs.getBoolean("concise_diff", true),
        singleHtmlAppMode = prefs.getBoolean("single_html_app", false),
        autoBundleZip = prefs.getBoolean("auto_bundle_zip", true),
        lineCheckpointTracker = prefs.getBoolean("line_checkpoint", true),
        speechRate = prefs.getFloat("speech_rate", 0.95f).coerceIn(0.6f, 1.6f),
        speechPitch = prefs.getFloat("speech_pitch", 1.0f).coerceIn(0.75f, 1.30f),
        preferredVoiceName = prefs.getString("preferred_voice_name", "") ?: "",
        voiceServerUrl = prefs.getString("voice_server_url", "") ?: "",
        voiceClipName = prefs.getString("voice_clip_name", "") ?: ""
    )
}

private fun saveConfig(context: Context, cfg: StudioConfig) {
    context.getSharedPreferences("studio_prefs", Context.MODE_PRIVATE).edit()
        .putString("host_url", cfg.hostUrl.trim())
        .putInt("cpu_cores", cfg.cpuCores)
        .putInt("gpu_layers", cfg.gpuLayers)
        .putInt("max_tokens", cfg.maxTokens)
        .putInt("context_size", cfg.contextSize)
        .putFloat("temperature", cfg.temperature)
        .putFloat("top_p", cfg.topP)
        .putString("keep_alive", cfg.keepAlive.trim().ifBlank { "15m" })
        .putBoolean("concise_diff", cfg.conciseDiffMode)
        .putBoolean("single_html_app", cfg.singleHtmlAppMode)
        .putBoolean("auto_bundle_zip", cfg.autoBundleZip)
        .putBoolean("line_checkpoint", cfg.lineCheckpointTracker)
        .putFloat("speech_rate", cfg.speechRate)
        .putFloat("speech_pitch", cfg.speechPitch)
        .putString("preferred_voice_name", cfg.preferredVoiceName)
        .putString("voice_server_url", cfg.voiceServerUrl.trim())
        .putString("voice_clip_name", cfg.voiceClipName.trim())
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
                        attachmentName = m.optString("attachment_name", "").ifBlank { null },
                        isAttachmentImage = m.optBoolean("attachment_is_image", false),
                        changeSummary = m.optString("change_summary", "").ifBlank { null },
                        bundleZipPath = m.optString("bundle_zip_path", "").ifBlank { null }
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
            mo.put("attachment_name", m.attachmentName ?: "")
            mo.put("attachment_is_image", m.isAttachmentImage)
            mo.put("change_summary", m.changeSummary ?: "")
            mo.put("bundle_zip_path", m.bundleZipPath ?: "")
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

private fun normalizeVoiceServerEndpoint(raw: String): String {
    val trimmed = raw.trim().removeSuffix("/")
    val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "http://$trimmed"
    }
    return if (withScheme.endsWith("/tts")) withScheme else "$withScheme/tts"
}

private suspend fun fetchClonedAudioFile(
    context: Context,
    serverUrl: String,
    text: String,
    voiceClipName: String
): File = withContext(Dispatchers.IO) {
    val endpoint = URL(normalizeVoiceServerEndpoint(serverUrl))
    val payload = JSONObject().apply {
        put("text", text.take(1500))
        if (voiceClipName.isNotBlank()) {
            put("ref_audio_name", voiceClipName)
        }
    }
    val conn = (endpoint.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 5000
        readTimeout = 30000
        setRequestProperty("Content-Type", "application/json")
    }
    try {
        OutputStreamWriter(conn.outputStream).use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }
        if (conn.responseCode !in 200..299) {
            error("Voice server HTTP ${conn.responseCode}")
        }
        val outFile = File(context.cacheDir, "cloned_speech_${System.currentTimeMillis()}.wav")
        conn.inputStream.use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
        outFile
    } finally {
        conn.disconnect()
    }
}

private data class HostTelemetry(
    val models: List<String>,
    val activeModel: String?,
    val vramSizeMb: Long?
)

private suspend fun inspectHostTelemetry(baseUrl: String): HostTelemetry = withContext(Dispatchers.IO) {
    val base = normalizeHost(baseUrl)
    val tagsUrl = URL("$base/api/tags")
    val tagsConn = (tagsUrl.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 2500
        readTimeout = 4000
    }
    val modelList = mutableListOf<String>()
    try {
        if (tagsConn.responseCode in 200..299) {
            val body = tagsConn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONObject(body).optJSONArray("models") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i)
                val name = item?.optString("name") ?: item?.optString("model")
                if (!name.isNullOrBlank()) modelList.add(name)
            }
        } else {
            error("HTTP ${tagsConn.responseCode} from $base")
        }
    } finally {
        tagsConn.disconnect()
    }

    var activeModel: String? = null
    var vramMb: Long? = null
    runCatching {
        val psConn = (URL("$base/api/ps").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 1500
            readTimeout = 2000
        }
        try {
            if (psConn.responseCode in 200..299) {
                val psBody = psConn.inputStream.bufferedReader().use { it.readText() }
                val psArr = JSONObject(psBody).optJSONArray("models") ?: JSONArray()
                if (psArr.length() > 0) {
                    val first = psArr.getJSONObject(0)
                    activeModel = first.optString("name").ifBlank { null }
                    val sizeVram = first.optLong("size_vram", 0L)
                    if (sizeVram > 0L) vramMb = sizeVram / (1024L * 1024L)
                }
            }
        } finally {
            psConn.disconnect()
        }
    }

    HostTelemetry(models = modelList, activeModel = activeModel, vramSizeMb = vramMb)
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
            for (hostLast in 1..254) candidates.add("$prefix.$hostLast")
        } else {
            listOf("10.0.0", "192.168.1", "192.168.0").forEach { p ->
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

private fun createTempPhotoUri(context: Context): Uri? {
    return runCatching {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}

private fun shareZipFile(context: Context, zipPath: String) {
    runCatching {
        val file = File(zipPath)
        if (!file.exists()) {
            Toast.makeText(context, "ZIP file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ZIP"))
    }.onFailure {
        Toast.makeText(context, "Could not share ZIP: ${it.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun saveZipDirectToDownloads(context: Context, sourceZipPath: String): String? {
    val src = File(sourceZipPath)
    if (!src.exists()) return null

    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, src.name)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching null
            resolver.openOutputStream(itemUri)?.use { out ->
                FileInputStream(src).use { input -> input.copyTo(out) }
                out.flush()
            } ?: run {
                resolver.delete(itemUri, null, null)
                return@runCatching null
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
            "${Environment.DIRECTORY_DOWNLOADS}/${src.name}"
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs()
            val outFile = File(downloadsDir, src.name)
            FileInputStream(src).use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
            outFile.absolutePath
        }
    }.getOrNull()
}

private fun copyZipToUri(context: Context, sourceZipPath: String, destinationUri: Uri): Boolean {
    return runCatching {
        val src = File(sourceZipPath)
        if (!src.exists()) return@runCatching false
        context.contentResolver.openOutputStream(destinationUri)?.use { out ->
            FileInputStream(src).use { input ->
                input.copyTo(out)
            }
            out.flush()
        } ?: return@runCatching false
        true
    }.getOrDefault(false)
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
    return null
}

private fun inspectAttachment(context: Context, uri: Uri): AttachmentPreview {
    var displayName = queryDisplayName(context, uri) ?: "attachment"
    var sizeBytes = 0L
    context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    sizeBytes = cursor.getLong(sizeIdx)
                }
            }
        }

    val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
    val lowerName = displayName.lowercase(Locale.ROOT)
    val looksImage = mime.startsWith("image/") ||
        lowerName.endsWith(".png") ||
        lowerName.endsWith(".jpg") ||
        lowerName.endsWith(".jpeg") ||
        lowerName.endsWith(".webp")

    if (looksImage) {
        val rawBytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        val decoded = rawBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        val base64 = decoded?.let { bmp ->
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
        }
        if (sizeBytes == 0L && rawBytes != null) sizeBytes = rawBytes.size.toLong()
        return AttachmentPreview(
            name = displayName,
            mimeType = mime,
            isImage = true,
            bitmap = decoded,
            base64Image = base64,
            extractedText = "",
            byteCount = sizeBytes
        )
    }

    val extracted = runCatching {
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
                    sb.append("\n[Truncated at 60,000 chars for safe local memory use]")
                }
                sb.toString()
            }
        }
    }.getOrNull().orEmpty()

    val cleanText = if (extracted.any { it.isLetterOrDigit() }) {
        extracted
    } else if (lowerName.endsWith(".pdf")) {
        "[PDF file attached: $displayName]"
    } else {
        "[File attached: $displayName ($mime)]"
    }

    return AttachmentPreview(
        name = displayName,
        mimeType = mime,
        isImage = false,
        bitmap = null,
        base64Image = null,
        extractedText = cleanText,
        byteCount = sizeBytes
    )
}

private fun writeFilesAndOptionalZip(
    context: Context,
    responseText: String,
    makeZip: Boolean
): Pair<String, String?> {
    val workspaceRoot = File(context.filesDir, "workspace/${System.currentTimeMillis()}").apply { mkdirs() }
    val fenceRegex = Regex("```([a-zA-Z0-9_+.-]*)\\s*(?:file:([^\\n]+))?\\n([\\s\\S]*?)```")
    val matches = fenceRegex.findAll(responseText).toList()

    val writtenFiles = mutableListOf<File>()
    matches.forEachIndexed { idx, m ->
        val lang = m.groupValues[1].trim().lowercase(Locale.ROOT)
        val explicitPath = m.groupValues[2].trim().removePrefix("/").removePrefix("./")
        val body = m.groupValues[3]

        val ext = when (lang) {
            "cpp", "c++", "cc", "cxx" -> "cpp"
            "c" -> "c"
            "h", "hpp", "hxx" -> "hpp"
            "html", "htm" -> "html"
            "kt", "kotlin" -> "kt"
            "py", "python" -> "py"
            "js", "javascript" -> "js"
            "ts", "typescript" -> "ts"
            "json" -> "json"
            "xml" -> "xml"
            "css" -> "css"
            "sh", "bash" -> "sh"
            else -> lang.ifBlank { "txt" }
        }

        val relPath = if (explicitPath.isNotBlank()) explicitPath else "generated_${idx + 1}.$ext"
        val safeTarget = File(workspaceRoot, relPath)
        if (safeTarget.canonicalPath.startsWith(workspaceRoot.canonicalPath)) {
            safeTarget.parentFile?.mkdirs()
            safeTarget.writeText(body)
            writtenFiles.add(safeTarget)
        }
    }

    val lineCount = responseText.lines().size
    val summary = buildString {
        append("Updated output ($lineCount lines)")
        if (writtenFiles.isNotEmpty()) {
            append(" • Saved ${writtenFiles.size} file(s): ")
            append(writtenFiles.take(3).joinToString { it.name })
            if (writtenFiles.size > 3) append(" +${writtenFiles.size - 3} more")
        }
    }

    var zipPath: String? = null
    if (makeZip && writtenFiles.size >= 2) {
        val zipFile = File(context.filesDir, "bundle_${System.currentTimeMillis()}.zip")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            writtenFiles.forEach { file ->
                val entryName = file.relativeTo(workspaceRoot).path.replace('\\', '/')
                zos.putNextEntry(ZipEntry(entryName))
                BufferedInputStream(FileInputStream(file)).use { bis ->
                    bis.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
        zipPath = zipFile.absolutePath
    }

    return summary to zipPath
}

private fun availableEnglishVoices(tts: TextToSpeech): List<Voice> {
    val all = runCatching { tts.voices }.getOrNull().orEmpty()
    return all
        .filter { it.locale.language == Locale.ENGLISH.language }
        .sortedBy { it.name }
}

private fun resolveVoice(tts: TextToSpeech, preferredName: String): Voice? {
    val voices = availableEnglishVoices(tts)
    if (voices.isEmpty()) return null
    if (preferredName.isNotBlank()) {
        voices.firstOrNull { it.name == preferredName }?.let { return it }
    }
    return voices.firstOrNull { !it.isNetworkConnectionRequired && it.quality >= Voice.QUALITY_HIGH }
        ?: voices.firstOrNull { !it.isNetworkConnectionRequired }
        ?: voices.firstOrNull()
}

private fun formatLineCheckpoint(lineCount: Int): Int {
    if (lineCount <= 1) return 1
    return (lineCount / 10) * 10
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
    var attachment by remember { mutableStateOf<AttachmentPreview?>(null) }
    var plusMenuOpen by remember { mutableStateOf(false) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingZipExportPath by rememberSaveable { mutableStateOf<String?>(null) }

    var streaming by remember { mutableStateOf(false) }
    var streamJob by remember { mutableStateOf<Job?>(null) }
    var elapsedSec by remember { mutableStateOf(0) }
    var estimatedTotalSec by remember { mutableStateOf(0) }
    var checkpointLine by remember { mutableStateOf(0) }
    var userScrolledAway by rememberSaveable { mutableStateOf(false) }

    var bannerError by remember { mutableStateOf<String?>(null) }
    var statusNote by remember { mutableStateOf<String?>(null) }
    var scanningLan by remember { mutableStateOf(false) }

    var settingsOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var speakingMessageId by remember { mutableStateOf<String?>(null) }

    val mediaPlayer = remember { MediaPlayer() }
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.US
                resolveVoice(instance!!, config.preferredVoiceName)?.let { instance?.voice = it }
                instance?.setSpeechRate(config.speechRate)
                instance?.setPitch(config.speechPitch)
                ttsReady = true
            }
        }
        instance
    }

    fun stopAllAudio() {
        runCatching {
            if (mediaPlayer.isPlaying) mediaPlayer.stop()
            mediaPlayer.reset()
        }
        runCatching { tts.stop() }
        speakingMessageId = null
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            }
            runCatching {
                tts.stop()
                tts.shutdown()
            }
        }
    }

    LaunchedEffect(streaming) {
        if (streaming) {
            elapsedSec = 0
            estimatedTotalSec = (config.maxTokens / 40).coerceIn(5, 90)
            while (streaming) {
                delay(1000L)
                elapsedSec += 1
            }
        }
    }

    val createZipDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { destUri ->
        val srcPath = pendingZipExportPath
        pendingZipExportPath = null
        if (destUri != null && srcPath != null) {
            val saved = copyZipToUri(context, srcPath, destUri)
            if (saved) {
                statusNote = "Saved ZIP to chosen folder"
                Toast.makeText(context, "Saved ZIP to chosen folder", Toast.LENGTH_SHORT).show()
            } else {
                bannerError = "Failed to write ZIP to selected storage location"
            }
        }
    }

    fun refreshModels(preferFastModel: Boolean = false) {
        scope.launch {
            bannerError = null
            runCatching { inspectHostTelemetry(config.hostUrl) }
                .onSuccess { info ->
                    models.clear()
                    models.addAll(info.models)
                    if (preferFastModel && info.models.isNotEmpty()) {
                        selectedModel = info.activeModel
                            ?: info.models.firstOrNull { it.contains("3b", true) || it.contains("4b", true) || it.contains("1.5b", true) }
                            ?: info.models.first()
                    } else if (selectedModel == null || selectedModel !in models) {
                        selectedModel = info.activeModel ?: models.firstOrNull()
                    }
                    val vramPart = info.vramSizeMb?.let { " • VRAM ${it}MB" }.orEmpty()
                    statusNote = "Ready (${selectedModel ?: "none"})$vramPart"
                }
                .onFailure { err ->
                    bannerError = "Cannot reach ${normalizeHost(config.hostUrl)}: ${err.message ?: "connection error"}"
                }
        }
    }

    fun applyBalancedMaxSpeed() {
        config = config.copy(
            cpuCores = 5,
            gpuLayers = 99,
            maxTokens = 512,
            contextSize = 1536,
            temperature = 0.55f,
            topP = 0.85f,
            keepAlive = "15m",
            conciseDiffMode = true,
            lineCheckpointTracker = true
        )
        saveConfig(context, config)
        refreshModels(preferFastModel = true)
        statusNote = "Max Speed tuned safely (CPU 5, GPU 99, ctx 1536, max 512)"
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
        stopAllAudio()
        streamJob?.cancel()
        streaming = false
        userScrolledAway = false
        activeThreadId = UUID.randomUUID().toString()
        messages.clear()
        attachment = null
        bannerError = null
    }

    fun openSavedThread(thread: SavedThread) {
        saveCurrentThreadSnapshot()
        stopAllAudio()
        streamJob?.cancel()
        streaming = false
        userScrolledAway = false
        activeThreadId = thread.id
        messages.clear()
        messages.addAll(thread.messages)
        if (thread.model != null) selectedModel = thread.model
        historyOpen = false
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            attachment = inspectAttachment(context, uri)
        }
    }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            attachment = inspectAttachment(context, uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val uri = pendingPhotoUri
        if (taken && uri != null) {
            attachment = inspectAttachment(context, uri)
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
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userScrolledAway = true
        }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && !userScrolledAway) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    fun sendPrompt() {
        val rawText = input.trim()
        val att = attachment
        if (rawText.isEmpty() && att == null) return

        val modelToUse = selectedModel ?: models.firstOrNull() ?: "qwen2.5-coder:3b"
        val combinedPrompt = buildString {
            if (config.singleHtmlAppMode) {
                append("[Format Rule: Output ONE complete single-file HTML/CSS/JS app instead of splitting into many files.]\n")
            }
            if (config.conciseDiffMode) {
                append("[Format Rule: For code, state exact target file/path (such as .cpp/.hpp/.html) and show only changed or essential lines with brief line notes; keep normal chat outside code natural and concise.]\n")
            }
            if (rawText.isNotEmpty()) {
                append("\n").append(rawText)
            }
            if (att != null && !att.isImage && att.extractedText.isNotBlank()) {
                append("\n\n--- File: ${att.name} ---\n${att.extractedText}\n--- End File ---")
            }
        }.trim()

        val displayText = when {
            rawText.isNotEmpty() -> rawText
            att?.isImage == true -> "Photo: ${att.name}"
            att != null -> "File: ${att.name}"
            else -> ""
        }

        userScrolledAway = false
        messages.add(
            ChatEntry(
                id = UUID.randomUUID().toString(),
                role = "user",
                content = displayText,
                attachmentName = att?.name,
                isAttachmentImage = att?.isImage == true
            )
        )
        val assistantIdx = messages.size
        val assistantId = UUID.randomUUID().toString()
        messages.add(ChatEntry(id = assistantId, role = "assistant", content = ""))

        val imagePayload = att?.base64Image
        input = ""
        attachment = null
        bannerError = null
        checkpointLine = 1
        streaming = true
        saveCurrentThreadSnapshot()

        streamJob = scope.launch {
            var completedCleanly = false
            runCatching {
                withContext(Dispatchers.IO) {
                    val endpoint = URL("${normalizeHost(config.hostUrl)}/api/generate")
                    val optionsJson = JSONObject().apply {
                        put("num_thread", config.cpuCores)
                        put("num_gpu", config.gpuLayers)
                        put("num_predict", config.maxTokens)
                        put("num_ctx", config.contextSize)
                        put("temperature", config.temperature)
                        put("top_p", config.topP)
                    }
                    val payload = JSONObject().apply {
                        put("model", modelToUse)
                        put("prompt", combinedPrompt.ifBlank { "Describe this image." })
                        put("stream", true)
                        put("keep_alive", config.keepAlive)
                        put("options", optionsJson)
                        if (imagePayload != null) {
                            put("images", JSONArray().put(imagePayload))
                        }
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
                            val errBody = runCatching {
                                conn.errorStream?.bufferedReader()?.use { it.readText() }
                            }.getOrNull()
                            withContext(Dispatchers.Main) {
                                bannerError = "Ollama HTTP ${conn.responseCode}: ${errBody ?: "request failed"}"
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
                                    val lineCount = snapshot.count { it == '\n' } + 1
                                    val stepCheckpoint = formatLineCheckpoint(lineCount)
                                    withContext(Dispatchers.Main) {
                                        checkpointLine = stepCheckpoint
                                        if (assistantIdx in messages.indices) {
                                            messages[assistantIdx] = messages[assistantIdx].copy(content = snapshot)
                                        }
                                    }
                                }
                                if (chunk.optBoolean("done", false)) {
                                    completedCleanly = true
                                    break
                                }
                            }
                        }
                    } finally {
                        conn.disconnect()
                    }
                }
            }.onFailure { err ->
                if (err !is kotlinx.coroutines.CancellationException) {
                    bannerError = err.message ?: "Generation stopped with an error"
                }
            }

            if (completedCleanly && assistantIdx in messages.indices) {
                val finalText = messages[assistantIdx].content
                val (summary, zipPath) = writeFilesAndOptionalZip(context, finalText, config.autoBundleZip)
                messages[assistantIdx] = messages[assistantIdx].copy(
                    changeSummary = summary,
                    bundleZipPath = zipPath
                )
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
                    IconButton(onClick = { applyBalancedMaxSpeed() }) {
                        Icon(Icons.Default.Bolt, contentDescription = "Fast safe performance", tint = WarmPeach)
                    }
                    IconButton(onClick = { historyOpen = true }) {
                        Icon(Icons.Default.History, contentDescription = "Chat history")
                    }
                    IconButton(onClick = { refreshModels(preferFastModel = false) }) {
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

            if (streaming) {
                Surface(
                    color = Color(0xFF171E2E),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .border(1.dp, Color(0xFF2D4366), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = WarmPeach
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        val lineLabel = if (config.lineCheckpointTracker) "Line $checkpointLine • " else ""
                        Text(
                            text = "${lineLabel}${elapsedSec}s / ~${estimatedTotalSec}s est.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else if (statusNote != null) {
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
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onToggleSpeak = {
                                if (isSpeakingThis) {
                                    stopAllAudio()
                                } else if (msg.content.isNotBlank()) {
                                    stopAllAudio()
                                    if (config.voiceServerUrl.isNotBlank()) {
                                        speakingMessageId = msg.id
                                        scope.launch {
                                            runCatching {
                                                fetchClonedAudioFile(
                                                    context = context,
                                                    serverUrl = config.voiceServerUrl,
                                                    text = msg.content,
                                                    voiceClipName = config.voiceClipName
                                                )
                                            }.onSuccess { wavFile ->
                                                if (speakingMessageId == msg.id) {
                                                    runCatching {
                                                        mediaPlayer.reset()
                                                        mediaPlayer.setDataSource(wavFile.absolutePath)
                                                        mediaPlayer.setOnCompletionListener {
                                                            speakingMessageId = null
                                                        }
                                                        mediaPlayer.prepare()
                                                        mediaPlayer.start()
                                                    }.onFailure {
                                                        speakingMessageId = null
                                                        bannerError = "Audio playback error: ${it.message}"
                                                    }
                                                }
                                            }.onFailure { err ->
                                                speakingMessageId = null
                                                bannerError = "Voice server error: ${err.message}"
                                            }
                                        }
                                    } else if (ttsReady) {
                                        resolveVoice(tts, config.preferredVoiceName)?.let { tts.voice = it }
                                        tts.setSpeechRate(config.speechRate)
                                        tts.setPitch(config.speechPitch)
                                        tts.speak(msg.content, TextToSpeech.QUEUE_FLUSH, null, msg.id)
                                        speakingMessageId = msg.id
                                    }
                                }
                            },
                            onDownloadZipToDownloads = { zipPath ->
                                val savedLabel = saveZipDirectToDownloads(context, zipPath)
                                if (savedLabel != null) {
                                    statusNote = "Downloaded to $savedLabel"
                                    Toast.makeText(context, "Saved to $savedLabel", Toast.LENGTH_SHORT).show()
                                } else {
                                    bannerError = "Could not write ZIP to Downloads"
                                }
                            },
                            onSaveZipAs = { zipPath ->
                                pendingZipExportPath = zipPath
                                createZipDocLauncher.launch(File(zipPath).name)
                            },
                            onShareZip = { zipPath ->
                                shareZipFile(context, zipPath)
                            }
                        )
                    }
                }
            }

            attachment?.let { att ->
                AttachmentPreviewCard(
                    item = att,
                    onRemove = { attachment = null }
                )
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
                    Box {
                        IconButton(onClick = { plusMenuOpen = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add photo or file",
                                tint = SkyBlue
                            )
                        }
                        DropdownMenu(
                            expanded = plusMenuOpen,
                            onDismissRequest = { plusMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Take photo") },
                                leadingIcon = {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = WarmPeach)
                                },
                                onClick = {
                                    plusMenuOpen = false
                                    val uri = createTempPhotoUri(context)
                                    if (uri != null) {
                                        pendingPhotoUri = uri
                                        cameraLauncher.launch(uri)
                                    } else {
                                        bannerError = "Camera FileProvider not configured in AndroidManifest.xml"
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Photos") },
                                leadingIcon = {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = WarmPeach)
                                },
                                onClick = {
                                    plusMenuOpen = false
                                    galleryPicker.launch("image/*")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Files / PDF") },
                                leadingIcon = {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = SkyBlue)
                                },
                                onClick = {
                                    plusMenuOpen = false
                                    filePicker.launch(arrayOf("*/*"))
                                }
                            )
                        }
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
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3E241C))
                                .clickable {
                                    streamJob?.cancel()
                                    streaming = false
                                    statusNote = "Cancelled at line $checkpointLine"
                                    saveCurrentThreadSnapshot()
                                }
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(34.dp),
                                strokeWidth = 2.dp,
                                color = Terracotta
                            )
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel generation",
                                tint = WarmPeach,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        val ready = input.isNotBlank() || attachment != null
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
        var gpuVal by rememberSaveable { mutableStateOf(config.gpuLayers.toFloat()) }
        var maxTokensText by rememberSaveable { mutableStateOf(config.maxTokens.toString()) }
        var ctxText by rememberSaveable { mutableStateOf(config.contextSize.toString()) }
        var tempVal by rememberSaveable { mutableStateOf(config.temperature) }
        var topPVal by rememberSaveable { mutableStateOf(config.topP) }
        var keepAliveDraft by rememberSaveable { mutableStateOf(config.keepAlive) }
        var conciseFlag by rememberSaveable { mutableStateOf(config.conciseDiffMode) }
        var singleHtmlFlag by rememberSaveable { mutableStateOf(config.singleHtmlAppMode) }
        var zipFlag by rememberSaveable { mutableStateOf(config.autoBundleZip) }
        var checkpointFlag by rememberSaveable { mutableStateOf(config.lineCheckpointTracker) }
        var rateVal by rememberSaveable { mutableStateOf(config.speechRate) }
        var pitchVal by rememberSaveable { mutableStateOf(config.speechPitch) }
        var selectedVoiceDraft by rememberSaveable { mutableStateOf(config.preferredVoiceName) }
        var voiceServerDraft by rememberSaveable { mutableStateOf(config.voiceServerUrl) }
        var voiceClipDraft by rememberSaveable { mutableStateOf(config.voiceClipName) }
        var voiceMenuOpen by rememberSaveable { mutableStateOf(false) }

        val audioClipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                val pickedName = queryDisplayName(context, uri)
                    ?: uri.lastPathSegment?.substringAfterLast('/')
                    ?: "reference_voice.wav"
                voiceClipDraft = pickedName
            }
        }

        val voicesList = remember(ttsReady, settingsOpen) {
            if (ttsReady) availableEnglishVoices(tts) else emptyList()
        }

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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { scanForLaptop() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (scanningLan) "Scanning..." else "Auto-IP")
                        }
                        OutlinedButton(
                            onClick = {
                                applyBalancedMaxSpeed()
                                coresVal = config.cpuCores.toFloat()
                                gpuVal = config.gpuLayers.toFloat()
                                maxTokensText = config.maxTokens.toString()
                                ctxText = config.contextSize.toString()
                                tempVal = config.temperature
                                topPVal = config.topP
                                keepAliveDraft = config.keepAlive
                                conciseFlag = config.conciseDiffMode
                                checkpointFlag = config.lineCheckpointTracker
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fast Safe")
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "CPU Threads (Balanced): ${coresVal.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = coresVal,
                        onValueChange = { coresVal = it },
                        valueRange = 1f..16f,
                        steps = 14
                    )

                    Text(
                        text = "GPU Offload Layers (num_gpu): ${gpuVal.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = gpuVal,
                        onValueChange = { gpuVal = it },
                        valueRange = 0f..100f
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

                    OutlinedTextField(
                        value = keepAliveDraft,
                        onValueChange = { keepAliveDraft = it },
                        label = { Text("Keep Model Loaded (keep_alive)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Temperature: ${"%.2f".format(tempVal)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = tempVal,
                        onValueChange = { tempVal = it },
                        valueRange = 0.0f..1.5f
                    )

                    Text(
                        text = "Top-P: ${"%.2f".format(topPVal)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = topPVal,
                        onValueChange = { topPVal = it },
                        valueRange = 0.1f..1.0f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Changed Lines Only Mode", fontWeight = FontWeight.Medium)
                            Text(
                                "Names target file & outputs only needed lines",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = conciseFlag, onCheckedChange = { conciseFlag = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Single HTML Web App Mode", fontWeight = FontWeight.Medium)
                            Text(
                                "Packs web apps into one HTML file instead of many files",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = singleHtmlFlag, onCheckedChange = { singleHtmlFlag = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-ZIP Multi-File Output", fontWeight = FontWeight.Medium)
                            Text(
                                "Saves code blocks into files & zips 2+ files automatically",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = zipFlag, onCheckedChange = { zipFlag = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Line Checkpoint Status (1, 10, 20...)", fontWeight = FontWeight.Medium)
                            Text(
                                "Shows live line checkpoints & time estimate while generating",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = checkpointFlag, onCheckedChange = { checkpointFlag = it })
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = voiceServerDraft,
                        onValueChange = { voiceServerDraft = it },
                        label = { Text("Voice Server URL (optional /tts)") },
                        placeholder = { Text("Leave blank for Android TTS") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = voiceClipDraft,
                        onValueChange = { voiceClipDraft = it },
                        label = { Text("Reference Voice Clip Name") },
                        placeholder = { Text("e.g. reference_voice.wav") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = { audioClipPicker.launch(arrayOf("audio/*")) },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Voice Clip File")
                    }

                    Text(
                        text = "Installed TTS Voice",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Box {
                        OutlinedButton(
                            onClick = { voiceMenuOpen = true },
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text(
                                text = if (selectedVoiceDraft.isBlank()) "Auto (Best Installed English Voice)" else selectedVoiceDraft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = voiceMenuOpen, onDismissRequest = { voiceMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Auto (Best Installed English Voice)") },
                                onClick = {
                                    selectedVoiceDraft = ""
                                    voiceMenuOpen = false
                                }
                            )
                            voicesList.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text("${v.name} (${v.locale})") },
                                    onClick = {
                                        selectedVoiceDraft = v.name
                                        voiceMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = "Voice Speed: ${"%.2f".format(rateVal)}x",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = rateVal,
                        onValueChange = { rateVal = it },
                        valueRange = 0.75f..1.35f
                    )

                    Text(
                        text = "Voice Pitch: ${"%.2f".format(pitchVal)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = pitchVal,
                        onValueChange = { pitchVal = it },
                        valueRange = 0.85f..1.15f
                    )

                    OutlinedButton(
                        onClick = {
                            if (ttsReady) {
                                resolveVoice(tts, selectedVoiceDraft)?.let { tts.voice = it }
                                tts.setSpeechRate(rateVal)
                                tts.setPitch(pitchVal)
                                tts.speak(
                                    "Testing voice speed and pitch for calm, natural playback.",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "voice_preview"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Voice Sample")
                    }
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
                            gpuLayers = gpuVal.toInt().coerceIn(0, 200),
                            maxTokens = parsedMax,
                            contextSize = parsedCtx,
                            temperature = tempVal,
                            topP = topPVal,
                            keepAlive = keepAliveDraft.trim().ifBlank { "15m" },
                            conciseDiffMode = conciseFlag,
                            singleHtmlAppMode = singleHtmlFlag,
                            autoBundleZip = zipFlag,
                            lineCheckpointTracker = checkpointFlag,
                            speechRate = rateVal,
                            speechPitch = pitchVal,
                            preferredVoiceName = selectedVoiceDraft,
                            voiceServerUrl = voiceServerDraft.trim(),
                            voiceClipName = voiceClipDraft.trim()
                        )
                        saveConfig(context, config)
                        if (ttsReady) {
                            resolveVoice(tts, config.preferredVoiceName)?.let { tts.voice = it }
                            tts.setSpeechRate(config.speechRate)
                            tts.setPitch(config.speechPitch)
                        }
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
private fun AttachmentPreviewCard(
    item: AttachmentPreview,
    onRemove: () -> Unit
) {
    Surface(
        color = Color(0xFF171E2E),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .border(1.dp, Color(0xFF344B73), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.isImage && item.bitmap != null) {
                Image(
                    bitmap = item.bitmap.asImageBitmap(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Surface(
                    color = Color(0xFF212B40),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = SkyBlue,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.mimeType} • ${item.byteCount} B",
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item.isImage && item.extractedText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.extractedText.take(180),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove attachment",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
        "Write clean C++ or Kotlin code and show only changed lines",
        "Build a single-file HTML/JS web app"
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
            text = "Pick a prompt, tap + for photo/file, or type below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            starters.forEach { text ->
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
    onToggleSpeak: () -> Unit,
    onDownloadZipToDownloads: (String) -> Unit,
    onSaveZipAs: (String) -> Unit,
    onShareZip: (String) -> Unit
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
                .widthIn(max = 340.dp)
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
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (entry.attachmentName != null) {
                    Text(
                        text = if (entry.isAttachmentImage) "Photo: ${entry.attachmentName}" else "File: ${entry.attachmentName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = entry.content.ifEmpty { "…" },
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!isUser && entry.changeSummary != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFF323746))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "What changed: ${entry.changeSummary}",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmPeach
                    )
                    if (entry.bundleZipPath != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFF192234),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF354B75), RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.FolderZip,
                                        contentDescription = null,
                                        tint = SkyBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = File(entry.bundleZipPath).name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = SkyBlue,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onDownloadZipToDownloads(entry.bundleZipPath) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Terracotta,
                                        contentColor = Color(0xFF140B07)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download to Downloads", fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onSaveZipAs(entry.bundleZipPath) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save As")
                                    }
                                    OutlinedButton(
                                        onClick = { onShareZip(entry.bundleZipPath) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (entry.content.isNotBlank()) {
            Row(
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onToggleSpeak,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Icon(
                        if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isSpeaking) "Stop TTS" else "Listen",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
