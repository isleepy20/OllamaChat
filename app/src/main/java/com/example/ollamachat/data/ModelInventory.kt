package com.example.ollamachat.data

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

enum class ModelSource {
    OLLAMA,
    HUGGING_FACE_LOCAL
}

data class ModelInfo(
    val id: String,
    val name: String,
    val source: ModelSource,
    val sizeBytes: Long,
    val estimatedMemoryBytes: Long,
    val location: String
) {
    fun sizeLabel(): String = formatByteSize(sizeBytes)
    fun memoryLabel(): String = formatByteSize(estimatedMemoryBytes)
}

fun formatByteSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val power = (log10(bytes.toDouble()) / log10(1024.0))
        .toInt()
        .coerceIn(0, units.lastIndex)
    val value = bytes / 1024.0.pow(power.toDouble())
    return String.format(Locale.US, "%.2f %s", value, units[power])
}

class ModelInventory(
    private val ollamaBaseUrl: String = UrlUtils.DEFAULT_BASE_URL,
    private val localModelDirs: List<File> = emptyList()
) {
    private val supportedExtensions = setOf("gguf", "bin", "safetensors", "onnx", "pt")

    fun scanAllModels(): List<ModelInfo> {
        val items = mutableListOf<ModelInfo>()

        // 1. Ollama daemon models (/api/tags)
        try {
            val endpoint = URL(ollamaBaseUrl.trimEnd('/') + "/api/tags")
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2500
                readTimeout = 4000
            }
            if (connection.responseCode in 200..299) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val modelsArray = JSONObject(body).optJSONArray("models")
                if (modelsArray != null) {
                    for (i in 0 until modelsArray.length()) {
                        val obj = modelsArray.optJSONObject(i) ?: continue
                        val modelName = obj.optString("name", "unknown-model")
                        val sizeBytes = obj.optLong("size", 0L)
                        // ~1.2x weight size approximates runtime + KV cache memory footprint
                        val estMem = (sizeBytes * 1.20).toLong()
                        items.add(
                            ModelInfo(
                                id = "ollama:$modelName",
                                name = modelName,
                                source = ModelSource.OLLAMA,
                                sizeBytes = sizeBytes,
                                estimatedMemoryBytes = estMem,
                                location = ollamaBaseUrl
                            )
                        )
                    }
                }
            }
            connection.disconnect()
        } catch (_: Exception) {
            // Continue with local file scan if Ollama isn't listening yet
        }

        // 2. Local model files on phone (Ollama models/downloads & Hugging Face local folders)
        for (dir in localModelDirs) {
            if (!dir.exists() || !dir.isDirectory) continue
            dir.walkTopDown()
                .filter { it.isFile && it.extension.lowercase(Locale.US) in supportedExtensions }
                .forEach { file ->
                    val sizeBytes = file.length()
                    val estMem = (sizeBytes * 1.20).toLong()
                    val isHf = file.absolutePath.contains("huggingface", ignoreCase = true) ||
                        file.absolutePath.contains("hf", ignoreCase = true)
                    val source = if (isHf) ModelSource.HUGGING_FACE_LOCAL else ModelSource.OLLAMA
                    items.add(
                        ModelInfo(
                            id = "${source.name.lowercase(Locale.US)}:${file.absolutePath}",
                            name = file.name,
                            source = source,
                            sizeBytes = sizeBytes,
                            estimatedMemoryBytes = estMem,
                            location = file.absolutePath
                        )
                    )
                }
        }

        return items.distinctBy { it.id }
    }
}
