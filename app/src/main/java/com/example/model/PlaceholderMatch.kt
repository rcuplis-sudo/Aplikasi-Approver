package com.example.model

enum class DetectionMethod {
    ACROFORM,
    TEXT_SEARCH,
    FALLBACK,
    MANUAL_DRAG
}

data class PlaceholderMatch(
    val pageIndex: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val source: DetectionMethod,
    val fieldName: String? = null,
    val details: String = "",
    val availableVerticalSpace: Float? = null,
    val textBelowPdfY: Float? = null
)
