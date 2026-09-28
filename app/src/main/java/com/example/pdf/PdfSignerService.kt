package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.local.SignedDocumentEntity
import com.example.data.repository.SignedDocumentRepository
import com.example.model.DetectionMethod
import com.example.model.PlaceholderMatch
import com.example.model.SignatureQrPayload
import com.example.qr.QrCodeGenerator
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
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
    val signedFile: File
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
     */
    suspend fun signPdf(
        documentTitle: String,
        signerName: String,
        inputSource: Any, // Uri or File or ByteArray
        originalFileName: String,
        customPlaceholder: String = "\${ttd_pengirim1}"
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

        // 2. Load PDF and Detect Placeholder (AcroForm, Text Search, or Fallback)
        val document = PDDocument.load(inputBytes)
        val detector = PdfPlaceholderDetector(customPlaceholder)
        val match = detector.detectPlaceholder(document)

        // 3. Prepare QR Payload JSON
        val signatureId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val payload = SignatureQrPayload(
            doc = documentTitle.ifBlank { originalFileName },
            signer = signerName.ifBlank { "Penandatangan Resmi" },
            ts = timestamp,
            id = signatureId,
            hash = docHash
        )
        val payloadJson = payload.toJson()

        // 4. Generate QR Code Bitmap with ZXing
        val qrBitmap = QrCodeGenerator.generateQrBitmap(payloadJson, 350, 350)

        // 5. Embed QR Code into PDF at coordinates
        try {
            embedQrToDocument(document, match, qrBitmap, signatureId, signerName)

            // 6. Save Signed Document to App Files Directory
            val outputDir = File(context.filesDir, "signed_pdfs")
            if (!outputDir.exists()) outputDir.mkdirs()

            val safeOriginalName = originalFileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val signedFileName = "signed_${timestamp}_$safeOriginalName"
            val signedFile = File(outputDir, signedFileName)

            FileOutputStream(signedFile).use { outStream: FileOutputStream ->
                document.save(outStream)
            }

            // 7. Save History to Room
            val entity = SignedDocumentEntity(
                id = signatureId,
                documentTitle = payload.doc,
                signerName = payload.signer,
                timestamp = timestamp,
                sha256Hash = docHash,
                qrPayloadJson = payloadJson,
                originalFileName = originalFileName,
                signedFilePath = signedFile.absolutePath,
                pageIndex = match.pageIndex,
                posX = match.x,
                posY = match.y,
                detectionMethod = match.source.name,
                fallbackUsed = match.source == DetectionMethod.FALLBACK,
                detectionDetails = match.details
            )

            repository.insertDocument(entity)

            return@withContext SigningResult(
                entity = entity,
                payload = payload,
                qrBitmap = qrBitmap,
                match = match,
                signedFile = signedFile
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
        signerName: String
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
        }

        val contentStream = PDPageContentStream(
            document,
            targetPage,
            PDPageContentStream.AppendMode.APPEND,
            true,
            true
        )

        try {
            // Draw the QR Code image
            contentStream.drawImage(qrImage, drawX, drawY, qrSize, qrSize)

            // Draw a subtle digital signature annotation label
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 7f)
            contentStream.newLineAtOffset(drawX, (drawY - 9f).coerceAtLeast(10f))
            contentStream.showText("DIGITALLY SIGNED • ID: ${sigId.take(8)}")
            contentStream.endText()
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
