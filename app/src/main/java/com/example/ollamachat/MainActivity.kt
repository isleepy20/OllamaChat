package com.example.ollamachat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ollamachat.ui.ChatScreen
import com.example.ollamachat.ui.ChatViewModel

private val AppGeminiDarkScheme = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF062043),
    primaryContainer = Color(0xFF1D3557),
    onPrimaryContainer = Color(0xFFE8F1FF),
    secondary = Color(0xFFA8C7FA),
    onSecondary = Color(0xFF0B2246),
    secondaryContainer = Color(0xFF1A2232),
    onSecondaryContainer = Color(0xFFE0E8F5),
    background = Color(0xFF090C12),
    onBackground = Color(0xFFEDF1F8),
    surface = Color(0xFF0E131D),
    onSurface = Color(0xFFEDF1F8),
    surfaceVariant = Color(0xFF171E2C),
    onSurfaceVariant = Color(0xFFA2B0C6),
    outline = Color(0xFF2B374E),
    errorContainer = Color(0xFF4B1A22),
    onErrorContainer = Color(0xFFFFD9DF)
)

/** Launches directly into the chat UI. There is no login, account, or token logic anywhere. */
class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = AppGeminiDarkScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(viewModel)
                }
            }
        }
    }
}
