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
import com.example.model.DetectionMethod
import com.example.pdf.PdfSampleGenerator
import com.example.ui.MainViewModel
import com.example.ui.PdfFileHelper
import com.example.ui.SignUiState
import com.example.ui.components.PdfPagePreviewCard
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

    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val isPreviewLoading by viewModel.isPreviewLoading.collectAsState()
    val previewCurrentPage by viewModel.previewCurrentPage.collectAsState()
    val previewTotalPages by viewModel.previewTotalPages.collectAsState()

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
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Text("Teks \${ttd}", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { viewModel.loadSample(PdfSampleGenerator.SampleType.ACROFORM_FIELD) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sample_acroform_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Text("AcroForm", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { viewModel.loadSample(PdfSampleGenerator.SampleType.NO_PLACEHOLDER_FALLBACK) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sample_fallback_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Text("Fallback", style = MaterialTheme.typography.labelSmall)
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

        // Section 2: PDF Page Preview using PDFBox PDFRenderer
        if (selectedDoc != null) {
            PdfPagePreviewCard(
                previewBitmap = previewBitmap,
                isLoading = isPreviewLoading,
                currentPage = previewCurrentPage,
                totalPages = previewTotalPages,
                targetPlaceholder = targetPlaceholder,
                onPreviousPage = { viewModel.changePreviewPage(previewCurrentPage - 1) },
                onNextPage = { viewModel.changePreviewPage(previewCurrentPage + 1) }
            )
        }

        // Section 3: Metadata & Signature Configuration
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("metadata_config_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "3. Parameter Dokumen & Penandatangan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

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
                    label = { Text("Nama Penandatangan (signer)") },
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
                    label = { Text("Placeholder Target (Text / AcroForm)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("placeholder_target_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    supportingText = {
                        Text("Default: \${ttd_pengirim1} (AcroForm atau teks)")
                    }
                )
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
                        val (bannerBg, bannerColor, badgeTitle) = when (match.source) {
                            DetectionMethod.ACROFORM -> Triple(EmeraldSuccess.copy(alpha = 0.15f), EmeraldSuccess, "AcroForm Field Berhasil Ditemukan")
                            DetectionMethod.TEXT_SEARCH -> Triple(NavyLight.copy(alpha = 0.15f), NavyLight, "Placeholder Teks Berhasil Ditemukan")
                            DetectionMethod.FALLBACK -> Triple(AmberFallback.copy(alpha = 0.15f), AmberFallback, "Mode Fallback Diaktifkan")
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
                                        text = match.details,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Fallback Notice (if fallback was triggered)
                        if (match.source == DetectionMethod.FALLBACK) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AmberFallback.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AmberFallback.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Handling Fallback: Karena placeholder tidak ditemukan pada isi dokumen, QR Code secara aman ditempelkan pada sudut kanan bawah di halaman terakhir dokumen.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberFallback,
                                    modifier = Modifier.padding(10.dp)
                                )
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

                        // Action Buttons: Open & Share Signed PDF
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    PdfFileHelper.openPdf(context, successResult.signedFile.absolutePath)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_signed_pdf_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buka File PDF")
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
                                    .weight(1f)
                                    .testTag("share_signed_pdf_button"),
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
    }
}
