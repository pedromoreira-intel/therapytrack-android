package com.example.therapytrack_android.ui.theme

import androidx.compose.ui.graphics.Color

// TherapyTrack Design System - Matching iOS Colors exactly

// Brand Palette
val MidnightNavy = Color(0xFF192A56)   // Primary - headers, nav bars
val Champagne = Color(0xFFF7D794)       // Accent - badges, icons
val DustyRose = Color(0xFFEDA6A3)     // Secondary - highlights
val PearlWhite = Color(0xFFFCFBFB)     // Background

// Semantic Colors
val Critical = Color(0xFFC0392B)        // High risk
val Warning = Color(0xFFE67E22)         // Medium risk
val Success = Color(0xFF27AE60)        // Low risk / stable
val Info = MidnightNavy                // Informational
val Neutral = Color(0xFF8E9AAB)         // Neutral

// Status Colors
val HighRisk = Critical
val MediumRisk = Warning
val LowRisk = Success
val Stable = Success
val Improving = Info
val AtRisk = Warning
val Crisis = Critical

// Backgrounds
val CardBackground = Color.White
val SectionBackground = Color(0xFFF5F5F5)
val SurfaceBackground = Color(0xFFE8E8E8)

// Text Colors
val TextPrimary = Color(0xFF1C1C1E)
val TextSecondary = Color(0xFF6B6B6B)
val TextTertiary = Color(0xFF8E9AAB)
val TextOnLight = MidnightNavy
val TextOnDark = PearlWhite
