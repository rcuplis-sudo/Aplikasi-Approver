package com.example.model

/**
 * Represents custom placement coordinates (in points or normalized ratio)
 * chosen by the user in the interactive touch-and-drag visual preview.
 */
data class CustomQrPlacement(
    val pageIndex: Int,
    // Normalized coordinates on the page [0f..1f] where (0,0) is top-left in screen coords
    // and can be translated to PDF coordinates (PDF origin is bottom-left).
    val normalizedX: Float,
    val normalizedY: Float, // from top
    val qrSizeDp: Float = 75f
)
