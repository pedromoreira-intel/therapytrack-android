package com.example.therapytrack_android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MidnightNavy,
    onPrimary = PearlWhite,
    primaryContainer = MidnightNavy.copy(alpha = 0.1f),
    onPrimaryContainer = MidnightNavy,
    
    secondary = DustyRose,
    onSecondary = MidnightNavy,
    secondaryContainer = DustyRose.copy(alpha = 0.15f),
    onSecondaryContainer = MidnightNavy,
    
    tertiary = Champagne,
    onTertiary = MidnightNavy,
    tertiaryContainer = Champagne.copy(alpha = 0.2f),
    onTertiaryContainer = MidnightNavy,
    
    error = Critical,
    onError = Color.White,
    errorContainer = Critical.copy(alpha = 0.1f),
    onErrorContainer = Critical,
    
    background = PearlWhite,
    onBackground = TextPrimary,
    
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = SectionBackground,
    onSurfaceVariant = TextSecondary,
    
    outline = Neutral,
    outlineVariant = Color(0xFFE0E0E0)
)

private val DarkColorScheme = darkColorScheme(
    primary = Champagne,
    onPrimary = MidnightNavy,
    primaryContainer = MidnightNavy.copy(alpha = 0.3f),
    onPrimaryContainer = Champagne,
    
    secondary = DustyRose,
    onSecondary = MidnightNavy,
    secondaryContainer = DustyRose.copy(alpha = 0.2f),
    onSecondaryContainer = DustyRose,
    
    tertiary = Champagne,
    onTertiary = MidnightNavy,
    tertiaryContainer = Champagne.copy(alpha = 0.2f),
    onTertiaryContainer = Champagne,
    
    error = Critical,
    onError = Color.White,
    errorContainer = Critical.copy(alpha = 0.2f),
    onErrorContainer = Critical,
    
    background = Color(0xFF1C1C1E),
    onBackground = Color.White,
    
    surface = Color(0xFF2C2C2E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF3A3A3C),
    onSurfaceVariant = Color(0xFFAEAEB2),
    
    outline = Neutral,
    outlineVariant = Color(0xFF3A3A3C)
)

@Composable
fun Therapytrack_androidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled to use custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
