package com.example.ollamachat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val GeminiDarkScheme = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF062043),
    primaryContainer = Color(0xFF1A365D),
    onPrimaryContainer = Color(0xFFD2E3FC),
    secondary = Color(0xFFA8C7FA),
    onSecondary = Color(0xFF0B2246),
    secondaryContainer = Color(0xFF232834),
    onSecondaryContainer = Color(0xFFE3E8F2),
    background = Color(0xFF0C0F14),
    onBackground = Color(0xFFE8ECF4),
    surface = Color(0xFF0C0F14),
    onSurface = Color(0xFFE8ECF4),
    surfaceVariant = Color(0xFF181D27),
    onSurfaceVariant = Color(0xFFA6B0C3),
    outline = Color(0xFF2B3344)
)

data class UiChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean
)

@Composable
fun ChatScreen(
    messages: List<UiChatMessage>,
    inputText: String,
    isStreaming: Boolean,
    modelTitle: String = "Assistant",
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onSuggestionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    MaterialTheme(colorScheme = GeminiDarkScheme) {
        val bgBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF101622),
                Color(0xFF0C0F14),
                Color(0xFF090B0F)
            )
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(bgBrush)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopHeaderBar(
                    title = modelTitle,
                    onNewChat = onNewChat,
                    onOpenSettings = onOpenSettings
                )

                if (messages.isEmpty()) {
                    EmptyGeminiState(
                        onSuggestionClick = onSuggestionClick,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(items = messages, key = { it.id }, contentType = { it.isUser }) { msg ->
                            if (msg.isUser) {
                                UserPillBubble(text = msg.text)
                            } else {
                                AssistantCardBlock(text = msg.text)
                            }
                        }
                    }
                }

                GeminiComposerBar(
                    value = inputText,
                    isStreaming = isStreaming,
                    onValueChange = onInputChange,
                    onSend = onSend,
                    onStop = onStop
                )
            }
        }
    }
}

@Composable
private fun TopHeaderBar(
    title: String,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A2538)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF8AB4F8),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNewChat) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "New chat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyGeminiState(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompts = listOf(
        "Debug a bug step by step",
        "Summarize ideas cleanly",
        "Plan a quick workflow"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Ready when you are",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFD2E3FC)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Ask anything or start with a quick prompt.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        prompts.forEach { prompt ->
            AssistChip(
                onClick = { onSuggestionClick(prompt) },
                label = {
                    Text(
                        text = prompt,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFF171D29)
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = Color(0xFF283246)
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun UserPillBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            color = Color(0xFF1E2A3D),
            contentColor = Color(0xFFEAF1FC),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)
            )
        }
    }
}

@Composable
private fun AssistantCardBlock(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF172236)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF8AB4F8),
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            color = Color(0xFF131822),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .weight(1f)
                .border(1.dp, Color(0xFF20293A), RoundedCornerShape(18.dp))
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun GeminiComposerBar(
    value: String,
    isStreaming: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        color = Color(0xFF141A24),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .border(1.dp, Color(0xFF253044), RoundedCornerShape(28.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = "Message...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = Color(0xFF8AB4F8),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            val canSend = value.isNotBlank() && !isStreaming
            IconButton(
                onClick = if (isStreaming) onStop else onSend,
                enabled = isStreaming || canSend,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isStreaming -> Color(0xFF2B3548)
                            canSend -> Color(0xFF8AB4F8)
                            else -> Color(0xFF1D2534)
                        }
                    )
            ) {
                Icon(
                    imageVector = if (isStreaming) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.Send,
                    contentDescription = if (isStreaming) "Stop" else "Send",
                    tint = when {
                        isStreaming -> Color(0xFFE8ECF4)
                        canSend -> Color(0xFF062043)
                        else -> Color(0xFF647087)
                    },
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}
