package com.example.pdf

import com.example.model.CharPosition
import com.example.model.DetectionMethod
import com.example.model.PlaceholderMatch
import java.util.regex.Pattern

object TextPositionMatcher {

    /**
     * Pure function to search for all occurrences of target text or regex pattern
     * in a list of character positions.
     * Easily testable in unit tests without requiring Android framework or PDF rendering engine.
     */
    fun searchAllInPositions(
        positions: List<CharPosition>,
        targetTextOrRegex: String,
        pageIndex: Int,
        pageHeight: Float
    ): List<PlaceholderMatch> {
        if (positions.isEmpty() || targetTextOrRegex.isEmpty()) return emptyList()

        val sb = StringBuilder()
        for (p in positions) {
            sb.append(p.char)
        }
        val fullText = sb.toString()

        val matches = mutableListOf<PlaceholderMatch>()

        // 1. If target looks like a regex pattern or placeholder pattern with wildcard / alternatives
        val isRegex = targetTextOrRegex.contains(".*") ||
                targetTextOrRegex.contains(".+") ||
                targetTextOrRegex.contains("|") ||
                targetTextOrRegex.contains("\\d") ||
                targetTextOrRegex.startsWith("regex:")

        if (isRegex) {
            val cleanPatternStr = targetTextOrRegex.removePrefix("regex:")
            try {
                val pattern = Pattern.compile(cleanPatternStr, Pattern.CASE_INSENSITIVE)
                val matcher = pattern.matcher(fullText)
                while (matcher.find()) {
                    val start = matcher.start()
                    val end = matcher.end()
                    val matchedSubtext = matcher.group()
                    if (start in positions.indices && end <= positions.size && start < end) {
                        val matchChars = positions.subList(start, end)
                        matches.add(createMatchFromChars(matchChars, pageIndex, pageHeight, matchedSubtext))
                    }
                }
            } catch (e: Exception) {
                // If regex parsing fails, fallback to literal search
            }
        }

        // 2. Literal substring match (find ALL occurrences in page)
        if (matches.isEmpty()) {
            var searchFromIndex = 0
            while (searchFromIndex < fullText.length) {
                val matchIdx = fullText.indexOf(targetTextOrRegex, searchFromIndex, ignoreCase = true)
                if (matchIdx == -1) break

                val endIdx = (matchIdx + targetTextOrRegex.length).coerceAtMost(positions.size)
                if (matchIdx < endIdx) {
                    val matchChars = positions.subList(matchIdx, endIdx)
                    matches.add(createMatchFromChars(matchChars, pageIndex, pageHeight, targetTextOrRegex))
                }
                searchFromIndex = matchIdx + targetTextOrRegex.length.coerceAtLeast(1)
            }
        }

        return matches
    }

    /**
     * Finds single (first) occurrence for backward compatibility.
     */
    fun searchInPositions(
        positions: List<CharPosition>,
        target: String,
        pageIndex: Int,
        pageHeight: Float
    ): PlaceholderMatch? {
        return searchAllInPositions(positions, target, pageIndex, pageHeight).firstOrNull()
    }

    private fun createMatchFromChars(
        matchChars: List<CharPosition>,
        pageIndex: Int,
        pageHeight: Float,
        matchedLabel: String
    ): PlaceholderMatch {
        val firstChar = matchChars.first()
        val minX = matchChars.minOf { it.x }
        val maxX = matchChars.maxOf { it.x + it.width }
        val width = maxX - minX
        val maxHeight = matchChars.maxOf { it.height }.coerceAtLeast(12f)

        // Convert Y from top-down to PDF standard bottom-up coordinate space
        val pdfY = (pageHeight - firstChar.y).coerceAtLeast(0f)

        return PlaceholderMatch(
            pageIndex = pageIndex,
            x = minX,
            y = pdfY,
            width = width,
            height = maxHeight,
            source = DetectionMethod.TEXT_SEARCH,
            details = "Ditemukan '$matchedLabel' via Text Search pada halaman ${pageIndex + 1} (X: ${minX.toInt()}, Y: ${pdfY.toInt()})"
        )
    }
}
