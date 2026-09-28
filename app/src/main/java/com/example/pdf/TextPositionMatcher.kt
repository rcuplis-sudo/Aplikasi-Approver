package com.example.pdf

import com.example.model.CharPosition
import com.example.model.DetectionMethod
import com.example.model.PlaceholderMatch

object TextPositionMatcher {

    /**
     * Pure function to search for target text in a list of character positions.
     * Easily testable in unit tests without requiring Android framework or PDF rendering engine.
     */
    fun searchInPositions(
        positions: List<CharPosition>,
        target: String,
        pageIndex: Int,
        pageHeight: Float
    ): PlaceholderMatch? {
        if (positions.isEmpty() || target.isEmpty()) return null

        val sb = StringBuilder()
        for (p in positions) {
            sb.append(p.char)
        }
        val fullText = sb.toString()
        val matchIdx = fullText.indexOf(target)
        if (matchIdx == -1) return null

        val matchChars = positions.subList(matchIdx, matchIdx + target.length)
        val firstChar = matchChars.first()

        val minX = matchChars.minOf { it.x }
        val maxX = matchChars.maxOf { it.x + it.width }
        val width = maxX - minX
        val maxHeight = matchChars.maxOf { it.height }.coerceAtLeast(12f)

        // Convert Y from top-down to PDF standard bottom-up coordinate space
        // In PDF content stream, (0, 0) is at the bottom-left corner
        val pdfY = (pageHeight - firstChar.y).coerceAtLeast(0f)

        return PlaceholderMatch(
            pageIndex = pageIndex,
            x = minX,
            y = pdfY,
            width = width,
            height = maxHeight,
            source = DetectionMethod.TEXT_SEARCH,
            details = "Ditemukan via Text Search pada halaman ${pageIndex + 1} (X: ${minX.toInt()}, Y: ${pdfY.toInt()})"
        )
    }
}
