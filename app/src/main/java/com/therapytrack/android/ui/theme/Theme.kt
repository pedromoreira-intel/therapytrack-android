package com.therapytrack.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The iOS palette, so the two apps read as one product. */
object TherapyColors {
    val navy = Color(0xFF192A56)
    val champagne = Color(0xFFF7D794)
    val rose = Color(0xFFEDA6A3)
    val pearl = Color(0xFFFCFBFB)
    val canvas = Color(0xFFF6F3EE)
    val hairline = Color(0xFFE8E2D6)
    val critical = Color(0xFFC0392B)
    val warning = Color(0xFFE67E22)
    val success = Color(0xFF27AE60)
    val ink = Color(0xFF1F2430)
    val muted = Color(0xFF6B7280)
}

private val scheme = lightColorScheme(
    primary = TherapyColors.navy,
    onPrimary = Color.White,
    secondary = TherapyColors.champagne,
    onSecondary = TherapyColors.navy,
    tertiary = TherapyColors.rose,
    background = TherapyColors.canvas,
    onBackground = TherapyColors.ink,
    surface = TherapyColors.pearl,
    onSurface = TherapyColors.ink,
    surfaceVariant = Color(0xFFF1EDE6),
    onSurfaceVariant = TherapyColors.muted,
    outline = TherapyColors.hairline,
    error = TherapyColors.critical,
    onError = Color.White
)

private val type = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 12.sp, color = TherapyColors.muted)
)

private val shapes = Shapes(
    small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp))

@Composable
fun TherapyTrackTheme(content: @Composable () -> Unit) {
    // Single look, on purpose: clinical text on a dark ground was never designed or reviewed.
    MaterialTheme(colorScheme = scheme, typography = type, shapes = shapes, content = content)
}
