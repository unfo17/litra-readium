package io.github.unfo17.litra.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun LitraTheme(mode: String = "SYSTEM", content: @Composable () -> Unit) {
    val dark = mode == "DARK" || (mode == "SYSTEM" && isSystemInDarkTheme())
    val scheme = if (dark) darkColorScheme(primary = Color(0xFFFFB59B), surface = Color(0xFF191714), background = Color(0xFF191714))
    else lightColorScheme(primary = Color(0xFFB64B2B), secondary = Color(0xFF795548), background = Color(0xFFFBFAF6), surface = Color(0xFFFBFAF6), surfaceContainer = Color(0xFFF2EFE8))
    MaterialTheme(colorScheme = scheme, content = content)
}
