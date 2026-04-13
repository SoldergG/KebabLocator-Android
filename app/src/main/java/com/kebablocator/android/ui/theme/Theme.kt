package com.kebablocator.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Design System Colors (matching iOS)
object KebabColors {
    val bgPrimary = Color(0xFF0A0A0F)
    val bgElevated = Color(0xFF12121A)
    val bgCard = Color(0xD912121A) // 85% opacity
    val surface = Color(0xFF1A1A29)
    val accentOrange = Color(0xFFF59E0A)
    val accentGlow = Color(0x40F59E0A) // 25% opacity
    val textPrimary = Color(0xFFF2F2F5)
    val textSecondary = Color(0xFF9CA3AF)
    val textMuted = Color(0xFF6B7080)
    val starYellow = Color(0xFFFABF24)
    val openGreen = Color(0xFF21C45E)
    val closedRed = Color(0xFFF04545)
    val gradientStart = Color(0xFFF59E0A)
    val gradientEnd = Color(0xFFF04545)
}

private val DarkColorScheme = darkColorScheme(
    primary = KebabColors.accentOrange,
    onPrimary = Color.Black,
    secondary = KebabColors.accentOrange,
    onSecondary = Color.Black,
    background = KebabColors.bgPrimary,
    onBackground = KebabColors.textPrimary,
    surface = KebabColors.surface,
    onSurface = KebabColors.textPrimary,
    surfaceVariant = KebabColors.bgElevated,
    onSurfaceVariant = KebabColors.textSecondary,
    error = KebabColors.closedRed,
    onError = Color.White,
)

@Composable
fun KebabLocatorTheme(content: @Composable () -> Unit) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = KebabColors.bgPrimary.toArgb()
            window.navigationBarColor = KebabColors.bgPrimary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                color = KebabColors.textPrimary
            ),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = KebabColors.textPrimary
            ),
            titleLarge = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = KebabColors.textPrimary
            ),
            titleMedium = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = KebabColors.textPrimary
            ),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                color = KebabColors.textPrimary
            ),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                color = KebabColors.textSecondary
            ),
            labelLarge = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            ),
            labelSmall = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = KebabColors.textMuted
            ),
        ),
        content = content
    )
}
