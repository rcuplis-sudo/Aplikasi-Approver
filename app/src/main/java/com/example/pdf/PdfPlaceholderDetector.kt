package com.example.pdf

import com.example.model.PlaceholderMatch
import com.tom_roush.pdfbox.pdmodel.PDDocument

class PdfPlaceholderDetector(
    private val placeholderTarget: String = "\${ttd_pengirim1}"
) {

    /**
     * Detects ALL placeholder locations in document purely via TEXT SEARCH.
     * When 1 text placeholder occurs 2+ times (multiple signatures) across pages,
     * all occurrences are detected so that all of them are signed.
     *
     * AcroForm and Fallback have been completely removed per specification.
     */
    fun detectAllPlaceholders(document: PDDocument): List<PlaceholderMatch> {
        val results = mutableListOf<PlaceholderMatch>()
        val target = placeholderTarget.trim()
        if (target.isEmpty()) return emptyList()

        // 1. Check exact Text Search for all occurrences of the specified placeholder
        // PdfTextLocator searches across all pages and returns all matches
        val locator = PdfTextLocator(target)
        val textMatches = locator.findAllTargets(document)
        results.addAll(textMatches)

        // 2. Also check for numbered placeholders if user specified "${ttd_pengirim1}"
        // or general ttd placeholder format: ${ttd_pengirim\d+} so documents with multiple ttd tags are captured
        if (target.contains("ttd_pengirim", ignoreCase = true)) {
            val generalLocator = PdfTextLocator("regex:\\\$\\{ttd_pengirim\\d*\\}")
            val generalMatches = generalLocator.findAllTargets(document)
            for (m in generalMatches) {
                if (!results.any { it.pageIndex == m.pageIndex && Math.abs(it.x - m.x) < 15 && Math.abs(it.y - m.y) < 15 }) {
                    results.add(m)
                }
            }
        }

        // 3. Try clean target without ${} if still empty (e.g. user typed ${ttd_pengirim1} but text in PDF is ttd_pengirim1)
        if (results.isEmpty()) {
            val cleanTarget = target.removePrefix("\${").removeSuffix("}")
            if (cleanTarget != target && cleanTarget.isNotBlank()) {
                val cleanLocator = PdfTextLocator(cleanTarget)
                val cleanMatches = cleanLocator.findAllTargets(document)
                results.addAll(cleanMatches)
            }
        }

        // Return purely the detected text occurrences (No AcroForm, No Fallback)
        return results
    }

    /**
     * Backward-compatible single match method.
     */
    fun detectPlaceholder(document: PDDocument): PlaceholderMatch? {
        return detectAllPlaceholders(document).firstOrNull()
    }
}

