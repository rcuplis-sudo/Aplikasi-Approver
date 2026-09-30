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
     * Detects ALL placeholder locations in document.
     * When there are multiple placeholders (e.g. ${ttd_pengirim1}, ${ttd_pengirim2}, multiple AcroForms,
     * or multiple occurrences of a placeholder), all locations are detected.
     */
    fun detectAllPlaceholders(document: PDDocument): List<PlaceholderMatch> {
        val results = mutableListOf<PlaceholderMatch>()

        // 1. Check AcroForms for all matching signature/placeholder fields
        val acroFormMatches = detectAllAcroFormFields(document)
        results.addAll(acroFormMatches)

        // 2. Check Text Search for all occurrences of the specified placeholder
        val locator = PdfTextLocator(placeholderTarget)
        val textMatches = locator.findAllTargets(document)
        results.addAll(textMatches)

        // 3. Also check for standard numbered placeholders if user specified "${ttd_pengirim1}"
        // or general ttd placeholder format: ${ttd_pengirim\d+} or ${ttd_\w+}
        if (placeholderTarget.contains("ttd_pengirim", ignoreCase = true)) {
            val generalLocator = PdfTextLocator("regex:\\\$\\{ttd_pengirim\\d+\\}")
            val generalMatches = generalLocator.findAllTargets(document)
            for (m in generalMatches) {
                if (!results.any { it.pageIndex == m.pageIndex && Math.abs(it.x - m.x) < 20 && Math.abs(it.y - m.y) < 20 }) {
                    results.add(m)
                }
            }
        }

        // 4. Try clean target without ${} if still empty
        if (results.isEmpty()) {
            val cleanTarget = placeholderTarget.removePrefix("\${").removeSuffix("}")
            if (cleanTarget != placeholderTarget) {
                val cleanLocator = PdfTextLocator(cleanTarget)
                val cleanMatches = cleanLocator.findAllTargets(document)
                results.addAll(cleanMatches)
            }
        }

        // 5. If no placeholders found at all, activate fallback
        if (results.isEmpty()) {
            results.add(createFallbackMatch(document))
        }

        return results
    }

    /**
     * Backward-compatible single match method.
     */
    fun detectPlaceholder(document: PDDocument): PlaceholderMatch {
        return detectAllPlaceholders(document).first()
    }

    private fun detectAllAcroFormFields(document: PDDocument): List<PlaceholderMatch> {
        val matches = mutableListOf<PlaceholderMatch>()
        val acroForm = document.documentCatalog?.acroForm ?: return emptyList()
        val cleanTarget = placeholderTarget.removePrefix("\${").removeSuffix("}")

        val fields: List<PDField> = acroForm.fields ?: return emptyList()
        for (field in fields) {
            val name = field.fullyQualifiedName ?: field.partialName ?: ""
            val isTarget = name.equals(placeholderTarget, ignoreCase = true) ||
                    name.equals(cleanTarget, ignoreCase = true) ||
                    name.contains("ttd_pengirim", ignoreCase = true) ||
                    name.contains("signature", ignoreCase = true) ||
                    name.contains("paraf", ignoreCase = true)

            if (isTarget) {
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

                        matches.add(
                            PlaceholderMatch(
                                pageIndex = pageIndex,
                                x = rect.lowerLeftX,
                                y = rect.lowerLeftY,
                                width = rect.width.coerceAtLeast(80f),
                                height = rect.height.coerceAtLeast(80f),
                                source = DetectionMethod.ACROFORM,
                                fieldName = name,
                                details = "AcroForm Field '$name' pada halaman ${pageIndex + 1}"
                            )
                        )
                    }
                }
            }
        }
        return matches
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
