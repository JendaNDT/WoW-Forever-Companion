package cz.jenda.wowforever.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GuideColors = darkColorScheme(
    primary = Color(0xFFE3BC7A), onPrimary = Color(0xFF392A13),
    secondary = Color(0xFFB7CDA6), background = Color(0xFF151718),
    surface = Color(0xFF1E2123), surfaceVariant = Color(0xFF303436),
    onSurface = Color(0xFFF3EEE4), onSurfaceVariant = Color(0xFFC5C5BF),
)
@Composable fun GuideTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GuideColors, content = content)
}
