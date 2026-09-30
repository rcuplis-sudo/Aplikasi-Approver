package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CustomQrPlacement
import com.example.model.QrOverlayConfig
import com.example.qr.QrCodeGenerator
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PrimaryBlueLight
import kotlin.math.roundToInt

@Composable
fun PdfPagePreviewCard(
    previewBitmap: Bitmap?,
    isLoading: Boolean,
    currentPage: Int,
    totalPages: Int,
    targetPlaceholder: String,
    signerName: String,
    qrOverlayConfig: QrOverlayConfig,
    isManualPlacementMode: Boolean,
    customPlacement: CustomQrPlacement?,
    onToggleManualMode: () -> Unit,
    onPositionChanged: (Float, Float) -> Unit,
    onResetPosition: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val previewQrBitmap = remember(qrOverlayConfig, signerName) {
        try {
            QrCodeGenerator.generateQrBitmap(
                content = "PREVIEW_VERIFIED_SIGNATURE\nSigner: $signerName\nTimestamp: ${System.currentTimeMillis()}",
                width = 200,
                height = 200,
                overlayConfig = qrOverlayConfig
            )
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pdf_preview_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with title and pagination badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.FindInPage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Visual Preview & Posisi QR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isManualPlacementMode) "Mode Geser QR (Drag & Drop) Aktif" else "Mode Otomatis (Cari Placeholder)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isManualPlacementMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (totalPages > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Hal ${currentPage + 1}/$totalPages",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Mode Toggle Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !isManualPlacementMode,
                    onClick = { if (isManualPlacementMode) onToggleManualMode() },
                    label = { Text("Auto (Placeholder)") },
                    leadingIcon = {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("mode_auto_chip")
                )

                FilterChip(
                    selected = isManualPlacementMode,
                    onClick = { if (!isManualPlacementMode) onToggleManualMode() },
                    label = { Text("Atur Posisi (Geser QR)") },
                    leadingIcon = {
                        Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlueLight,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    modifier = Modifier.testTag("mode_drag_chip")
                )

                if (isManualPlacementMode && customPlacement != null) {
                    IconButton(
                        onClick = onResetPosition,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = "Reset Posisi",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Interactive Instructions Banner
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isManualPlacementMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isManualPlacementMode) Icons.Default.PanTool else Icons.Outlined.Description,
                        contentDescription = null,
                        tint = if (isManualPlacementMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isManualPlacementMode) {
                            "Sentuh & geser kotak QR ke letak tanda tangan yang diinginkan."
                        } else {
                            "Mencari posisi placeholder: $targetPlaceholder"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isManualPlacementMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isManualPlacementMode) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }

            // Preview Display Area with Touch-and-Drag Overlay
            var imageContainerSize by remember { mutableStateOf(IntSize.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp, max = 400.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE2E8F0))
                    .border(
                        width = if (isManualPlacementMode) 2.dp else 1.dp,
                        color = if (isManualPlacementMode) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Merender halaman PDF ke Bitmap...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (previewBitmap != null) {
                    // Document sheet look
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .shadow(8.dp, RoundedCornerShape(4.dp))
                            .background(Color.White)
                            .onGloballyPositioned { coordinates ->
                                imageContainerSize = coordinates.size
                            }
                    ) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Preview Halaman Dokumen PDF",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pdf_preview_image")
                        )

                        // Touch-and-drag overlay
                        val hasDimensions = imageContainerSize.width > 0 && imageContainerSize.height > 0
                        if (hasDimensions) {
                            val qrOverlaySizeDp = 75.dp
                            val qrSizePx = with(density) { qrOverlaySizeDp.toPx() }

                            val normX = customPlacement?.normalizedX ?: 0.65f
                            val normY = customPlacement?.normalizedY ?: 0.75f

                            val maxOffsetX = (imageContainerSize.width - qrSizePx).coerceAtLeast(0f)
                            val maxOffsetY = (imageContainerSize.height - qrSizePx).coerceAtLeast(0f)

                            var currentPixelX by remember(normX, imageContainerSize.width) {
                                mutableStateOf(normX * maxOffsetX)
                            }
                            var currentPixelY by remember(normY, imageContainerSize.height) {
                                mutableStateOf(normY * maxOffsetY)
                            }

                            LaunchedEffect(normX, normY, imageContainerSize) {
                                currentPixelX = normX * maxOffsetX
                                currentPixelY = normY * maxOffsetY
                            }

                            val isInteractive = isManualPlacementMode
                            val dragModifier = if (isInteractive) {
                                Modifier.pointerInput(imageContainerSize) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            val finalNormX = if (maxOffsetX > 0) (currentPixelX / maxOffsetX).coerceIn(0f, 1f) else 0.5f
                                            val finalNormY = if (maxOffsetY > 0) (currentPixelY / maxOffsetY).coerceIn(0f, 1f) else 0.5f
                                            onPositionChanged(finalNormX, finalNormY)
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        currentPixelX = (currentPixelX + dragAmount.x).coerceIn(0f, maxOffsetX)
                                        currentPixelY = (currentPixelY + dragAmount.y).coerceIn(0f, maxOffsetY)
                                    }
                                }
                            } else {
                                Modifier
                            }

                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            currentPixelX.roundToInt(),
                                            currentPixelY.roundToInt()
                                        )
                                    }
                                    .size(qrOverlaySizeDp)
                                    .testTag("draggable_qr_overlay")
                                    .then(dragModifier)
                                    .shadow(elevation = if (isInteractive) 8.dp else 4.dp, shape = RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isInteractive) 2.dp else 1.5.dp,
                                        color = if (isInteractive) PrimaryBlueLight else EmeraldSuccess,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (previewQrBitmap != null) {
                                        Image(
                                            bitmap = previewQrBitmap.asImageBitmap(),
                                            contentDescription = "QR Code Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.QrCode2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                if (isInteractive) {
                                    Surface(
                                        shape = CircleShape,
                                        color = PrimaryBlueLight,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(18.dp)
                                            .offset(x = 4.dp, y = (-4).dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.OpenWith,
                                                contentDescription = "Drag icon",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Pilih dokumen PDF untuk melihat preview halaman & letak QR",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Pagination Controls (if more than 1 page)
            if (totalPages > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onPreviousPage,
                        enabled = currentPage > 0 && !isLoading,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("preview_prev_page_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sebelumnya")
                    }

                    Text(
                        text = "${currentPage + 1} / $totalPages",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    OutlinedButton(
                        onClick = onNextPage,
                        enabled = currentPage < totalPages - 1 && !isLoading,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("preview_next_page_button")
                    ) {
                        Text("Berikutnya")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
