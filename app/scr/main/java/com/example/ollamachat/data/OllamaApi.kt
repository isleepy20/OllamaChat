package com.example.ollamachat.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/** Plain HTTP client for the Ollama REST API. No authentication headers are ever sent. */
class OllamaApi {

    private val quickClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // Streaming responses can pause for a long time (model loading), so no read timeout.
    private val streamClient = quickClient.newBuilder()
        .readTimeout(0, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS)
        .build()

    /** GET /api/tags -> installed model identifiers. */
    suspend fun listModels(baseUrl: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(UrlUtils.endpoint(baseUrl, "api/tags"))
                .get()
                .build()
            quickClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw OllamaException(httpError(resp))
                val body = resp.body?.string().orEmpty()
                val arr = JSONObject(body).optJSONArray("models") ?: JSONArray()
                val names = ArrayList<String>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("name").ifBlank { o.optString("model") }
                    if (id.isNotBlank()) names.add(id)
                }
                names.distinct().sorted()
            }
        } catch (e: IOException) {
            throw OllamaException(describe(e))
        } catch (e: JSONException) {
            throw OllamaException("The server replied, but not with valid Ollama JSON. Is this an Ollama server?")
        } catch (e: IllegalArgumentException) {
            throw OllamaException("Invalid server URL.")
        }
    }

    /** POST /api/pull (NDJSON stream) -> progress updates. Throws OllamaException on failure. */
    fun pull(baseUrl: String, model: String): Flow<PullProgress> {
        val body = JSONObject().put("model", model).put("stream", true)
        return streamLines(post(baseUrl, "api/pull", body)).mapNotNull { line ->
            val o = parseObject(line) ?: return@mapNotNull null
            if (o.has("error")) throw OllamaException(o.optString("error"))
            PullProgress(
                status = o.optString("status"),
                completed = o.optLong("completed", 0L),
                total = o.optLong("total", 0L)
            )
        }
    }

    /** POST /api/chat (NDJSON stream) -> content chunks. Throws OllamaException on failure. */
    fun chat(baseUrl: String, model: String, messages: List<ChatMessage>): Flow<ChatChunk> {
        val arr = JSONArray()
        messages.forEach { arr.put(JSONObject().put("role", it.role).put("content", it.content)) }
        val body = JSONObject().put("model", model).put("messages", arr).put("stream", true)
        return streamLines(post(baseUrl, "api/chat", body)).mapNotNull { line ->
            val o = parseObject(line) ?: return@mapNotNull null
            if (o.has("error")) throw OllamaException(o.optString("error"))
            val content = o.optJSONObject("message")?.optString("content").orEmpty()
            ChatChunk(content = content, done = o.optBoolean("done", false))
        }
    }

    private fun post(baseUrl: String, path: String, json: JSONObject): Request =
        try {
            Request.Builder()
                .url(UrlUtils.endpoint(baseUrl, path))
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
        } catch (e: IllegalArgumentException) {
            throw OllamaException("Invalid server URL.")
        }

    private fun parseObject(line: String): JSONObject? =
        try { JSONObject(line) } catch (e: JSONException) { null }

    /** Emits each non-blank line of the response body as it arrives; cancels the call if collection is cancelled. */
    private fun streamLines(request: Request): Flow<String> = flow {
        val call = streamClient.newCall(request)
        val handle = currentCoroutineContext()[Job]?.invokeOnCompletion { cause ->
            if (cause != null) call.cancel()
        }
        try {
            val response = try {
                call.execute()
            } catch (e: IOException) {
                throw OllamaException(describe(e))
            }
            response.use { resp ->
                if (!resp.isSuccessful) throw OllamaException(httpError(resp))
                val source = resp.body?.source() ?: throw OllamaException("Empty response from server.")
                while (true) {
                    var line: String? = null
                    try {
                        line = source.readUtf8Line()
                    } catch (e: IOException) {
                        throw OllamaException("Connection lost: " + describe(e))
                    }
                    if (line == null) break
                    if (line.isNotBlank()) emit(line)
                }
            }
        } finally {
            handle?.dispose()
        }
    }.flowOn(Dispatchers.IO)

    private fun httpError(resp: Response): String {
        val raw = try { resp.body?.string().orEmpty() } catch (e: IOException) { "" }
        val detail = try { JSONObject(raw).optString("error") } catch (e: JSONException) { "" }
        return if (detail.isNotBlank()) "Server error ${resp.code}: $detail" else "Server error: HTTP ${resp.code}"
    }

    private fun describe(e: IOException): String = when (e) {
        is SocketTimeoutException ->
            "Connection timed out. Check the address and that Ollama is running."
        is UnknownHostException ->
            "Unknown host. Check the server address."
        is ConnectException ->
            "Could not connect. Is Ollama running at this address? For a PC on your network, " +
                "use its LAN IP and start Ollama with OLLAMA_HOST=0.0.0.0."
        else -> "Network error: ${e.message ?: e.javaClass.simpleName}"
    }
}
