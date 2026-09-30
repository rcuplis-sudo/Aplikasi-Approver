package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DetectionMethod
import com.example.pdf.PdfSampleGenerator
import com.example.ui.MainViewModel
import com.example.ui.PdfFileHelper
import com.example.ui.SignUiState
import com.example.ui.components.PdfPagePreviewCard
import com.example.ui.components.QrCenterOverlaySettingsCard
import com.example.ui.theme.AmberFallback
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyLight
import com.example.ui.theme.PrimaryBlueLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignPdfScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val selectedDoc by viewModel.selectedDocument.collectAsState()
    val docTitle by viewModel.documentTitle.collectAsState()
    val signerName by viewModel.signerName.collectAsState()
    val targetPlaceholder by viewModel.targetPlaceholder.collectAsState()
    val qrOverlayConfig by viewModel.qrOverlayConfig.collectAsState()

    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val isPreviewLoading by viewModel.isPreviewLoading.collectAsState()
    val previewCurrentPage by viewModel.previewCurrentPage.collectAsState()
    val previewTotalPages by viewModel.previewTotalPages.collectAsState()
    val customPlacements by viewModel.customPlacements.collectAsState()
    val isManualPlacementMode by viewModel.isManualPlacementMode.collectAsState()

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "document.pdf"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: "document.pdf"
                }
            }
            viewModel.setCustomUri(uri, fileName)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Document Selection Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("document_selection_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1. Pilih Sumber File PDF",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Upload Button
                Button(
                    onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pick_pdf_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih PDF dari Perangkat")
                }

                // Quick Sample Section
                Text(
                    text = "Atau gunakan PDF sampel siap uji:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.loadSample(PdfSampleGenerator.SampleType.TEXT_PLACEHOLDER) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sample_text_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("1 Teks TTD", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { viewModel.loadSample(PdfSampleGenerator.SampleType.MULTI_PLACEHOLDER) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sample_multi_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("1 Teks 2+ Multi TTD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                // Currently Selected Document Banner
                if (selectedDoc != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedDoc!!.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (selectedDoc!!.isSample) "Sampel Siap Uji" else "File Perangkat",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: PDF Page Preview with Touch-and-Drag QR Placement
        if (selectedDoc != null) {
            PdfPagePreviewCard(
                previewBitmap = previewBitmap,
                isLoading = isPreviewLoading,
                currentPage = previewCurrentPage,
                totalPages = previewTotalPages,
                targetPlaceholder = targetPlaceholder,
                signerName = signerName,
                qrOverlayConfig = qrOverlayConfig,
                isManualPlacementMode = isManualPlacementMode,
                customPlacement = customPlacements[previewCurrentPage],
                onToggleManualMode = { viewModel.toggleManualPlacementMode() },
                onPositionChanged = { normX, normY ->
                    viewModel.setQrPlacement(previewCurrentPage, normX, normY)
                },
                onSizeChanged = { newSizeDp ->
                    viewModel.updateQrPlacementSize(previewCurrentPage, newSizeDp)
                },
                onResetPosition = { viewModel.clearCustomPlacement(previewCurrentPage) },
                onPreviousPage = { viewModel.changePreviewPage(previewCurrentPage - 1) },
                onNextPage = { viewModel.changePreviewPage(previewCurrentPage + 1) }
            )
        }

        // Section 3: Ringkasan Parameter & Tanda Tangan
        var isParamExpanded by remember(signerName.isBlank()) { mutableStateOf(signerName.isBlank()) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("metadata_config_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (signerName.isNotBlank()) "Penandatangan: $signerName" else "Penandatangan: (Belum diisi)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (signerName.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (signerName.isNotBlank()) "Target: $targetPlaceholder (Tersimpan)" else "Isi nama penandatangan di bawah",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    TextButton(
                        onClick = { isParamExpanded = !isParamExpanded },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(if (isParamExpanded) "Tutup" else "Ubah")
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            if (isParamExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Expandable Fields
                AnimatedVisibility(visible = isParamExpanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = docTitle,
                            onValueChange = { viewModel.updateDocumentTitle(it) },
                            label = { Text("Nama / Judul Dokumen (doc)") },
                            leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("doc_title_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = signerName,
                            onValueChange = { viewModel.updateSignerName(it) },
                            label = { Text("Nama Penandatangan (Signer)") },
                            placeholder = { Text("Ketik nama penandatangan...") },
                            supportingText = {
                                Text(
                                    if (signerName.isBlank()) "Nama wajib diisi dan akan tersimpan otomatis untuk selanjutnya"
                                    else "Tersimpan otomatis untuk penandatanganan berikutnya",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signer_name_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = targetPlaceholder,
                            onValueChange = { viewModel.updateTargetPlaceholder(it) },
                            label = { Text("Teks Placeholder Target") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("placeholder_target_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            supportingText = {
                                Text("Pencarian teks murni. Jika 1 teks muncul 2+ kali, seluruhnya otomatis ditandatangani.")
                            }
                        )
                    }
                }
            }
        }

        // Action Button: Sign & Embed QR
        Button(
            onClick = { viewModel.signDocument() },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("process_sign_button"),
            enabled = selectedDoc != null && uiState !is SignUiState.Loading,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlueLight
            )
        ) {
            if (uiState is SignUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text((uiState as SignUiState.Loading).message)
            } else {
                Icon(Icons.Default.QrCode2, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Proses & Tempel QR ke PDF", style = MaterialTheme.typography.titleMedium)
            }
        }

        // Section 3: Error Message
        if (uiState is SignUiState.Error) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("error_banner")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = (uiState as SignUiState.Error).errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Section 4: Success Result Card
        AnimatedVisibility(
            visible = uiState is SignUiState.Success,
            enter = fadeIn() + slideInVertically()
        ) {
            val successResult = (uiState as? SignUiState.Success)?.result
            if (successResult != null) {
                val match = successResult.match
                val payload = successResult.payload

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("success_result_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Detection Status Banner
                        val (bannerBg, bannerColor, badgeTitle) = when {
                            successResult.signaturesCount > 1 -> Triple(
                                EmeraldSuccess.copy(alpha = 0.18f),
                                EmeraldSuccess,
                                "Otomatis Menandatangani ${successResult.signaturesCount} Placeholder"
                            )
                            match.source == DetectionMethod.MANUAL_DRAG -> Triple(
                                PrimaryBlueLight.copy(alpha = 0.15f),
                                PrimaryBlueLight,
                                "Posisi QR Ditempatkan via Touch-and-Drag"
                            )
                            match.source == DetectionMethod.ACROFORM -> Triple(EmeraldSuccess.copy(alpha = 0.15f), EmeraldSuccess, "AcroForm Field Berhasil Ditemukan")
                            match.source == DetectionMethod.TEXT_SEARCH -> Triple(NavyLight.copy(alpha = 0.15f), NavyLight, "Placeholder Teks Berhasil Ditemukan")
                            else -> Triple(AmberFallback.copy(alpha = 0.15f), AmberFallback, "Mode Fallback Diaktifkan")
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = bannerBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (match.source == DetectionMethod.FALLBACK) Icons.Outlined.Warning else Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = bannerColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = badgeTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = bannerColor
                                    )
                                    Text(
                                        text = if (successResult.signaturesCount > 1) {
                                            "Terdeteksi ${successResult.signaturesCount} lokasi placeholder di dokumen. Seluruh ${successResult.signaturesCount} posisi langsung ditempeli kode QR tanda tangan digital secara serentak."
                                        } else {
                                            match.details
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // QR Code Preview & Basic Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Image(
                                bitmap = successResult.qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code Preview",
                                modifier = Modifier
                                    .size(110.dp)
                                    .background(Color.White, RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp))
                                    .padding(6.dp)
                            )

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = payload.doc,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Signer: ${payload.signer}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Posisi: Hal ${match.pageIndex + 1} (X:${match.x.toInt()}, Y:${match.y.toInt()})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Ukuran QR: ${match.width.toInt()}x${match.height.toInt()} pt",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // QR JSON Payload Preview
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
                                    text = "Isi Payload JSON (ZXing)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = {
                                        PdfFileHelper.copyToClipboard(context, "Payload JSON", payload.toPrettyJson())
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy JSON", modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = payload.toJson(),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            )
                        }

                        // Action Buttons: Open, Download, & Share Signed PDF
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    PdfFileHelper.openPdf(context, successResult.signedFile.absolutePath)
                                },
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("open_signed_pdf_button"),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Buka",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    PdfFileHelper.downloadPdfToDevice(
                                        context,
                                        successResult.signedFile.absolutePath,
                                        successResult.signedFile.name
                                    )
                                },
                                modifier = Modifier
                                    .weight(1.0f)
                                    .testTag("download_signed_pdf_button"),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Unduh",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    PdfFileHelper.sharePdf(
                                        context,
                                        successResult.signedFile.absolutePath,
                                        payload.doc
                                    )
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("share_signed_pdf_button"),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Bagikan / WA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
