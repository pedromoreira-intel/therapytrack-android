package com.therapytrack.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.therapytrack.android.R

/** The iOS palette (DesignSystem/TherapyColors.swift), so the two apps read as one product. */
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
    val neutral = Color(0xFF8E9AAB)
    val ink = Color(0xFF1F2430)
    val muted = Color(0xFF6B7280)
    val shadowInk = Color(0xFF1A1730)

    /** The sign-in hero: navy into a violet, top-left to bottom-right. */
    val heroGradient = Brush.linearGradient(listOf(Color(0xFF1B2A5A), Color(0xFF2A3672), Color(0xFF3B2F5E)))
    val navyGradient = Brush.linearGradient(listOf(navy, Color(0xFF243B71)))
    val roseChampagne = Brush.linearGradient(listOf(rose, champagne))
}

/** Playfair Display stands in for iOS's New York: the serif that carries every title. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val SerifDisplay = FontFamily(
    // A variable font: the weight axis must be set explicitly or every style renders at the default 400.
    Font(R.font.playfair_display, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.playfair_display, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600)))
)

/** iOS TherapyTypography, one to one. */
object TherapyType {
    val displayXL = TextStyle(fontFamily = SerifDisplay, fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp, lineHeight = 46.sp)
    val display = TextStyle(fontFamily = SerifDisplay, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp, lineHeight = 38.sp)
    val serifTitle = TextStyle(fontFamily = SerifDisplay, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp, lineHeight = 30.sp)
    val h1 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
    val h2 = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)
    val h3 = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
    val h4 = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
    val overline = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp)
    val bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp)
    val body = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
    val bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)
    val caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
    val captionBold = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
    val label = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium)
    val emphasis = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val emphasisLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val metric = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
    val metricLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold)
    val metricSmall = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
}

/** iOS TherapySpacing radii. */
object TherapyRadius { val small = 8.dp; val medium = 12.dp; val large = 16.dp; val xlarge = 24.dp }

private val scheme = lightColorScheme(
    primary = TherapyColors.navy, onPrimary = Color.White,
    secondary = TherapyColors.champagne, onSecondary = TherapyColors.navy,
    tertiary = TherapyColors.rose,
    background = TherapyColors.canvas, onBackground = TherapyColors.ink,
    surface = Color.White, onSurface = TherapyColors.ink,
    surfaceVariant = TherapyColors.canvas, onSurfaceVariant = TherapyColors.muted,
    outline = TherapyColors.hairline, error = TherapyColors.critical, onError = Color.White
)

private val type = Typography(
    headlineLarge = TherapyType.display, headlineMedium = TherapyType.serifTitle, headlineSmall = TherapyType.h2,
    titleLarge = TherapyType.h2, titleMedium = TherapyType.emphasisLarge, titleSmall = TherapyType.emphasis,
    bodyLarge = TherapyType.bodyLarge, bodyMedium = TherapyType.body, bodySmall = TherapyType.bodySmall,
    labelLarge = TherapyType.emphasisLarge, labelMedium = TherapyType.captionBold, labelSmall = TherapyType.caption.copy(color = TherapyColors.muted)
)

private val shapes = Shapes(small = RoundedCornerShape(TherapyRadius.small), medium = RoundedCornerShape(TherapyRadius.large), large = RoundedCornerShape(TherapyRadius.xlarge))

@Composable
fun TherapyTrackTheme(content: @Composable () -> Unit) {
    // Single look, on purpose: clinical text on a dark ground was never designed or reviewed.
    MaterialTheme(colorScheme = scheme, typography = type, shapes = shapes, content = content)
}
