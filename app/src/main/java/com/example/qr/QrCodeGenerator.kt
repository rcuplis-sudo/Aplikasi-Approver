package com.example.qr

import android.graphics.*
import com.example.model.QrCenterOverlayType
import com.example.model.QrOverlayConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream

object QrCodeGenerator {

    /**
     * Generates a QR Code as an Android Bitmap from the provided content string.
     * Uses ErrorCorrectionLevel.H (30% redundancy) when an overlay logo or initials
     * is embedded at the center, ensuring the QR code remains 100% scannable.
     */
    fun generateQrBitmap(
        content: String,
        width: Int = 300,
        height: Int = 300,
        overlayConfig: QrOverlayConfig? = null
    ): Bitmap {
        val hints = HashMap<EncodeHintType, Any>()
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        // Use High error correction (30%) if embedding a center logo/initials to ensure scan reliability
        hints[EncodeHintType.ERROR_CORRECTION] = if (overlayConfig != null && overlayConfig.type != QrCenterOverlayType.NONE) {
            ErrorCorrectionLevel.H
        } else {
            ErrorCorrectionLevel.M
        }
        hints[EncodeHintType.MARGIN] = 1

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)

        val matrixWidth = bitMatrix.width
        val matrixHeight = bitMatrix.height
        val pixels = IntArray(matrixWidth * matrixHeight)

        for (y in 0 until matrixHeight) {
            val offset = y * matrixWidth
            for (x in 0 until matrixWidth) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }

        val baseBitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
        baseBitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)

        // Embed overlay logo or initials if configured
        if (overlayConfig != null && overlayConfig.type != QrCenterOverlayType.NONE) {
            return embedCenterOverlay(baseBitmap, overlayConfig)
        }

        return baseBitmap
    }

    /**
     * Embeds a circular/rounded badge with either user-defined initials or custom logo image
     * at the exact center of the QR code.
     */
    fun embedCenterOverlay(qrBitmap: Bitmap, config: QrOverlayConfig): Bitmap {
        val width = qrBitmap.width
        val height = qrBitmap.height

        // Overlay occupies around 22% - 25% of QR code dimensions (safe for Level H)
        val overlaySize = (minOf(width, height) * 0.24f).toInt().coerceAtLeast(40)
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = overlaySize / 2f

        val combinedBitmap = qrBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(combinedBitmap)

        // 1. Draw outer circular badge background with subtle border
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.backgroundColor
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.textColor
            style = Paint.Style.STROKE
            strokeWidth = (overlaySize * 0.06f).coerceAtLeast(2f)
        }

        // Draw background circle & border
        canvas.drawCircle(centerX, centerY, radius, bgPaint)
        canvas.drawCircle(centerX, centerY, radius, borderPaint)

        // 2. Draw content based on type
        when (config.type) {
            QrCenterOverlayType.INITIALS -> {
                val cleanInitials = config.initials.trim().uppercase().take(3).ifBlank { "QR" }
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = config.textColor
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    // Auto scale text size to fit badge
                    val charCountFactor = if (cleanInitials.length == 1) 0.55f else if (cleanInitials.length == 2) 0.44f else 0.35f
                    textSize = overlaySize * charCountFactor
                }

                // Vertical text alignment at center
                val fontMetrics = textPaint.fontMetrics
                val textY = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f

                canvas.drawText(cleanInitials, centerX, textY, textPaint)
            }
            QrCenterOverlayType.CUSTOM_LOGO -> {
                val logo = config.logoBitmap
                if (logo != null) {
                    val innerPadding = (overlaySize * 0.16f).toInt()
                    val targetLogoSize = overlaySize - (innerPadding * 2)

                    // Crop/scale logo to fit circular area
                    val scaledLogo = Bitmap.createScaledBitmap(logo, targetLogoSize, targetLogoSize, true)

                    // Create circular cropped bitmap
                    val circularLogo = Bitmap.createBitmap(targetLogoSize, targetLogoSize, Bitmap.Config.ARGB_8888)
                    val logoCanvas = Canvas(circularLogo)
                    val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                    val rect = RectF(0f, 0f, targetLogoSize.toFloat(), targetLogoSize.toFloat())
                    logoCanvas.drawRoundRect(rect, targetLogoSize / 2f, targetLogoSize / 2f, clipPaint)
                    clipPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                    logoCanvas.drawBitmap(scaledLogo, 0f, 0f, clipPaint)

                    val logoLeft = centerX - targetLogoSize / 2f
                    val logoTop = centerY - targetLogoSize / 2f
                    canvas.drawBitmap(circularLogo, logoLeft, logoTop, null)
                } else {
                    // Fallback to initials if logo bitmap is null
                    val cleanInitials = config.initials.trim().uppercase().take(2).ifBlank { "ID" }
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = config.textColor
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                        textSize = overlaySize * 0.45f
                    }
                    val fontMetrics = textPaint.fontMetrics
                    val textY = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
                    canvas.drawText(cleanInitials, centerX, textY, textPaint)
                }
            }
            QrCenterOverlayType.NONE -> {
                // Do nothing
            }
        }

        return combinedBitmap
    }

    /**
     * Converts a Bitmap to PNG byte array.
     */
    fun bitmapToPngBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
