package com.example.pdf

import com.example.model.CharPosition
import com.example.model.PlaceholderMatch
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.StringWriter

/**
 * Searches for a text string within a PDDocument and determines
 * the exact page and coordinates (x, y, width, height) in PDF coordinate space.
 */
class PdfTextLocator(private val targetText: String) : PDFTextStripper() {

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
     * Executes the search across all pages of the document.
     * Returns PlaceholderMatch if found, or null otherwise.
     */
    fun findTarget(document: PDDocument): PlaceholderMatch? {
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

            val match = TextPositionMatcher.searchInPositions(
                positions = allPositions,
                target = targetText,
                pageIndex = pageIdx,
                pageHeight = currentPageHeight
            )
            if (match != null) {
                return match
            }
        }
        return null
    }
}
