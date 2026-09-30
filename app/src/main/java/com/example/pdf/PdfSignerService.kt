package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.local.SignedDocumentEntity
import com.example.data.repository.SignedDocumentRepository
import com.example.model.CustomQrPlacement
import com.example.model.DetectionMethod
import com.example.model.PlaceholderMatch
import com.example.model.QrOverlayConfig
import com.example.model.SignatureQrPayload
import com.example.qr.QrCodeGenerator
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

data class SigningResult(
    val entity: SignedDocumentEntity,
    val payload: SignatureQrPayload,
    val qrBitmap: Bitmap,
    val match: PlaceholderMatch,
    val signedFile: File,
    val allMatches: List<PlaceholderMatch> = listOf(match),
    val signaturesCount: Int = 1
)

class PdfSignerService(
    private val context: Context,
    private val repository: SignedDocumentRepository
) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    /**
     * Signs a PDF provided via Uri or File.
     * If customPlacement is specified, the QR is stamped exactly at the custom position (touch-and-drag).
     * Otherwise, automatically finds all placeholders in the document.
     */
    suspend fun signPdf(
        documentTitle: String,
        signerName: String,
        inputSource: Any, // Uri or File or ByteArray
        originalFileName: String,
        customPlaceholder: String = "\${ttd_pengirim1}",
        qrOverlayConfig: QrOverlayConfig? = null,
        customPlacement: CustomQrPlacement? = null
    ): SigningResult = withContext(Dispatchers.IO) {
        val inputBytes: ByteArray = when (inputSource) {
            is Uri -> {
                context.contentResolver.openInputStream(inputSource)?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("Gagal membaca file dari URI")
            }
            is File -> inputSource.readBytes()
            is ByteArray -> inputSource
            is InputStream -> inputSource.readBytes()
            else -> throw IllegalArgumentException("Format sumber dokumen tidak didukung")
        }

        // 1. Calculate Document Hash (SHA-256)
        val docHash = calculateSha256(inputBytes)

        // 2. Load PDF and Determine Placeholders
        val document = PDDocument.load(inputBytes)

        val allMatches: List<PlaceholderMatch> = if (customPlacement != null) {
            val pageCount = document.numberOfPages.coerceAtLeast(1)
            val targetPageIdx = customPlacement.pageIndex.coerceIn(0, pageCount - 1)
            val page = document.getPage(targetPageIdx)
            val box = page.cropBox ?: page.mediaBox

            val qrSizePts = 85f
            // In PDF coordinate space, (0,0) is bottom-left, while in visual/screen it's top-left
            val pdfX = (customPlacement.normalizedX * box.width).coerceIn(10f, box.width - qrSizePts - 10f)
            val pdfY = ((1f - customPlacement.normalizedY) * box.height - qrSizePts).coerceIn(15f, box.height - qrSizePts - 15f)

            listOf(
                PlaceholderMatch(
                    pageIndex = targetPageIdx,
                    x = pdfX,
                    y = pdfY,
                    width = qrSizePts,
                    height = qrSizePts,
                    source = DetectionMethod.MANUAL_DRAG,
                    fieldName = "Manual Drag & Drop",
                    details = "Posisi QR ditentukan manual via Touch-and-Drag di Halaman ${targetPageIdx + 1}"
                )
            )
        } else {
            val detector = PdfPlaceholderDetector(customPlaceholder)
            detector.detectAllPlaceholders(document)
        }

        val primaryMatch = allMatches.first()

        // 3. Prepare QR Payloads & Embed QR Code into EACH detected placeholder
        val baseSignatureId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        var firstQrBitmap: Bitmap? = null
        var primaryPayload: SignatureQrPayload? = null

        try {
            allMatches.forEachIndexed { index, match ->
                val signIndex = index + 1
                val sigId = if (allMatches.size > 1) "$baseSignatureId-$signIndex" else baseSignatureId
                val effectiveSigner = if (allMatches.size > 1) {
                    val signSuffix = match.fieldName ?: "Pihak $signIndex"
                    "$signerName ($signSuffix)"
                } else {
                    signerName.ifBlank { "Penandatangan Resmi" }
                }

                val payload = SignatureQrPayload(
                    doc = documentTitle.ifBlank { originalFileName },
                    signer = effectiveSigner,
                    ts = timestamp,
                    id = sigId,
                    hash = docHash
                )

                if (index == 0) {
                    primaryPayload = payload
                }

                // Generate QR Code Bitmap with ZXing (with center logo/initials if configured)
                val qrBitmap = QrCodeGenerator.generateQrBitmap(
                    content = payload.toJson(),
                    width = 350,
                    height = 350,
                    overlayConfig = qrOverlayConfig
                )
                if (index == 0) {
                    firstQrBitmap = qrBitmap
                }

                // Embed QR to Document at match coordinates
                embedQrToDocument(document, match, qrBitmap, sigId, effectiveSigner, signIndex, allMatches.size)
            }

            // 4. Save Signed Document to App Files Directory
            val outputDir = File(context.filesDir, "signed_pdfs")
            if (!outputDir.exists()) outputDir.mkdirs()

            val safeOriginalName = originalFileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val signedFileName = "signed_${timestamp}_$safeOriginalName"
            val signedFile = File(outputDir, signedFileName)

            FileOutputStream(signedFile).use { outStream: FileOutputStream ->
                document.save(outStream)
            }

            // 5. Save History to Room
            val detectionSummary = when {
                allMatches.size > 1 -> "Ditemukan ${allMatches.size} placeholder. Seluruh ${allMatches.size} posisi berhasil ditandatangani otomatis."
                primaryMatch.source == DetectionMethod.MANUAL_DRAG -> primaryMatch.details
                else -> primaryMatch.details
            }

            val finalPayload = primaryPayload ?: SignatureQrPayload(
                doc = documentTitle.ifBlank { originalFileName },
                signer = signerName,
                ts = timestamp,
                id = baseSignatureId,
                hash = docHash
            )

            val entity = SignedDocumentEntity(
                id = baseSignatureId,
                documentTitle = finalPayload.doc,
                signerName = if (allMatches.size > 1) "$signerName (${allMatches.size} Tanda Tangan)" else finalPayload.signer,
                timestamp = timestamp,
                sha256Hash = docHash,
                qrPayloadJson = finalPayload.toJson(),
                originalFileName = originalFileName,
                signedFilePath = signedFile.absolutePath,
                pageIndex = primaryMatch.pageIndex,
                posX = primaryMatch.x,
                posY = primaryMatch.y,
                detectionMethod = if (allMatches.size > 1) "MULTI_${primaryMatch.source.name}" else primaryMatch.source.name,
                fallbackUsed = primaryMatch.source == DetectionMethod.FALLBACK,
                detectionDetails = detectionSummary
            )

            repository.insertDocument(entity)

            return@withContext SigningResult(
                entity = entity,
                payload = finalPayload,
                qrBitmap = firstQrBitmap ?: QrCodeGenerator.generateQrBitmap(finalPayload.toJson(), 350, 350),
                match = primaryMatch,
                signedFile = signedFile,
                allMatches = allMatches,
                signaturesCount = allMatches.size
            )
        } finally {
            document.close()
        }
    }

    private fun embedQrToDocument(
        document: PDDocument,
        match: PlaceholderMatch,
        qrBitmap: Bitmap,
        sigId: String,
        signerName: String,
        signatureNumber: Int,
        totalSignatures: Int
    ) {
        val targetPage = document.getPage(match.pageIndex)
        val pageBox = targetPage.cropBox ?: targetPage.mediaBox

        val qrImage = LosslessFactory.createFromImage(document, qrBitmap)

        // Determine size and position based on detection source
        val (drawX, drawY, qrSize) = when (match.source) {
            DetectionMethod.ACROFORM -> {
                val size = minOf(match.width, match.height).coerceIn(60f, 120f)
                val x = match.x + (match.width - size) / 2f
                val y = match.y + (match.height - size) / 2f
                Triple(x, y, size)
            }
            DetectionMethod.TEXT_SEARCH -> {
                val size = 85f
                val x = match.x
                // Position QR just under or over the placeholder text line
                val y = (match.y - size + 10f).coerceIn(20f, pageBox.height - size - 20f)
                Triple(x, y, size)
            }
            DetectionMethod.FALLBACK -> {
                val size = 90f
                Triple(match.x, match.y, size)
            }
            DetectionMethod.MANUAL_DRAG -> {
                val size = match.width.coerceIn(60f, 120f)
                Triple(match.x, match.y, size)
            }
        }

        val contentStream = PDPageContentStream(
            document,
            targetPage,
            PDPageContentStream.AppendMode.APPEND,
            true,
            true
        )

        try {
            // Draw ONLY the QR Code image (no text underneath)
            contentStream.drawImage(qrImage, drawX, drawY, qrSize, qrSize)
        } finally {
            contentStream.close()
        }
    }

    companion object {
        fun calculateSha256(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(bytes)
            return hash.joinToString("") { "%02x".format(it) }
        }
    }
}
