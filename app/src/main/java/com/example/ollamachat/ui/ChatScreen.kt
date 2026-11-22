package com.example.ollamachat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ollamachat.data.ChatMessage

private val ClaudeTerracotta = Color(0xFFD97757)
private val ClaudeWarmAccent = Color(0xFFE08E6D)
private val GeminiBlue = Color(0xFF8AB4F8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val lastLength = state.messages.lastOrNull()?.content?.length

    LaunchedEffect(state.messages.size, lastLength) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.lastIndex)
    }

    val hybridBackdrop = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF161316),
            Color(0xFF0E1016),
            Color(0xFF0A0B0F)
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF12131A),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(GeminiBlue, ClaudeTerracotta))
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
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
                    IconButton(onClick = { vm.refreshModels() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh models")
                    }
                    IconButton(onClick = vm::clearChat) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear chat")
                    }
                    IconButton(onClick = vm::openSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .background(hybridBackdrop)
                .padding(padding)
                .navigationBarsPadding()
                .imePadding()
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ModelSelector(
                    models = state.models,
                    selected = state.selectedModel,
                    onSelect = vm::selectModel
                )
            }

            HorizontalDivider(color = Color(0xFF232734), thickness = 1.dp)

            state.connectionError?.let { Banner(it, onDismiss = null) }
            state.chatError?.let { Banner(it, onDismiss = vm::dismissChatError) }

            if (state.messages.isEmpty()) {
                HybridEmptyState(
                    onPickStarter = { starter -> input = starter },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.messages, key = { it.id }) { MessageBubble(it) }
                }
            }

            Surface(
                color = Color(0xFF161922),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF33486B), Color(0xFF5A372B))
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            cursorColor = ClaudeWarmAccent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (state.streaming) {
                        IconButton(
                            onClick = vm::stopStreaming,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3E241C))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Stop",
                                tint = ClaudeWarmAccent
                            )
                        }
                    } else {
                        val canSend = input.isNotBlank()
                        IconButton(
                            onClick = { if (vm.sendMessage(input)) input = "" },
                            enabled = canSend,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) {
                                        Brush.linearGradient(listOf(GeminiBlue, ClaudeTerracotta))
                                    } else {
                                        Brush.linearGradient(listOf(Color(0xFF1F2330), Color(0xFF1F2330)))
                                    }
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (canSend) Color(0xFF0C0D10)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.settingsOpen) SettingsDialog(state, vm)
}

@Composable
private fun HybridEmptyState(
    onPickStarter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val starters = listOf(
        "Break down a tricky problem step by step",
        "Draft clean code and explain why it works",
        "Organize rough notes into a clear plan"
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
            text = "Pick a prompt or type directly below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))

        starters.forEachIndexed { index, prompt ->
            val accentBorder = if (index % 2 == 0) Color(0xFF2D4366) else Color(0xFF55352A)
            val accentBg = if (index % 2 == 0) Color(0xFF141C2B) else Color(0xFF1D1514)
            Surface(
                color = accentBg,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .border(1.dp, accentBorder, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onPickStarter(prompt) }
            ) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ModelSelector(models: List<String>, selected: String?, onSelect: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        Surface(
            color = Color(0xFF191D29),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.border(1.dp, Color(0xFF31394D), RoundedCornerShape(999.dp))
        ) {
            TextButton(
                onClick = { expanded = true },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (selected != null) ClaudeWarmAccent else GeminiBlue)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selected ?: "No model selected",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.widthIn(max = 250.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (models.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No installed models") },
                    onClick = { expanded = false },
                    enabled = false
                )
            }
            models.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(name)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun Banner(message: String, onDismiss: (() -> Unit)?) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) Color(0xFF1B2E4B) else Color(0xFF17161B),
            contentColor = if (isUser) Color(0xFFEAF2FF) else Color(0xFFF3EFEA),
            shape = if (isUser) {
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp)
            } else {
                RoundedCornerShape(16.dp)
            },
            modifier = Modifier
                .widthIn(max = 325.dp)
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
            Text(
                text = message.content.ifEmpty { "…" },
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
