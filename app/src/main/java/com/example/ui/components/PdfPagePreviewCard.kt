package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.graphicsLayer
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
    onSizeChanged: (Float) -> Unit = {},
    onResetPosition: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentPage, previewBitmap) {
        zoomScale = 1.0f
        panOffsetX = 0f
        panOffsetY = 0f
    }

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

            // Auto-Fit Adaptive Info (in Auto Mode)
            if (!isManualPlacementMode) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Auto-Fit Adaptif Aktif",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Ukuran QR otomatis dihitung sesuai jarak ke kalimat di bawahnya agar tidak menutupi tulisan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Interactive Instructions & Size Adjustment (in Manual Mode)
            if (isManualPlacementMode) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Title & Current Size Badge
                        val currentSizeVal = customPlacement?.qrSizeDp ?: 75f
                        val sizeCategory = when {
                            currentSizeVal < 35f -> "Mikro (Sangat Kecil)"
                            currentSizeVal < 55f -> "Kecil"
                            currentSizeVal < 80f -> "Sedang"
                            currentSizeVal < 105f -> "Besar"
                            else -> "Ekstra Besar"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ukuran QR Code",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "$sizeCategory (${currentSizeVal.roundToInt()} dp)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Stepper & Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val next = (currentSizeVal - 5f).coerceAtLeast(25f)
                                    onSizeChanged(next)
                                },
                                modifier = Modifier.size(36.dp),
                                enabled = currentSizeVal > 25f
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "Perkecil ukuran QR",
                                    tint = if (currentSizeVal > 25f) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Slider(
                                value = currentSizeVal,
                                onValueChange = { onSizeChanged(it) },
                                valueRange = 25f..130f,
                                steps = 20,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("qr_size_slider")
                            )

                            IconButton(
                                onClick = {
                                    val next = (currentSizeVal + 5f).coerceAtMost(130f)
                                    onSizeChanged(next)
                                },
                                modifier = Modifier.size(36.dp),
                                enabled = currentSizeVal < 130f
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Perbesar ukuran QR",
                                    tint = if (currentSizeVal < 130f) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Preset buttons: Mikro (28dp), Kecil (45dp), Sedang (70dp), Besar (95dp), Ekstra (125dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                Triple("Mikro", 28f, "28dp"),
                                Triple("Kecil", 45f, "45dp"),
                                Triple("Sedang", 70f, "70dp"),
                                Triple("Besar", 95f, "95dp"),
                                Triple("Ekstra", 125f, "125dp")
                            ).forEach { (label, presetVal, subLabel) ->
                                val isSelected = kotlin.math.abs(currentSizeVal - presetVal) <= 5f
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSizeChanged(presetVal) },
                                    label = {
                                        Text(
                                            text = "$label\n$subLabel",
                                            fontSize = 9.sp,
                                            lineHeight = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Micro-Positioning D-Pad Controls (Nudge 1% for placing small QR precisely)
                        val normX = customPlacement?.normalizedX ?: 0.65f
                        val normY = customPlacement?.normalizedY ?: 0.75f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Presisi Geser (1%):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedIconButton(
                                    onClick = { onPositionChanged((normX - 0.015f).coerceAtLeast(0f), normY) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Geser Kiri", modifier = Modifier.size(18.dp))
                                }
                                OutlinedIconButton(
                                    onClick = { onPositionChanged(normX, (normY - 0.015f).coerceAtLeast(0f)) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Geser Atas", modifier = Modifier.size(18.dp))
                                }
                                OutlinedIconButton(
                                    onClick = { onPositionChanged(normX, (normY + 0.015f).coerceAtMost(1f)) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Geser Bawah", modifier = Modifier.size(18.dp))
                                }
                                OutlinedIconButton(
                                    onClick = { onPositionChanged((normX + 0.015f).coerceAtMost(1f), normY) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Geser Kanan", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Text(
                            text = "💡 Geser slider atau ketuk preset (misal: Mikro 28dp). Gunakan tombol Zoom di bawah untuk memperbesar dokumen dan menempelkan QR code kecil secara presisi.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mencari posisi placeholder: $targetPlaceholder",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Zoom Controls Bar for Enlarge / Zooming In to Place Small QR Precisely
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ZoomIn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Zoom Preview PDF",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Stepper (- % +) & Reset
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val newZoom = (zoomScale - 0.5f).coerceAtLeast(1.0f)
                                    zoomScale = newZoom
                                    if (newZoom == 1.0f) {
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                    }
                                },
                                enabled = zoomScale > 1.0f,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Perkecil Zoom", modifier = Modifier.size(18.dp))
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (zoomScale > 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    zoomScale = 1.0f
                                    panOffsetX = 0f
                                    panOffsetY = 0f
                                }
                            ) {
                                Text(
                                    text = "${(zoomScale * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (zoomScale > 1f) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val newZoom = (zoomScale + 0.5f).coerceAtMost(3.5f)
                                    zoomScale = newZoom
                                },
                                enabled = zoomScale < 3.5f,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Perbesar Zoom", modifier = Modifier.size(18.dp))
                            }

                            if (zoomScale > 1.0f || panOffsetX != 0f || panOffsetY != 0f) {
                                TextButton(
                                    onClick = {
                                        zoomScale = 1.0f
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Reset 1x", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Quick Zoom Level Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            1.0f to "1x Fit",
                            1.5f to "1.5x",
                            2.0f to "2x Detail",
                            3.0f to "3x Mikro"
                        ).forEach { (scale, label) ->
                            val isSelected = kotlin.math.abs(zoomScale - scale) < 0.1f
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    zoomScale = scale
                                    if (scale == 1.0f) {
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                    }
                                },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Preview Display Area with Touch-and-Drag Overlay
            var imageContainerSize by remember { mutableStateOf(IntSize.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 280.dp, max = 460.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE2E8F0))
                    .border(
                        width = if (isManualPlacementMode) 2.dp else 1.dp,
                        color = if (isManualPlacementMode) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .pointerInput(imageContainerSize) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newZoom = (zoomScale * zoom).coerceIn(1.0f, 3.5f)
                            zoomScale = newZoom
                            if (newZoom > 1.0f && imageContainerSize.width > 0 && imageContainerSize.height > 0) {
                                val maxPanX = (imageContainerSize.width * (newZoom - 1f)) / 2f
                                val maxPanY = (imageContainerSize.height * (newZoom - 1f)) / 2f
                                panOffsetX = (panOffsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                                panOffsetY = (panOffsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                            } else {
                                panOffsetX = 0f
                                panOffsetY = 0f
                            }
                        }
                    },
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
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffsetX
                                translationY = panOffsetY
                            }
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
                            val qrOverlaySizeDp = (customPlacement?.qrSizeDp ?: 75f).dp
                            val qrSizePx = with(density) { qrOverlaySizeDp.toPx() }

                            val normX = customPlacement?.normalizedX ?: 0.65f
                            val normY = customPlacement?.normalizedY ?: 0.75f

                            val maxOffsetX = (imageContainerSize.width - qrSizePx).coerceAtLeast(0f)
                            val maxOffsetY = (imageContainerSize.height - qrSizePx).coerceAtLeast(0f)

                            var currentPixelX by remember(normX, imageContainerSize.width) {
                                mutableStateOf((normX * maxOffsetX).coerceIn(0f, maxOffsetX))
                            }
                            var currentPixelY by remember(normY, imageContainerSize.height) {
                                mutableStateOf((normY * maxOffsetY).coerceIn(0f, maxOffsetY))
                            }

                            LaunchedEffect(normX, normY, imageContainerSize, qrOverlaySizeDp) {
                                currentPixelX = (normX * maxOffsetX).coerceIn(0f, maxOffsetX)
                                currentPixelY = (normY * maxOffsetY).coerceIn(0f, maxOffsetY)
                            }

                            val isInteractive = isManualPlacementMode
                            val dragModifier = if (isInteractive) {
                                Modifier.pointerInput(imageContainerSize, zoomScale) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            val finalNormX = if (maxOffsetX > 0) (currentPixelX / maxOffsetX).coerceIn(0f, 1f) else 0.5f
                                            val finalNormY = if (maxOffsetY > 0) (currentPixelY / maxOffsetY).coerceIn(0f, 1f) else 0.5f
                                            onPositionChanged(finalNormX, finalNormY)
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        val deltaX = dragAmount.x / zoomScale
                                        val deltaY = dragAmount.y / zoomScale
                                        currentPixelX = (currentPixelX + deltaX).coerceIn(0f, maxOffsetX)
                                        currentPixelY = (currentPixelY + deltaY).coerceIn(0f, maxOffsetY)
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
                                            .size(20.dp)
                                            .offset(x = 5.dp, y = (-5).dp)
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

                                    Surface(
                                        shape = CircleShape,
                                        color = EmeraldSuccess,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(20.dp)
                                            .offset(x = 5.dp, y = 5.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.AspectRatio,
                                                contentDescription = "Resize icon",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating Pan Navigator overlay when zoomed in
                    if (zoomScale > 1.0f) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val maxPan = (imageContainerSize.width * (zoomScale - 1f)) / 2f
                                        panOffsetX = (panOffsetX + 60f).coerceIn(-maxPan, maxPan)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Geser Kiri", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        val maxPan = (imageContainerSize.height * (zoomScale - 1f)) / 2f
                                        panOffsetY = (panOffsetY + 60f).coerceIn(-maxPan, maxPan)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Geser Atas", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Pusatkan Tampilan", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(
                                    onClick = {
                                        val maxPan = (imageContainerSize.height * (zoomScale - 1f)) / 2f
                                        panOffsetY = (panOffsetY - 60f).coerceIn(-maxPan, maxPan)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Geser Bawah", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        val maxPan = (imageContainerSize.width * (zoomScale - 1f)) / 2f
                                        panOffsetX = (panOffsetX - 60f).coerceIn(-maxPan, maxPan)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Geser Kanan", modifier = Modifier.size(18.dp))
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
