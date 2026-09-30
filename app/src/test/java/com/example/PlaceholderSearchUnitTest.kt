package com.example

import com.example.model.CharPosition
import com.example.model.DetectionMethod
import com.example.model.SignatureQrPayload
import com.example.pdf.PdfSignerService
import com.example.pdf.TextPositionMatcher
import org.junit.Assert.*
import org.junit.Test

class PlaceholderSearchUnitTest {

    @Test
    fun testTextPositionSearch_foundPlaceholder() {
        val target = "\${ttd_pengirim1}"
        val pageHeight = 842f // A4 height in points
        val pageIndex = 0

        // Simulate character positions as emitted by PDFTextStripper
        val positions = mutableListOf<CharPosition>()

        // Prefix text: "Tertanda, "
        var currentX = 50f
        val prefix = "Tertanda, "
        for (ch in prefix) {
            positions.add(
                CharPosition(
                    char = ch.toString(),
                    x = currentX,
                    y = 300f, // 300 points from top
                    width = 8f,
                    height = 12f,
                    pageIndex = pageIndex,
                    pageHeight = pageHeight
                )
            )
            currentX += 8f
        }

        // Placeholder text: "${ttd_pengirim1}"
        val expectedStartX = currentX
        for (ch in target) {
            positions.add(
                CharPosition(
                    char = ch.toString(),
                    x = currentX,
                    y = 300f,
                    width = 9f,
                    height = 12f,
                    pageIndex = pageIndex,
                    pageHeight = pageHeight
                )
            )
            currentX += 9f
        }
        val expectedWidth = target.length * 9f

        // Execute pure search algorithm
        val result = TextPositionMatcher.searchInPositions(
            positions = positions,
            target = target,
            pageIndex = pageIndex,
            pageHeight = pageHeight
        )

        // Assertions
        assertNotNull("Placeholder harus ditemukan di posisi teks", result)
        assertEquals(DetectionMethod.TEXT_SEARCH, result!!.source)
        assertEquals(pageIndex, result.pageIndex)
        assertEquals(expectedStartX, result.x, 0.01f)
        assertEquals(expectedWidth, result.width, 0.01f)

        // Verify PDF coordinate conversion (bottom-up: pageHeight - yFromTop)
        val expectedPdfY = pageHeight - 300f
        assertEquals(expectedPdfY, result.y, 0.01f)
    }

    @Test
    fun testTextPositionSearch_multiplePlaceholdersDetected() {
        val pageHeight = 842f
        val pageIndex = 0
        val positions = mutableListOf<CharPosition>()

        // Placeholder 1: ${ttd_pengirim1} at Y=300, X=50
        val p1 = "\${ttd_pengirim1}"
        var x1 = 50f
        for (ch in p1) {
            positions.add(CharPosition(ch.toString(), x1, 300f, 9f, 12f, pageIndex, pageHeight))
            x1 += 9f
        }

        // Intermediary text
        val gap = " --- "
        for (ch in gap) {
            positions.add(CharPosition(ch.toString(), x1, 300f, 6f, 12f, pageIndex, pageHeight))
            x1 += 6f
        }

        // Placeholder 2: ${ttd_pengirim2} at Y=300, X=x1
        val p2 = "\${ttd_pengirim2}"
        val expectedStartX2 = x1
        for (ch in p2) {
            positions.add(CharPosition(ch.toString(), x1, 300f, 9f, 12f, pageIndex, pageHeight))
            x1 += 9f
        }

        // Search with regex matching both: \${ttd_pengirim\d+}
        val matches = TextPositionMatcher.searchAllInPositions(
            positions = positions,
            targetTextOrRegex = "regex:\\\$\\{ttd_pengirim\\d+\\}",
            pageIndex = pageIndex,
            pageHeight = pageHeight
        )

        assertEquals("Harus menemukan 2 placeholder secara otomatis", 2, matches.size)
        assertEquals(50f, matches[0].x, 0.01f)
        assertEquals(expectedStartX2, matches[1].x, 0.01f)
    }

    @Test
    fun testTextPositionSearch_placeholderNotFound() {
        val pageHeight = 842f
        val positions = listOf(
            CharPosition(
                char = "H",
                x = 50f,
                y = 100f,
                width = 8f,
                height = 12f,
                pageIndex = 0,
                pageHeight = pageHeight
            ),
            CharPosition(
                char = "i",
                x = 58f,
                y = 100f,
                width = 8f,
                height = 12f,
                pageIndex = 0,
                pageHeight = pageHeight
            )
        )

        val result = TextPositionMatcher.searchInPositions(
            positions = positions,
            target = "\${ttd_pengirim1}",
            pageIndex = 0,
            pageHeight = pageHeight
        )

        assertNull("Jika placeholder tidak ada, harus mengembalikan null (tanpa fallback)", result)

        val allResults = TextPositionMatcher.searchAllInPositions(
            positions = positions,
            targetTextOrRegex = "\${ttd_pengirim1}",
            pageIndex = 0,
            pageHeight = pageHeight
        )
        assertTrue("Hasil pencarian teks kosong jika tidak ditemukan", allResults.isEmpty())
    }

    @Test
    fun testTextPositionSearch_oneIdenticalTextMultipleOccurrences() {
        val pageHeight = 842f
        val pageIndex = 0
        val target = "\${ttd_pengirim1}"
        val positions = mutableListOf<CharPosition>()

        // 1st occurrence of ${ttd_pengirim1}
        var curX = 50f
        for (ch in target) {
            positions.add(CharPosition(ch.toString(), curX, 300f, 9f, 12f, pageIndex, pageHeight))
            curX += 9f
        }

        // Intermediary separator text
        for (ch in " --- Pemisah Tanda Tangan --- ") {
            positions.add(CharPosition(ch.toString(), curX, 300f, 7f, 12f, pageIndex, pageHeight))
            curX += 7f
        }

        // 2nd occurrence of the EXACT SAME text ${ttd_pengirim1}
        val expectedStartX2 = curX
        for (ch in target) {
            positions.add(CharPosition(ch.toString(), curX, 300f, 9f, 12f, pageIndex, pageHeight))
            curX += 9f
        }

        // 3rd occurrence on another section (Y=500f)
        var curX3 = 50f
        for (ch in target) {
            positions.add(CharPosition(ch.toString(), curX3, 500f, 9f, 12f, pageIndex, pageHeight))
            curX3 += 9f
        }

        // Search for the 1 identical target text
        val matches = TextPositionMatcher.searchAllInPositions(
            positions = positions,
            targetTextOrRegex = target,
            pageIndex = pageIndex,
            pageHeight = pageHeight
        )

        assertEquals("Harus mendeteksi ketiga kemunculan teks yang sama (1 teks 2+ multi TTD)", 3, matches.size)
        assertEquals(50f, matches[0].x, 0.01f)
        assertEquals(expectedStartX2, matches[1].x, 0.01f)
        assertEquals(50f, matches[2].x, 0.01f)
        assertEquals(pageHeight - 500f, matches[2].y, 0.01f)
        assertEquals(DetectionMethod.TEXT_SEARCH, matches[0].source)
        assertEquals(DetectionMethod.TEXT_SEARCH, matches[1].source)
        assertEquals(DetectionMethod.TEXT_SEARCH, matches[2].source)
    }

    @Test
    fun testSignatureQrPayload_serializationAndDeserialization() {
        val originalPayload = SignatureQrPayload(
            doc = "Surat Perjanjian Kerjasama",
            signer = "Budi Santoso",
            ts = 1727481600000L,
            id = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
            hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        )

        val jsonString = originalPayload.toJson()

        assertTrue(jsonString.contains("\"doc\":\"Surat Perjanjian Kerjasama\""))
        assertTrue(jsonString.contains("\"signer\":\"Budi Santoso\""))
        assertTrue(jsonString.contains("\"ts\":1727481600000"))
        assertTrue(jsonString.contains("\"id\":\"f47ac10b-58cc-4372-a567-0e02b2c3d479\""))
        assertTrue(jsonString.contains("\"hash\":\"e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\""))

        // Deserialize back
        val parsedPayload = SignatureQrPayload.fromJson(jsonString)
        assertEquals(originalPayload.doc, parsedPayload.doc)
        assertEquals(originalPayload.signer, parsedPayload.signer)
        assertEquals(originalPayload.ts, parsedPayload.ts)
        assertEquals(originalPayload.id, parsedPayload.id)
        assertEquals(originalPayload.hash, parsedPayload.hash)
    }

    @Test
    fun testSha256Calculation() {
        val testInput = "PDF Content Test".toByteArray(Charsets.UTF_8)
        val hash = PdfSignerService.calculateSha256(testInput)

        assertNotNull(hash)
        assertEquals(64, hash.length) // SHA-256 is 64 hex characters
    }

    @Test
    fun testBackupFileNameFormat() {
        val timeStampRegex = "^PDF_Signer_Backup_\\d{8}_\\d{6}\\.zip$".toRegex()
        val sampleName = "PDF_Signer_Backup_20260928_060000.zip"
        assertTrue("Nama file backup harus sesuai format ZIP bertanggal", sampleName.matches(timeStampRegex))
    }

    @Test
    fun testBatchProgressStateFractionCalculation() {
        val emptyState = com.example.model.BatchProgressState(totalCount = 0, currentIndex = 0)
        assertEquals(0f, emptyState.progressFraction, 0.001f)

        val halfState = com.example.model.BatchProgressState(totalCount = 4, currentIndex = 2)
        assertEquals(0.5f, halfState.progressFraction, 0.001f)

        val fullState = com.example.model.BatchProgressState(totalCount = 10, currentIndex = 10)
        assertEquals(1.0f, fullState.progressFraction, 0.001f)
    }

    @Test
    fun testQrOverlayConfigDefaults() {
        val config = com.example.model.QrOverlayConfig()
        assertEquals(com.example.model.QrCenterOverlayType.INITIALS, config.type)
        assertEquals("HW", config.initials)
        assertNull(config.logoBitmap)
        assertNull(config.logoUri)
    }

    @Test
    fun testCustomQrPlacementCreation() {
        val placement = com.example.model.CustomQrPlacement(
            pageIndex = 0,
            normalizedX = 0.7f,
            normalizedY = 0.8f
        )
        assertEquals(0, placement.pageIndex)
        assertEquals(0.7f, placement.normalizedX, 0.001f)
        assertEquals(0.8f, placement.normalizedY, 0.001f)
        assertEquals(75f, placement.qrSizeDp, 0.001f)
        assertEquals(com.example.model.DetectionMethod.MANUAL_DRAG.name, "MANUAL_DRAG")

        val customSizedPlacement = com.example.model.CustomQrPlacement(
            pageIndex = 1,
            normalizedX = 0.5f,
            normalizedY = 0.5f,
            qrSizeDp = 100f
        )
        assertEquals(100f, customSizedPlacement.qrSizeDp, 0.001f)
    }

    @Test
    fun testAppThemePreferences() {
        val defaultMode = com.example.model.AppThemeMode.LIGHT
        val defaultPalette = com.example.model.ColorPaletteStyle.OCEAN_BLUE
        assertEquals("LIGHT", defaultMode.name)
        assertEquals("OCEAN_BLUE", defaultPalette.name)
        assertEquals(4, com.example.model.ColorPaletteStyle.entries.size)
    }

    @Test
    fun testComputeInitialsAndSignerName() {
        assertEquals("BS", com.example.ui.MainViewModel.computeInitials("Budi Santoso"))
        assertEquals("HW", com.example.ui.MainViewModel.computeInitials("Hendra Wijaya"))
        assertEquals("JD", com.example.ui.MainViewModel.computeInitials("John Doe"))
        assertEquals("JO", com.example.ui.MainViewModel.computeInitials("John"))
        assertEquals("", com.example.ui.MainViewModel.computeInitials(""))
        assertEquals("", com.example.ui.MainViewModel.computeInitials("   "))
    }

    @Test
    fun testQrCenterOverlayTypeValues() {
        assertEquals("NONE", com.example.model.QrCenterOverlayType.NONE.name)
        assertEquals("INITIALS", com.example.model.QrCenterOverlayType.INITIALS.name)
        assertEquals("CUSTOM_LOGO", com.example.model.QrCenterOverlayType.CUSTOM_LOGO.name)
    }

    @Test
    fun testAutoFitVerticalSpaceDetection() {
        val pageHeight = 842f
        val pageIndex = 0
        val target = "\${ttd_pengirim1}"
        val positions = mutableListOf<CharPosition>()

        // Placeholder at top-down Y=300f, height=12f (bottom is 312f)
        var curX = 60f
        for (ch in target) {
            positions.add(CharPosition(ch.toString(), curX, 300f, 9f, 12f, pageIndex, pageHeight))
            curX += 9f
        }

        // Sentence/name below at top-down Y=362f (gap = 362 - 312 = 50f)
        val nameText = "(Budi Santoso, S.T.)"
        var nameX = 60f
        for (ch in nameText) {
            positions.add(CharPosition(ch.toString(), nameX, 362f, 8f, 12f, pageIndex, pageHeight))
            nameX += 8f
        }

        val matches = TextPositionMatcher.searchAllInPositions(
            positions = positions,
            targetTextOrRegex = target,
            pageIndex = pageIndex,
            pageHeight = pageHeight
        )

        assertEquals(1, matches.size)
        val match = matches.first()
        assertNotNull(match.availableVerticalSpace)
        assertEquals(50f, match.availableVerticalSpace!!, 0.5f)
        assertNotNull(match.textBelowPdfY)
        assertEquals(pageHeight - 362f, match.textBelowPdfY!!, 0.5f)

        // Verify adaptive size: gap 50f -> size (50 - 8) = 42f (fits perfectly without collision)
        val adaptiveSize = (match.availableVerticalSpace!! - 8f).coerceIn(38f, 85f)
        assertEquals(42f, adaptiveSize, 0.5f)
    }
}
