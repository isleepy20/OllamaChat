package com.example.ollamachat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ollamachat.data.ModelInfo
import com.example.ollamachat.data.formatByteSize

@Composable
fun SettingsDialog(
    state: UiState,
    vm: ChatViewModel,
    modelInventory: List<ModelInfo> = emptyList(),
    initialTtsUrl: String = "http://127.0.0.1:8020",
    initialVoiceCloneEnabled: Boolean = false,
    initialVoiceSample: String = "default",
    onSaveTtsSettings: (url: String, enabled: Boolean, sample: String) -> Unit = { _, _, _ -> }
) {
    var url by remember { mutableStateOf(state.baseUrl) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var prompt by remember { mutableStateOf(state.systemPrompt) }
    var pullName by remember { mutableStateOf("") }
    var ttsUrl by remember { mutableStateOf(initialTtsUrl) }
    var voiceCloneEnabled by remember { mutableStateOf(initialVoiceCloneEnabled) }
    var voiceSample by remember { mutableStateOf(initialVoiceSample) }
    val pull = state.pull

    AlertDialog(
        onDismissRequest = vm::closeSettings,
        containerColor = Color(0xFF111722),
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Settings") },
        confirmButton = {
            TextButton(onClick = {
                onSaveTtsSettings(ttsUrl.trim(), voiceCloneEnabled, voiceSample.trim())
                vm.closeSettings()
            }) {
                Text("Close")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Server", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; urlError = null },
                    label = { Text("Base URL") },
                    placeholder = { Text("http://192.168.1.20:11434") },
                    singleLine = true,
                    isError = urlError != null,
                    supportingText = {
                        Text(urlError ?: "Use http://127.0.0.1:11434 for local or http://<HOST-IP>:11434.")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = {
                    urlError = vm.saveBaseUrl(url)
                    if (urlError == null) url = vm.state.value.baseUrl
                }) {
                    Text("Save & connect")
                }

                Text(
                    "Local TTS & Voice Clone",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = ttsUrl,
                    onValueChange = { ttsUrl = it },
                    label = { Text("TTS Server URL") },
                    placeholder = { Text("http://127.0.0.1:8020") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Voice Cloning / TTS", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = voiceCloneEnabled,
                        onCheckedChange = { checked ->
                            voiceCloneEnabled = checked
                            onSaveTtsSettings(ttsUrl.trim(), checked, voiceSample.trim())
                        }
                    )
                }
                OutlinedTextField(
                    value = voiceSample,
                    onValueChange = { voiceSample = it },
                    label = { Text("Voice Sample / Preset") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = {
                    onSaveTtsSettings(ttsUrl.trim(), voiceCloneEnabled, voiceSample.trim())
                }) {
                    Text("Save TTS settings")
                }

                Text(
                    "System prompt (optional)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                OutlinedButton(onClick = { vm.saveSystemPrompt(prompt) }) {
                    Text("Save prompt")
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Installed models", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    if (state.loadingModels) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = { vm.refreshModels() }) { Text("Refresh models") }
                    }
                }
                state.connectionError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (modelInventory.isNotEmpty()) {
                    modelInventory.forEach { info ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.selectModel(info.name) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = info.name == state.selectedModel,
                                onClick = { vm.selectModel(info.name) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(info.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${info.source.name} • Size: ${info.sizeLabel()} • Est. Mem: ${info.memoryLabel()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else {
                    state.models.forEach { name ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.selectModel(name) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = name == state.selectedModel, onClick = { vm.selectModel(name) })
                            Text(name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Text(
                    "Download a model",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = pullName,
                    onValueChange = { pullName = it },
                    label = { Text("Model name") },
                    placeholder = { Text("llama3.2 or qwen2.5:14b") },
                    singleLine = true,
                    enabled = !pull.active,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.pullModel(pullName) }, enabled = !pull.active) {
                        Text("Download / Pull")
                    }
                    if (pull.active) {
                        OutlinedButton(onClick = vm::cancelPull) { Text("Cancel") }
                    }
                }
                if (pull.active || pull.status.isNotEmpty()) {
                    val fraction = pull.fraction
                    if (pull.active) {
                        if (fraction != null) {
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    val pct = if (pull.active && fraction != null) {
                        " — ${(fraction * 100).toInt()}%"
                    } else {
                        ""
                    }
                    Text(pull.status + pct, style = MaterialTheme.typography.bodySmall)
                }
                pull.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                // Bottom summary block so Settings doesn't leave dead space at the bottom
                val totalDisk = remember(modelInventory) { modelInventory.sumOf { it.sizeBytes } }
                val totalMem = remember(modelInventory) { modelInventory.sumOf { it.estimatedMemoryBytes } }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2232))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Local Runtime Status", style = MaterialTheme.typography.titleSmall)
                        Text("Indexed Models: ${if (modelInventory.isNotEmpty()) modelInventory.size else state.models.size}", style = MaterialTheme.typography.bodySmall)
                        Text("Total Disk Used: ${formatByteSize(totalDisk)}", style = MaterialTheme.typography.bodySmall)
                        Text("Total Est. Memory: ${formatByteSize(totalMem)}", style = MaterialTheme.typography.bodySmall)
                        Text("TTS Endpoint: $ttsUrl (${if (voiceCloneEnabled) "Clone ON" else "Clone OFF"})", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    )
}
