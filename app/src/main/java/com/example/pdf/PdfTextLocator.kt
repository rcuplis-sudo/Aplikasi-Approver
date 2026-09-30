package com.example.pdf

import com.example.model.CharPosition
import com.example.model.PlaceholderMatch
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.StringWriter

/**
 * Searches for all occurrences of a text string or pattern within a PDDocument
 * and determines the exact page and coordinates (x, y, width, height) in PDF coordinate space.
 */
class PdfTextLocator(private val targetTextOrRegex: String) : PDFTextStripper() {

    private val allPositions = mutableListOf<CharPosition>()
    private var currentPageIndex = 0
    private var currentPageHeight = 792f

    override fun processPage(page: PDPage) {
        currentPageHeight = page.cropBox?.height ?: page.mediaBox.height
        super.processPage(page)
    }

    override fun writeString(text: String, textPositions: List<TextPosition>) {
        for (tp in textPositions) {
            allPositions.add(
                CharPosition(
                    char = tp.unicode ?: "",
                    x = tp.xDirAdj,
                    y = tp.yDirAdj,
                    width = tp.widthDirAdj,
                    height = tp.heightDir,
                    pageIndex = currentPageIndex,
                    pageHeight = currentPageHeight
                )
            )
        }
        super.writeString(text, textPositions)
    }

    /**
     * Executes search across all pages of the document, finding ALL matching occurrences.
     */
    fun findAllTargets(document: PDDocument): List<PlaceholderMatch> {
        val allMatches = mutableListOf<PlaceholderMatch>()
        val totalPages = document.numberOfPages
        for (pageIdx in 0 until totalPages) {
            currentPageIndex = pageIdx
            val pageNum = pageIdx + 1
            startPage = pageNum
            endPage = pageNum
            allPositions.clear()

            // Run stripper on this single page
            val dummyWriter = StringWriter()
            writeText(document, dummyWriter)

            val matchesOnPage = TextPositionMatcher.searchAllInPositions(
                positions = allPositions,
                targetTextOrRegex = targetTextOrRegex,
                pageIndex = pageIdx,
                pageHeight = currentPageHeight
            )
            allMatches.addAll(matchesOnPage)
        }
        return allMatches
    }

    /**
     * Backward-compatible single match search.
     */
    fun findTarget(document: PDDocument): PlaceholderMatch? {
        return findAllTargets(document).firstOrNull()
    }
}
