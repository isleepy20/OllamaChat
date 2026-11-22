package com.example.ollamachat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDialog(state: UiState, vm: ChatViewModel) {
    var url by remember { mutableStateOf(state.baseUrl) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var prompt by remember { mutableStateOf(state.systemPrompt) }
    var pullName by remember { mutableStateOf("") }
    val pull = state.pull

    AlertDialog(
        onDismissRequest = vm::closeSettings,
        title = { Text("Settings") },
        confirmButton = { TextButton(onClick = vm::closeSettings) { Text("Close") } },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ---- Server ----
                Text("Ollama server", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; urlError = null },
                    label = { Text("Base URL") },
                    placeholder = { Text("http://192.168.1.20:11434") },
                    singleLine = true,
                    isError = urlError != null,
                    supportingText = { Text(urlError ?: "Use http://<HOST-IP>:11434 for a PC on your network.") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = {
                    urlError = vm.saveBaseUrl(url)
                    if (urlError == null) url = vm.state.value.baseUrl
                }) { Text("Save & connect") }

                // ---- System prompt ----
                Text("System prompt (optional)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                OutlinedButton(onClick = { vm.saveSystemPrompt(prompt) }) { Text("Save prompt") }

                // ---- Installed models ----
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Installed models", style = MaterialTheme.typography.titleSmall)
                    if (state.loadingModels) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = { vm.refreshModels() }) { Text("Refresh models") }
                    }
                }
                state.connectionError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                state.models.forEach { name ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.selectModel(name) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = name == state.selectedModel, onClick = { vm.selectModel(name) })
                        Text(name, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // ---- Pull ----
                Text("Download a model", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
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
                    Button(onClick = { vm.pullModel(pullName) }, enabled = !pull.active) { Text("Download / Pull") }
                    if (pull.active) {
                        OutlinedButton(onClick = vm::cancelPull) { Text("Cancel") }
                    }
                }
                if (pull.active || pull.status.isNotEmpty()) {
                    val fraction = pull.fraction
                    if (pull.active) {
                        if (fraction != null) LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                        else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    val pct = if (pull.active && fraction != null) " — ${(fraction * 100).toInt()}%" else ""
                    Text(pull.status + pct, style = MaterialTheme.typography.bodySmall)
                }
                pull.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    )
}
