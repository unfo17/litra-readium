package io.github.unfo17.litra.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.*
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

@Composable
fun LitraTheme(mode: String = "SYSTEM", content: @Composable () -> Unit) {
    val dark = mode == "DARK" || (mode == "SYSTEM" && isSystemInDarkTheme())
    val scheme = if (dark) darkColorScheme(primary = Color(0xFFFFB59B), onPrimary = Color(0xFF56200B), surfaceContainer = Color(0xFF24211D), surfaceContainerHighest = Color(0xFF3A352F), outlineVariant = Color(0xFF4B443B), primaryContainer = Color(0xFF71321D), onPrimaryContainer = Color(0xFFFFDACE), secondaryContainer = Color(0xFF514339), onSecondaryContainer = Color(0xFFF2DFD2), surface = Color(0xFF191714), background = Color(0xFF191714))
    else lightColorScheme(primary = Color(0xFFB64B2B), secondary = Color(0xFF795548), primaryContainer = Color(0xFFF3E2D9), onPrimaryContainer = Color(0xFF6D2A15), secondaryContainer = Color(0xFFEADFD2), onSecondaryContainer = Color(0xFF45372B), background = Color(0xFFFBFAF6), surface = Color(0xFFFBFAF6), surfaceContainer = Color(0xFFF2EFE8), surfaceContainerHighest = Color(0xFFEAE5DC), outline = Color(0xFF867A70), outlineVariant = Color(0xFFDDD5CA), onSurfaceVariant = Color(0xFF686057))
    val activity = LocalActivity.current
    val view = LocalView.current
    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = scheme, shapes = Shapes(extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp)), typography = Typography().let { it.copy(titleLarge = it.titleLarge.copy(fontWeight = FontWeight.SemiBold), headlineSmall = it.headlineSmall.copy(fontWeight = FontWeight.SemiBold)) }, content = content)
}
