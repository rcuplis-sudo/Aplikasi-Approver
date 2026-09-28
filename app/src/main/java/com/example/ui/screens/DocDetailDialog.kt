package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.SignedDocumentEntity
import com.example.model.DetectionMethod
import com.example.model.SignatureQrPayload
import com.example.pdf.PdfPreviewRenderer
import com.example.qr.QrCodeGenerator
import com.example.ui.PdfFileHelper
import com.example.ui.theme.AmberFallback
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyLight
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocDetailDialog(
    document: SignedDocumentEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val formattedDate = remember(document.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
        sdf.format(Date(document.timestamp))
    }

    var renderedPdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRenderingPdf by remember { mutableStateOf(false) }

    LaunchedEffect(document.signedFilePath) {
        val file = File(document.signedFilePath)
        if (file.exists()) {
            isRenderingPdf = true
            try {
                renderedPdfBitmap = PdfPreviewRenderer.renderPageToBitmap(
                    context = context,
                    inputSource = file,
                    pageIndex = document.pageIndex,
                    scale = 1.2f
                )
            } catch (e: Exception) {
                renderedPdfBitmap = null
            } finally {
                isRenderingPdf = false
            }
        }
    }

    val qrBitmap: Bitmap? = remember(document.qrPayloadJson) {
        try {
            QrCodeGenerator.generateQrBitmap(document.qrPayloadJson, 280, 280)
        } catch (e: Exception) {
            null
        }
    }

    val prettyJson = remember(document.qrPayloadJson) {
        try {
            SignatureQrPayload.fromJson(document.qrPayloadJson).toPrettyJson()
        } catch (e: Exception) {
            document.qrPayloadJson
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("doc_detail_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detail Dokumen & QR",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_detail_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                // Detection Badge
                val (badgeColor, badgeText) = when (document.detectionMethod) {
                    DetectionMethod.ACROFORM.name -> Pair(EmeraldSuccess, "AcroForm Field")
                    DetectionMethod.TEXT_SEARCH.name -> Pair(NavyLight, "Text Search")
                    else -> Pair(AmberFallback, "Fallback (Pojok Bawah)")
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Metode: $badgeText",
                        color = badgeColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Text(
                    text = document.documentTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Info Rows
                InfoRow("Signer", document.signerName)
                InfoRow("Waktu", formattedDate)
                InfoRow("Posisi", "Hal ${document.pageIndex + 1} (X: ${document.posX.toInt()}, Y: ${document.posY.toInt()})")
                InfoRow("Keterangan", document.detectionDetails)

                // Rendered PDF Page with Embedded QR (PDFBox Renderer)
                if (renderedPdfBitmap != null || isRenderingPdf) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Preview Halaman Dokumen Bertanda Tangan (PDFBox)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        if (isRenderingPdf) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        } else {
                            val bitmap = renderedPdfBitmap
                            if (bitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(6.dp))
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Halaman PDF bertanda tangan",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 240.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // QR Code Image
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code Signature",
                            modifier = Modifier
                                .size(160.dp)
                                .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        )
                    }
                }

                // SHA-256 Hash Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SHA-256 Dokumen",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                PdfFileHelper.copyToClipboard(context, "SHA-256", document.sha256Hash)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Salin Hash",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = document.sha256Hash,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // JSON Payload Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QR Payload JSON {doc, signer, ts, id, hash}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                PdfFileHelper.copyToClipboard(context, "QR JSON Payload", prettyJson)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Salin JSON",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = prettyJson,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    )
                }

                // Actions: Open and Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { PdfFileHelper.openPdf(context, document.signedFilePath) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_open_pdf_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka PDF")
                    }
                    OutlinedButton(
                        onClick = { PdfFileHelper.sharePdf(context, document.signedFilePath, document.documentTitle) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_share_pdf_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bagikan")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
