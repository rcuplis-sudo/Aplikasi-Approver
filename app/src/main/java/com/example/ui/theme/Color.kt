package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// --- Modern Crisp Light Palette (Tema Terang Bersih & Elegan) ---
val PureWhite = Color(0xFFFFFFFF)
val SnowWhite = Color(0xFFF8FAFC)           // Ultra-clean canvas background
val OffWhiteSurface = Color(0xFFF1F5F9)     // Card elevated surface
val BorderCrispLight = Color(0xFFE2E8F0)    // High clarity subtle outline
val TextPrimaryLight = Color(0xFF0F172A)    // Deep slate black for max readability
val TextSecondaryLight = Color(0xFF475569)  // Slate dark gray for secondary descriptions
val TextMutedLight = Color(0xFF64748B)

// Royal Ocean Blue (Default Light Accent - Cerah, Terang, Modern)
val OceanBluePrimary = Color(0xFF0284C7)     // Cerah tajam
val OceanBlueContainer = Color(0xFFE0F2FE)   // Sangat terang lembut
val OceanBlueOnContainer = Color(0xFF0369A1)
val OceanBlueAccent = Color(0xFF0EA5E9)

// Emerald & Mint (Fresh & Organic)
val EmeraldPrimary = Color(0xFF059669)
val EmeraldContainer = Color(0xFFD1FAE5)
val EmeraldOnContainer = Color(0xFF065F46)

// Indigo Neo
val IndigoPrimary = Color(0xFF4F46E5)
val IndigoContainer = Color(0xFFEEF2FF)
val IndigoOnContainer = Color(0xFF3730A3)

// Warm Amber
val AmberPrimary = Color(0xFFD97706)
val AmberContainer = Color(0xFFFEF3C7)
val AmberOnContainer = Color(0xFF92400E)

// Modern Status Colors
val SuccessGreen = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRose = Color(0xFFE11D48)

// --- Dark Mode Obsidian Palette (Untuk Pilihan Dark) ---
val DarkCanvas = Color(0xFF0B0F19)
val DarkSurface = Color(0xFF131A29)
val DarkSurfaceElevated = Color(0xFF1E293B)
val DarkBorder = Color(0xFF334155)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)

// Compatibility Aliases for all existing components
val NavyPrimary = OceanBluePrimary
val NavyLight = OceanBlueAccent
val EmeraldSuccess = SuccessGreen
val AmberFallback = WarningAmber
val PrimaryBlueLight = OceanBluePrimary
val PrimaryBlueContainer = OceanBlueContainer
val LightCanvas = SnowWhite
val LightSurface = PureWhite
val LightSurfaceElevated = OffWhiteSurface
val LightBorder = BorderCrispLight
val LightTextPrimary = TextPrimaryLight
val LightTextSecondary = TextSecondaryLight
val RoseError = ErrorRose
