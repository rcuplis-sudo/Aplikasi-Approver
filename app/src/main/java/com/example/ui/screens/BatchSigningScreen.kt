package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BatchFileItem
import com.example.model.BatchItemStatus
import com.example.ui.MainViewModel
import com.example.ui.PdfFileHelper
import com.example.ui.theme.AmberFallback
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyLight
import com.example.ui.theme.PrimaryBlueLight

@Composable
fun BatchSigningScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val batchState by viewModel.batchState.collectAsState()
    val signerName by viewModel.signerName.collectAsState()
    val targetPlaceholder by viewModel.targetPlaceholder.collectAsState()

    // Multiple PDF selector
    val batchPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val list = uris.map { uri ->
                var displayName = "document.pdf"
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex >= 0) {
                            displayName = cursor.getString(nameIndex)
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to last path segment
                    displayName = uri.lastPathSegment ?: "document.pdf"
                }
                Pair(uri, displayName)
            }
            viewModel.addBatchFiles(list)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Batch Auto-Signer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Deteksi & tanda tangani banyak PDF otomatis (mendukung >1 placeholder)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (batchState.items.isNotEmpty() && !batchState.isRunning) {
                IconButton(
                    onClick = { viewModel.clearBatchQueue() },
                    modifier = Modifier.testTag("clear_batch_queue_button")
                ) {
                    Icon(
                        Icons.Outlined.DeleteSweep,
                        contentDescription = "Bersihkan antrean",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Live Progress Card (Always visible when items exist or running)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("batch_progress_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (batchState.isCompleted) {
                    EmeraldSuccess.copy(alpha = 0.12f)
                } else if (batchState.isRunning) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (batchState.isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (batchState.isCompleted) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Layers,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                batchState.isRunning -> "Memproses Dokumen..."
                                batchState.isCompleted -> "Proses Batch Selesai!"
                                batchState.items.isNotEmpty() -> "Antrean Siap Dijalankan"
                                else -> "Antrean Dokumen Kosong"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (batchState.totalCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "${batchState.currentIndex} / ${batchState.totalCount} File",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Progress Bar
                LinearProgressIndicator(
                    progress = { batchState.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .testTag("batch_linear_progress"),
                    color = if (batchState.isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Current file being processed
                if (batchState.isRunning && batchState.currentFileName != null) {
                    Text(
                        text = "Sedang memproses: ${batchState.currentFileName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Real-time Counters Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Success Counter
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldSuccess.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${batchState.successCount} Berhasil",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        }
                    }

                    // Failure Counter
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${batchState.failureCount} Gagal",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Total Signatures Placed
                    Surface(
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(8.dp),
                        color = NavyLight.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.QrCode, contentDescription = null, tint = NavyLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${batchState.totalSignaturesPlaced} QR Ditempel",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NavyLight
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons Row (Add Batch, Load Sample Suite, Start Processing)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { batchPickerLauncher.launch(arrayOf("application/pdf")) },
                enabled = !batchState.isRunning,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("pick_batch_files_button")
            ) {
                Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pilih PDF", style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
                onClick = { viewModel.loadSampleBatchSuite() },
                enabled = !batchState.isRunning,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("load_batch_suite_button")
            ) {
                Icon(Icons.Default.AutoMode, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Muat Dokumen Uji", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Main Execute Button
        Button(
            onClick = { viewModel.startBatchSigning() },
            enabled = batchState.items.isNotEmpty() && !batchState.isRunning,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueLight),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("start_batch_signing_button")
        ) {
            if (batchState.isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Menandatangani Otomatis...")
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Jalankan Batch Auto-Sign (${batchState.items.size} File)")
            }
        }

        // Per-File Success / Failure Realtime Log List
        Text(
            text = "Log Eksekusi Tiap Berkas (${batchState.items.size}):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        if (batchState.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.FolderCopy,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Belum ada dokumen di dalam antrean batch",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Klik 'Pilih PDF' atau 'Muat 4 Dokumen Uji' untuk memulai",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("batch_logs_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(batchState.items, key = { it.id }) { item ->
                    BatchItemLogRow(item = item)
                }
            }
        }
    }
}

@Composable
fun BatchItemLogRow(
    item: BatchFileItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (statusBg, statusBorder, statusIcon, statusTint) = when (item.status) {
        BatchItemStatus.PENDING -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.4f),
            Icons.Outlined.HourglassEmpty,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        BatchItemStatus.PROCESSING -> Quadruple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.primary,
            Icons.Outlined.Sync,
            MaterialTheme.colorScheme.primary
        )
        BatchItemStatus.SUCCESS -> Quadruple(
            EmeraldSuccess.copy(alpha = 0.1f),
            EmeraldSuccess.copy(alpha = 0.4f),
            Icons.Default.CheckCircle,
            EmeraldSuccess
        )
        BatchItemStatus.FAILED -> Quadruple(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
            Icons.Default.Error,
            MaterialTheme.colorScheme.error
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("batch_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = statusBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon / Spinner
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(statusTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.status == BatchItemStatus.PROCESSING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = statusTint
                    )
                } else {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )

                    if (item.status == BatchItemStatus.SUCCESS && item.signaturesCount > 1) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldSuccess
                        ) {
                            Text(
                                text = "${item.signaturesCount} TTD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = item.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (item.status) {
                        BatchItemStatus.SUCCESS -> EmeraldSuccess
                        BatchItemStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                // Quick Action Bar for completed signed file
                if (item.status == BatchItemStatus.SUCCESS && item.signedFilePath != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { PdfFileHelper.openPdf(context, item.signedFilePath) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = "Buka PDF",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                PdfFileHelper.downloadPdfToDevice(
                                    context,
                                    item.signedFilePath,
                                    item.fileName
                                )
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Unduh ke HP",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                PdfFileHelper.sharePdf(
                                    context,
                                    item.signedFilePath,
                                    item.fileName
                                )
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Kirim ke WhatsApp/Lainnya",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
