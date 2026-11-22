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

private val HybridGeminiClaudeScheme = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF072146),
    primaryContainer = Color(0xFF1B3458),
    onPrimaryContainer = Color(0xFFEAF2FF),
    secondary = Color(0xFFE08E6D),
    onSecondary = Color(0xFF3A1609),
    secondaryContainer = Color(0xFF38221A),
    onSecondaryContainer = Color(0xFFFFE5DA),
    tertiary = Color(0xFFD97757),
    background = Color(0xFF0C0D10),
    onBackground = Color(0xFFF3EFEA),
    surface = Color(0xFF121419),
    onSurface = Color(0xFFF3EFEA),
    surfaceVariant = Color(0xFF1B1E26),
    onSurfaceVariant = Color(0xFFB5B0A8),
    outline = Color(0xFF323746),
    errorContainer = Color(0xFF4D1D24),
    onErrorContainer = Color(0xFFFFDCE0)
)

/** Launches directly into the chat UI. There is no login, account, or token logic anywhere. */
class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = HybridGeminiClaudeScheme) {
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
