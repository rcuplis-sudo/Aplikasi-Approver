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

        assertNull("Jika placeholder tidak ada, harus mengembalikan null untuk memicu fallback", result)
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
}
