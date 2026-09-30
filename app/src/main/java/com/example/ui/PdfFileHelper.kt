package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PdfFileHelper {

    fun openPdf(context: Context, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File tidak ditemukan: $filePath", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Buka Dokumen PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak ada aplikasi pembuka PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Share PDF to WhatsApp, Telegram, Gmail, Drive, Bluetooth, etc.
     */
    fun sharePdf(context: Context, filePath: String, title: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File tidak ditemukan: $filePath", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Dokumen PDF telah ditandatangani secara digital dengan verifikasi QR: $title")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Kirim / Bagikan PDF ke WhatsApp atau Lainnya").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Export / Download PDF to device's public Download folder so user can easily find it in File Manager.
     */
    fun downloadPdfToDevice(context: Context, filePath: String, suggestedFileName: String? = null): Boolean {
        val srcFile = File(filePath)
        if (!srcFile.exists()) {
            Toast.makeText(context, "Berkas asal tidak ditemukan", Toast.LENGTH_SHORT).show()
            return false
        }

        val targetName = suggestedFileName ?: srcFile.name
        val cleanName = if (targetName.endsWith(".pdf", ignoreCase = true)) targetName else "$targetName.pdf"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PDF_Signer")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException("Gagal membuat entri file di folder Download")

                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(srcFile).use { inStream ->
                        inStream.copyTo(out)
                    }
                }
                Toast.makeText(context, "Berhasil diunduh ke folder Download/PDF_Signer/$cleanName", Toast.LENGTH_LONG).show()
                true
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "PDF_Signer")
                if (!targetDir.exists()) targetDir.mkdirs()

                val destFile = File(targetDir, cleanName)
                FileInputStream(srcFile).use { inStream ->
                    FileOutputStream(destFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                Toast.makeText(context, "Berhasil diunduh ke ${destFile.absolutePath}", Toast.LENGTH_LONG).show()
                true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal mengunduh file: ${e.localizedMessage ?: "Izin penyimpanan bermasalah"}", Toast.LENGTH_LONG).show()
            false
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label disalin ke clipboard", Toast.LENGTH_SHORT).show()
    }
}
