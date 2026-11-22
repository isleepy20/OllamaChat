package com.example.ollamachat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ollamachat.ui.ChatScreen

private val HybridThemeColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF072146),
    primaryContainer = Color(0xFF1B3458),
    onPrimaryContainer = Color(0xFFEAF2FF),
    secondary = Color(0xFFE08E6D),
    onSecondary = Color(0xFF3A1609),
    secondaryContainer = Color(0xFF2B2024),
    onSecondaryContainer = Color(0xFFF3EFEA),
    background = Color(0xFF0C0D10),
    onBackground = Color(0xFFF3EFEA),
    surface = Color(0xFF12141B),
    onSurface = Color(0xFFF3EFEA),
    surfaceVariant = Color(0xFF17161B),
    onSurfaceVariant = Color(0xFFB5B0A8),
    outline = Color(0xFF323746),
    errorContainer = Color(0xFF4D1D24),
    onErrorContainer = Color(0xFFFFDCE0)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = HybridThemeColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen()
                }
            }
        }
    }
}
