package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.ui.MainViewModel
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyLight
import com.example.ui.theme.PrimaryBlueLight

@Composable
fun BackupRestoreScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val historyList by viewModel.historyDocuments.collectAsState()
    val isProcessing by viewModel.isBackupProcessing.collectAsState()
    val statusMessage by viewModel.backupStatusMessage.collectAsState()
    val lastBackup by viewModel.lastBackupResult.collectAsState()

    var showRestoreConfirmDialog by remember { mutableStateOf<Uri?>(null) }
    var replaceExistingOption by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // File picker launcher for ZIP restore
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            showRestoreConfirmDialog = uri
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Banner
        Text(
            text = "Backup & Pemulihan Data",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Status or Error Banner
        if (statusMessage != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (statusMessage!!.contains("Gagal", ignoreCase = true)) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    EmeraldSuccess.copy(alpha = 0.15f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("backup_status_banner")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (statusMessage!!.contains("Gagal", ignoreCase = true)) {
                            Icons.Default.ErrorOutline
                        } else {
                            Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = if (statusMessage!!.contains("Gagal", ignoreCase = true)) {
                            MaterialTheme.colorScheme.error
                        } else {
                            EmeraldSuccess
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearBackupStatus() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Section 1: Backup Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("backup_section_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1. Buat Berkas Backup (ZIP)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Mengekspor seluruh catatan Room database (metadata tanda tangan, posisi, SHA-256 hash, payload QR) dan file fisik PDF hasil ke dalam arsip ZIP mandiri.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dokumen siap di-backup:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${historyList.size} berkas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Button(
                    onClick = { viewModel.performBackup() },
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("create_backup_button")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sedang memproses...")
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buat Cadangan Sekarang")
                    }
                }

                // If backup was created, show share button
                if (lastBackup != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldSuccess.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "File Backup Tersedia:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                            Text(
                                text = lastBackup!!.backupFile.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Ukuran: ${lastBackup!!.fileSizeFormatted} • ${lastBackup!!.totalDocuments} entri dokumen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    val backupFile = lastBackup!!.backupFile
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        backupFile
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "Backup PDF QR Signer")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Kirim Berkas Backup ZIP"))
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("share_backup_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bagikan / Simpan File Backup ZIP")
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Restore Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("restore_section_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "2. Pulihkan Dokumen (Restore ZIP)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Pilih arsip backup ZIP yang telah dibuat sebelumnya untuk mengimpor kembali semua data riwayat dan file PDF ke aplikasi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = {
                        zipPickerLauncher.launch(
                            arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*")
                        )
                    },
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pick_restore_zip_button")
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih File Backup ZIP")
                }
            }
        }

        // Section 3: Reset / Clear Database Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clear_section_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pengaturan Database",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "Kosongkan seluruh tabel riwayat di Room Database lokal (berguna untuk pengujian atau reset data).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { showClearConfirmDialog = true },
                    enabled = historyList.isNotEmpty() && !isProcessing,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("clear_all_data_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kosongkan Semua Data Riwayat")
                }
            }
        }
    }

    // Confirmation Dialog for Restore
    if (showRestoreConfirmDialog != null) {
        val selectedUri = showRestoreConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = null },
            icon = { Icon(Icons.Default.RestorePage, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Konfirmasi Pemulihan Data") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Apakah Anda ingin memulihkan riwayat dan dokumen PDF dari file ZIP ini?")

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = replaceExistingOption,
                            onCheckedChange = { replaceExistingOption = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hapus data saat ini terlebih dahulu (Replace)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.performRestore(selectedUri, replaceExistingOption)
                        showRestoreConfirmDialog = null
                    },
                    modifier = Modifier.testTag("confirm_restore_button")
                ) {
                    Text("Pulihkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Confirmation Dialog for Clear Data
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Hapus Semua Riwayat?") },
            text = { Text("Tindakan ini akan menghapus semua riwayat penandatanganan dari database Room lokal. Dokumen yang tidak di-backup tidak dapat dikembalikan.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_button")
                ) {
                    Text("Hapus Semua")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
