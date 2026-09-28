package com.example.pdf

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField
import java.io.File
import java.io.FileOutputStream

object PdfSampleGenerator {

    enum class SampleType {
        TEXT_PLACEHOLDER,
        ACROFORM_FIELD,
        NO_PLACEHOLDER_FALLBACK
    }

    fun generateSample(context: Context, type: SampleType): File {
        PDFBoxResourceLoader.init(context)

        val outputDir = File(context.cacheDir, "sample_pdfs")
        if (!outputDir.exists()) outputDir.mkdirs()

        val fileName = when (type) {
            SampleType.TEXT_PLACEHOLDER -> "dokumen_placeholder_teks.pdf"
            SampleType.ACROFORM_FIELD -> "dokumen_acroform_field.pdf"
            SampleType.NO_PLACEHOLDER_FALLBACK -> "dokumen_tanpa_placeholder_fallback.pdf"
        }
        val file = File(outputDir, fileName)

        val doc = PDDocument()
        try {
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)

            when (type) {
                SampleType.TEXT_PLACEHOLDER -> writeTextPlaceholderContent(doc, page)
                SampleType.ACROFORM_FIELD -> writeAcroFormContent(doc, page)
                SampleType.NO_PLACEHOLDER_FALLBACK -> writeFallbackContent(doc, page)
            }

            FileOutputStream(file).use { out ->
                doc.save(out)
            }
        } finally {
            doc.close()
        }

        return file
    }

    private fun writeTextPlaceholderContent(doc: PDDocument, page: PDPage) {
        val stream = PDPageContentStream(doc, page)
        try {
            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA_BOLD, 16f)
            stream.newLineAtOffset(50f, 750f)
            stream.showText("SURAT PERSETUJUAN RESMI")
            stream.endText()

            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA, 11f)
            stream.newLineAtOffset(50f, 710f)
            stream.showText("Nomor Dokumen: REG/2026/09/X-882")
            stream.endText()

            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA, 10f)
            stream.newLineAtOffset(50f, 670f)
            stream.showText("Dengan ini diterangkan bahwa dokumen ini memerlukan verifikasi tanda tangan digital.")
            stream.endText()

            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA, 10f)
            stream.newLineAtOffset(50f, 650f)
            stream.showText("Sistem akan mendeteksi token penandatangan di bawah secara otomatis:")
            stream.endText()

            // Signature area with the target placeholder ${ttd_pengirim1}
            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA_BOLD, 11f)
            stream.newLineAtOffset(350f, 320f)
            stream.showText("Pihak Penandatangan,")
            stream.endText()

            // Target literal placeholder
            stream.beginText()
            stream.setFont(PDType1Font.COURIER_BOLD, 12f)
            stream.newLineAtOffset(350f, 250f)
            stream.showText("\${ttd_pengirim1}")
            stream.endText()

            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA, 11f)
            stream.newLineAtOffset(350f, 180f)
            stream.showText("Dr. Hendra Wijaya, S.Kom., M.T.")
            stream.endText()

            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA_OBLIQUE, 9f)
            stream.newLineAtOffset(350f, 165f)
            stream.showText("NIP. 19850412 201012 1 004")
            stream.endText()

        } finally {
            stream.close()
        }
    }

    private fun writeAcroFormContent(doc: PDDocument, page: PDPage) {
        val stream = PDPageContentStream(doc, page)
        try {
            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA_BOLD, 16f)
            stream.newLineAtOffset(50f, 750f)
            stream.showText("BERITA ACARA KESEPAKATAN KERJASAMA")
            stream.endText()

            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA, 11f)
            stream.newLineAtOffset(50f, 710f)
            stream.showText("Tipe Formulir: AcroForm Digital Fillable Field")
            stream.endText()

            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA, 10f)
            stream.newLineAtOffset(50f, 660f)
            stream.showText("Formulir ini memiliki field AcroForm khusus bernama 'ttd_pengirim1'.")
            stream.endText()

            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA, 10f)
            stream.newLineAtOffset(50f, 640f)
            stream.showText("QR Code akan ditempelkan tepat pada kotak widget form field tersebut.")
            stream.endText()

            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA_BOLD, 11f)
            stream.newLineAtOffset(70f, 320f)
            stream.showText("Kotak Tanda Tangan (AcroForm Field):")
            stream.endText()
        } finally {
            stream.close()
        }

        // Add AcroForm field
        val acroForm = PDAcroForm(doc)
        doc.documentCatalog.acroForm = acroForm

        val textField = PDTextField(acroForm)
        textField.partialName = "ttd_pengirim1"

        val widget = textField.widgets.firstOrNull() ?: return
        val rect = PDRectangle(70f, 190f, 110f, 110f)
        widget.rectangle = rect
        widget.page = page

        page.annotations.add(widget)
        acroForm.fields.add(textField)
    }

    private fun writeFallbackContent(doc: PDDocument, page: PDPage) {
        val stream = PDPageContentStream(doc, page)
        try {
            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA_BOLD, 16f)
            stream.newLineAtOffset(50f, 750f)
            stream.showText("DOKUMEN BIASA (TEST FALLBACK)")
            stream.endText()

            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA, 10f)
            stream.newLineAtOffset(50f, 700f)
            stream.showText("Dokumen ini sengaja TIDAK memiliki placeholder ataupun field AcroForm.")
            stream.endText()

            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA, 10f)
            stream.newLineAtOffset(50f, 680f)
            stream.showText("Sistem harus mendeteksi ketiadaan placeholder dan mengaktifkan mode FALLBACK.")
            stream.endText()

            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA, 10f)
            stream.newLineAtOffset(50f, 660f)
            stream.showText("QR Code akan ditempelkan pada sudut kanan bawah di halaman terakhir secara aman.")
            stream.endText()
        } finally {
            stream.close()
        }
    }
}
