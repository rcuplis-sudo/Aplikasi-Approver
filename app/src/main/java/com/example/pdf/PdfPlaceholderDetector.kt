package com.example.pdf

import com.example.model.DetectionMethod
import com.example.model.PlaceholderMatch
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField

class PdfPlaceholderDetector(
    private val placeholderTarget: String = "\${ttd_pengirim1}"
) {

    /**
     * Attempts detection in priority order:
     * 1. AcroForm field matching placeholder target or "ttd_pengirim1"
     * 2. Text search for literal placeholder "${ttd_pengirim1}"
     * 3. Fallback to bottom-right corner of the last page
     */
    fun detectPlaceholder(document: PDDocument): PlaceholderMatch {
        // 1. Check AcroForm
        val acroFormMatch = detectAcroFormField(document)
        if (acroFormMatch != null) {
            return acroFormMatch
        }

        // 2. Check Text Search
        val locator = PdfTextLocator(placeholderTarget)
        val textMatch = locator.findTarget(document)
        if (textMatch != null) {
            return textMatch
        }

        // Also try clean target without ${} if not yet found
        val cleanTarget = placeholderTarget.removePrefix("\${").removeSuffix("}")
        if (cleanTarget != placeholderTarget) {
            val cleanLocator = PdfTextLocator(cleanTarget)
            val cleanMatch = cleanLocator.findTarget(document)
            if (cleanMatch != null) {
                return cleanMatch
            }
        }

        // 3. Fallback: place on the last page at bottom-right corner
        return createFallbackMatch(document)
    }

    private fun detectAcroFormField(document: PDDocument): PlaceholderMatch? {
        val acroForm = document.documentCatalog?.acroForm ?: return null
        val cleanTarget = placeholderTarget.removePrefix("\${").removeSuffix("}")

        val fields: List<PDField> = acroForm.fields ?: return null
        for (field in fields) {
            val name = field.fullyQualifiedName ?: field.partialName ?: ""
            val matches = name.equals(placeholderTarget, ignoreCase = true) ||
                    name.equals(cleanTarget, ignoreCase = true) ||
                    name.contains("ttd_pengirim1", ignoreCase = true)

            if (matches) {
                val widgets: List<PDAnnotationWidget> = field.widgets ?: emptyList()
                for (widget in widgets) {
                    val rect = widget.rectangle
                    if (rect != null) {
                        val page = widget.page
                        val pageIndex = if (page != null) {
                            val idx = document.pages.indexOf(page)
                            if (idx >= 0) idx else (document.numberOfPages - 1)
                        } else {
                            findPageIndexWithAnnotation(document, widget)
                        }

                        return PlaceholderMatch(
                            pageIndex = pageIndex,
                            x = rect.lowerLeftX,
                            y = rect.lowerLeftY,
                            width = rect.width.coerceAtLeast(80f),
                            height = rect.height.coerceAtLeast(80f),
                            source = DetectionMethod.ACROFORM,
                            fieldName = name,
                            details = "Ditemukan pada AcroForm field '$name' di halaman ${pageIndex + 1}"
                        )
                    }
                }
            }
        }
        return null
    }

    private fun findPageIndexWithAnnotation(document: PDDocument, widget: PDAnnotationWidget): Int {
        for (i in 0 until document.numberOfPages) {
            val page = document.getPage(i)
            if (page.annotations.contains(widget)) {
                return i
            }
        }
        return 0
    }

    private fun createFallbackMatch(document: PDDocument): PlaceholderMatch {
        val totalPages = document.numberOfPages.coerceAtLeast(1)
        val lastPageIndex = totalPages - 1
        val lastPage = document.getPage(lastPageIndex)
        val mediaBox = lastPage.cropBox ?: lastPage.mediaBox

        val qrSize = 100f
        val margin = 50f
        val fallbackX = (mediaBox.width - qrSize - margin).coerceAtLeast(margin)
        val fallbackY = margin

        return PlaceholderMatch(
            pageIndex = lastPageIndex,
            x = fallbackX,
            y = fallbackY,
            width = qrSize,
            height = qrSize,
            source = DetectionMethod.FALLBACK,
            fieldName = null,
            details = "Fallback: Placeholder '$placeholderTarget' tidak ditemukan. Ditempatkan di halaman ${lastPageIndex + 1} sudut kanan bawah."
        )
    }
}
