package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

data class PdfDocumentPreviewInfo(
    val pageCount: Int,
    val renderedPages: Map<Int, Bitmap>
)

object PdfPreviewRenderer {

    /**
     * Renders a specific page or the first page of a PDF document to an Android Bitmap using PDFBox PDFRenderer.
     */
    suspend fun renderPageToBitmap(
        context: Context,
        inputSource: Any, // Uri, File, or ByteArray
        pageIndex: Int = 0,
        scale: Float = 1.3f
    ): Bitmap? = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)

        val inputBytes: ByteArray = when (inputSource) {
            is Uri -> {
                context.contentResolver.openInputStream(inputSource)?.use { it.readBytes() } ?: return@withContext null
            }
            is File -> inputSource.readBytes()
            is ByteArray -> inputSource
            is InputStream -> inputSource.readBytes()
            else -> return@withContext null
        }

        var document: PDDocument? = null
        try {
            document = PDDocument.load(inputBytes)
            val totalPages = document.numberOfPages
            if (pageIndex < 0 || pageIndex >= totalPages) return@withContext null

            val renderer = PDFRenderer(document)
            // PDFBox-Android renderImage returns android.graphics.Bitmap
            val bitmap: Bitmap = renderer.renderImage(pageIndex, scale)
            return@withContext bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            document?.close()
        }
    }

    /**
     * Retrieves the total number of pages in the PDF document.
     */
    suspend fun getPageCount(
        context: Context,
        inputSource: Any
    ): Int = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)

        val inputBytes: ByteArray = when (inputSource) {
            is Uri -> {
                context.contentResolver.openInputStream(inputSource)?.use { it.readBytes() } ?: return@withContext 0
            }
            is File -> inputSource.readBytes()
            is ByteArray -> inputSource
            is InputStream -> inputSource.readBytes()
            else -> return@withContext 0
        }

        var document: PDDocument? = null
        try {
            document = PDDocument.load(inputBytes)
            return@withContext document.numberOfPages
        } catch (e: Exception) {
            0
        } finally {
            document?.close()
        }
    }
}
