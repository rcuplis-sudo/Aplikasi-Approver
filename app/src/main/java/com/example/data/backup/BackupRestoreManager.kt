package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.local.SignedDocumentEntity
import com.example.data.repository.SignedDocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupResult(
    val backupFile: File,
    val totalDocuments: Int,
    val totalPdfFiles: Int,
    val fileSizeFormatted: String
)

data class RestoreResult(
    val restoredDocuments: Int,
    val restoredPdfFiles: Int,
    val message: String
)

object BackupRestoreManager {

    private const val MANIFEST_FILENAME = "backup_manifest.json"
    private const val PDF_FOLDER_NAME = "signed_pdfs"

    /**
     * Creates a self-contained ZIP backup containing:
     * 1. backup_manifest.json (Metadata array from Room Database)
     * 2. signed_pdfs/ (All physical PDF files)
     */
    suspend fun createBackup(
        context: Context,
        repository: SignedDocumentRepository
    ): BackupResult = withContext(Dispatchers.IO) {
        val documents = repository.getAllDocumentsList()
        val backupDir = File(context.cacheDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupFile = File(backupDir, "PDF_Signer_Backup_$timeStamp.zip")

        var pdfFilesCount = 0

        ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
            // 1. Create Manifest JSON
            val rootJson = JSONObject()
            rootJson.put("version", 1)
            rootJson.put("app", "PDF QR Signer")
            rootJson.put("createdAt", System.currentTimeMillis())
            rootJson.put("count", documents.size)

            val array = JSONArray()
            for (doc in documents) {
                val docObj = JSONObject()
                docObj.put("id", doc.id)
                docObj.put("documentTitle", doc.documentTitle)
                docObj.put("signerName", doc.signerName)
                docObj.put("timestamp", doc.timestamp)
                docObj.put("sha256Hash", doc.sha256Hash)
                docObj.put("qrPayloadJson", doc.qrPayloadJson)
                docObj.put("originalFileName", doc.originalFileName)
                // Relative file name inside ZIP
                val originalFile = File(doc.signedFilePath)
                val relativeName = originalFile.name
                docObj.put("relativePdfName", relativeName)
                docObj.put("pageIndex", doc.pageIndex)
                docObj.put("posX", doc.posX.toDouble())
                docObj.put("posY", doc.posY.toDouble())
                docObj.put("detectionMethod", doc.detectionMethod)
                docObj.put("fallbackUsed", doc.fallbackUsed)
                docObj.put("detectionDetails", doc.detectionDetails)
                array.put(docObj)

                // 2. Add PDF file to ZIP if exists
                if (originalFile.exists()) {
                    val entryName = "$PDF_FOLDER_NAME/$relativeName"
                    zipOut.putNextEntry(ZipEntry(entryName))
                    FileInputStream(originalFile).use { inStream ->
                        inStream.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                    pdfFilesCount++
                }
            }
            rootJson.put("documents", array)

            // Write Manifest
            zipOut.putNextEntry(ZipEntry(MANIFEST_FILENAME))
            zipOut.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
        }

        val sizeKb = backupFile.length() / 1024
        val sizeFormatted = if (sizeKb >= 1024) "%.2f MB".format(sizeKb / 1024.0) else "$sizeKb KB"

        return@withContext BackupResult(
            backupFile = backupFile,
            totalDocuments = documents.size,
            totalPdfFiles = pdfFilesCount,
            fileSizeFormatted = sizeFormatted
        )
    }

    /**
     * Restores database records and physical PDF files from a backup ZIP.
     */
    suspend fun restoreBackup(
        context: Context,
        zipUri: Uri,
        repository: SignedDocumentRepository,
        replaceExisting: Boolean = false
    ): RestoreResult = withContext(Dispatchers.IO) {
        val targetPdfDir = File(context.filesDir, "signed_pdfs")
        if (!targetPdfDir.exists()) targetPdfDir.mkdirs()

        val tempExtractDir = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}")
        if (!tempExtractDir.exists()) tempExtractDir.mkdirs()

        var manifestJsonString: String? = null
        var restoredPdfCount = 0

        // Extract ZIP
        context.contentResolver.openInputStream(zipUri)?.use { rawIn ->
            ZipInputStream(rawIn).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    if (entryName == MANIFEST_FILENAME) {
                        manifestJsonString = zipIn.bufferedReader(Charsets.UTF_8).readText()
                    } else if (entryName.startsWith("$PDF_FOLDER_NAME/") && !entry.isDirectory) {
                        val fileName = File(entryName).name
                        val targetFile = File(targetPdfDir, fileName)
                        FileOutputStream(targetFile).use { out ->
                            zipIn.copyTo(out)
                        }
                        restoredPdfCount++
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        } ?: throw IllegalArgumentException("Gagal membuka file backup ZIP")

        if (manifestJsonString.isNullOrBlank()) {
            throw IllegalArgumentException("File backup tidak valid: berkas '$MANIFEST_FILENAME' tidak ditemukan.")
        }

        // Parse Manifest JSON
        val rootJson = JSONObject(manifestJsonString!!)
        val array = rootJson.getJSONArray("documents")

        if (replaceExisting) {
            repository.clearAll()
        }

        val entitiesToInsert = mutableListOf<SignedDocumentEntity>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val relativePdfName = obj.optString("relativePdfName", "")
            val resolvedPath = File(targetPdfDir, relativePdfName).absolutePath

            val entity = SignedDocumentEntity(
                id = obj.getString("id"),
                documentTitle = obj.optString("documentTitle", "Dokumen Terpulihkan"),
                signerName = obj.optString("signerName", "Penandatangan"),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                sha256Hash = obj.optString("sha256Hash", ""),
                qrPayloadJson = obj.optString("qrPayloadJson", "{}"),
                originalFileName = obj.optString("originalFileName", "document.pdf"),
                signedFilePath = resolvedPath,
                pageIndex = obj.optInt("pageIndex", 0),
                posX = obj.optDouble("posX", 0.0).toFloat(),
                posY = obj.optDouble("posY", 0.0).toFloat(),
                detectionMethod = obj.optString("detectionMethod", "FALLBACK"),
                fallbackUsed = obj.optBoolean("fallbackUsed", false),
                detectionDetails = obj.optString("detectionDetails", "Dipulihkan dari backup")
            )
            entitiesToInsert.add(entity)
        }

        repository.insertAll(entitiesToInsert)

        // Cleanup temp
        tempExtractDir.deleteRecursively()

        return@withContext RestoreResult(
            restoredDocuments = entitiesToInsert.size,
            restoredPdfFiles = restoredPdfCount,
            message = "Berhasil memulihkan ${entitiesToInsert.size} metadata dokumen dan $restoredPdfCount file PDF fisik."
        )
    }
}
