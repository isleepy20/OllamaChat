package com.example.ollamachat.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object UrlUtils {
    const val DEFAULT_BASE_URL = "http://127.0.0.1:11434"

    /**
     * Trims whitespace, adds http:// if no scheme was typed, strips trailing slashes.
     * Returns null if the result is not a valid http(s) URL.
     */
    fun normalizeBaseUrl(raw: String): String? {
        var s = raw.trim()
        if (s.isEmpty()) return null
        if (!s.contains("://")) s = "http://$s"
        s = s.trimEnd('/')
        val parsed = s.toHttpUrlOrNull() ?: return null
        if (parsed.scheme != "http" && parsed.scheme != "https") return null
        return s
    }

    /** Joins a normalized base URL and a path such as "api/tags" with exactly one slash. */
    fun endpoint(base: String, path: String): String =
        base.trimEnd('/') + "/" + path.trimStart('/')
}
