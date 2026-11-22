package com.example.ollamachat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL

enum class RunTarget {
    PHONE_LOCAL,
    LAPTOP
}

data class DetectedNetworkEndpoints(
    val phoneIpv4: String? = null,
    val laptopIpv4: String? = null,
    val laptopOnline: Boolean = false
)

suspend fun detectPhoneIpv4(): String? = withContext(Dispatchers.IO) {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@withContext null
        for (intf in interfaces.toList()) {
            if (!intf.isUp || intf.isLoopback) continue
            for (addr in intf.inetAddresses.toList()) {
                val ip = addr.hostAddress
                if (!addr.isLoopbackAddress && addr is Inet4Address && !ip.isNullOrBlank() && !ip.startsWith("169.254.") && ip != "127.0.0.1") {
                    return@withContext ip
                }
            }
        }
        null
    } catch (_: Exception) {
        null
    }
}

suspend fun isOllamaHostReachable(
    host: String,
    port: Int = 11434,
    timeoutMs: Int = 500
): Boolean = withContext(Dispatchers.IO) {
    val cleanHost = host.trim()
        .removePrefix("http://")
        .removePrefix("https://")
        .substringBefore("/")
        .substringBefore(":")
    if (cleanHost.isEmpty()) return@withContext false

    var conn: HttpURLConnection? = null
    return@withContext try {
        val url = URL("http://$cleanHost:$port/api/tags")
        conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = false
        }
        val code = conn.responseCode
        code in 200..299
    } catch (_: Exception) {
        false
    } finally {
        conn?.disconnect()
    }
}

suspend fun discoverLaptopOllamaIp(
    phoneIp: String?,
    preferredLaptopIp: String? = null,
    port: Int = 11434
): String? = withContext(Dispatchers.IO) {
    withTimeoutOrNull(4_000L) {
        val candidates = linkedSetOf<String>()
        preferredLaptopIp?.trim()?.takeIf { it.isNotBlank() && it != phoneIp }?.let(candidates::add)

        if (!phoneIp.isNullOrBlank()) {
            val parts = phoneIp.split(".")
            if (parts.size == 4) {
                val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
                val selfLastOctet = parts[3].toIntOrNull()
                for (i in 1..254) {
                    if (i == selfLastOctet) continue
                    candidates.add("$prefix.$i")
                }
            }
        }

        if (candidates.isEmpty()) return@withTimeoutOrNull null

        coroutineScope {
            for (batch in candidates.chunked(32)) {
                val found = batch.map { ip ->
                    async {
                        if (isOllamaHostReachable(ip, port = port, timeoutMs = 400)) ip else null
                    }
                }.awaitAll().firstOrNull { it != null }
                if (found != null) return@coroutineScope found
            }
            null
        }
    }
}

fun resolveActiveOllamaUrl(
    target: RunTarget,
    detectedLaptopIp: String?,
    manualLaptopIp: String,
    port: Int = 11434
): String? = when (target) {
    RunTarget.PHONE_LOCAL -> "http://127.0.0.1:$port"
    RunTarget.LAPTOP -> {
        val laptopHost = detectedLaptopIp?.trim()?.takeIf { it.isNotBlank() }
            ?: manualLaptopIp.trim().takeIf { it.isNotBlank() }
        laptopHost?.let { "http://$it:$port" }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalComputeTargetSelector(
    runTarget: RunTarget,
    phoneIp: String?,
    laptopIp: String?,
    laptopOnline: Boolean,
    isScanning: Boolean = false,
    onSelectTarget: (RunTarget) -> Unit,
    onRefreshIps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = runTarget == RunTarget.PHONE_LOCAL,
                onClick = { onSelectTarget(RunTarget.PHONE_LOCAL) },
                label = { Text("Phone Local") }
            )
            FilterChip(
                selected = runTarget == RunTarget.LAPTOP,
                onClick = { onSelectTarget(RunTarget.LAPTOP) },
                label = {
                    Text(
                        when {
                            laptopOnline -> "Laptop (Online)"
                            runTarget == RunTarget.LAPTOP -> "Laptop (Bypassed)"
                            else -> "Laptop (Offline)"
                        }
                    )
                }
            )
            TextButton(
                onClick = onRefreshIps,
                enabled = !isScanning
            ) {
                Text(if (isScanning) "Scanning..." else "Detect IPs")
            }
        }
        Text(
            text = "Phone IP: ${phoneIp ?: "127.0.0.1 (Local)"}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Laptop IP: ${if (laptopOnline && !laptopIp.isNullOrBlank()) laptopIp else "Not detected (bypassed)"}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

suspend fun sendPromptWithLocalFallback(
    prompt: String,
    runTarget: RunTarget,
    detectedLaptopIp: String?,
    manualLaptopIp: String,
    onFallbackToPhoneLocal: () -> Unit,
    onStatusMessage: (String) -> Unit,
    callOllamaChat: suspend (baseUrl: String, prompt: String) -> String
): String? {
    val activeUrl = resolveActiveOllamaUrl(
        target = runTarget,
        detectedLaptopIp = detectedLaptopIp,
        manualLaptopIp = manualLaptopIp
    )

    if (runTarget == RunTarget.LAPTOP) {
        val reachable = activeUrl != null && isOllamaHostReachable(activeUrl)
        if (!reachable) {
            onFallbackToPhoneLocal()
            val phoneUrl = "http://127.0.0.1:11434"
            return try {
                callOllamaChat(phoneUrl, prompt)
            } catch (_: Exception) {
                onStatusMessage("Local phone engine not running on 127.0.0.1:11434")
                null
            }
        }
    }

    val targetUrl = activeUrl ?: "http://127.0.0.1:11434"
    return try {
        callOllamaChat(targetUrl, prompt)
    } catch (_: Exception) {
        if (runTarget == RunTarget.LAPTOP) {
            onFallbackToPhoneLocal()
            onStatusMessage("Laptop offline — switched to Phone Local")
        } else {
            onStatusMessage("Local engine offline on $targetUrl")
        }
        null
    }
}
