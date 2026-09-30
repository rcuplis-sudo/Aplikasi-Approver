package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.QrCenterOverlayType
import com.example.model.QrOverlayConfig
import com.example.qr.QrCodeGenerator
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PrimaryBlueLight

@Composable
fun QrCenterOverlaySettingsCard(
    overlayConfig: QrOverlayConfig,
    onTypeChange: (QrCenterOverlayType) -> Unit,
    onInitialsChange: (String) -> Unit,
    onLogoSelected: (Uri?, android.graphics.Bitmap?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        onLogoSelected(uri, bitmap)
                    }
                }
            } catch (e: Exception) {
                // Ignore failure
            }
        }
    }

    // Mini QR live preview badge
    val previewQrBitmap = remember(overlayConfig) {
        try {
            QrCodeGenerator.generateQrBitmap(
                content = "https://verified-signer.example.com/check",
                width = 160,
                height = 160,
                overlayConfig = overlayConfig
            )
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("qr_overlay_settings_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BrandingWatermark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Logo / Inisial di Tengah QR Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Mini preview of QR with center overlay
                if (previewQrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(46.dp)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .testTag("qr_live_badge_preview")
                    ) {
                        Image(
                            bitmap = previewQrBitmap.asImageBitmap(),
                            contentDescription = "Preview QR Badge",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp)
                        )
                    }
                }
            }

            Text(
                text = "Sematkan inisial nama atau gambar logo instansi di tengah QR code. Sistem otomatis menggunakan Error Correction Tinggi (Level H 30%) agar QR tetap terbaca akurat.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Segmented option: Initials, Custom Logo, None
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = overlayConfig.type == QrCenterOverlayType.INITIALS,
                    onClick = { onTypeChange(QrCenterOverlayType.INITIALS) },
                    label = { Text("Inisial Teks", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (overlayConfig.type == QrCenterOverlayType.INITIALS) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("overlay_initials_chip")
                )

                FilterChip(
                    selected = overlayConfig.type == QrCenterOverlayType.CUSTOM_LOGO,
                    onClick = { onTypeChange(QrCenterOverlayType.CUSTOM_LOGO) },
                    label = { Text("Logo Gambar", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (overlayConfig.type == QrCenterOverlayType.CUSTOM_LOGO) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("overlay_logo_chip")
                )

                FilterChip(
                    selected = overlayConfig.type == QrCenterOverlayType.NONE,
                    onClick = { onTypeChange(QrCenterOverlayType.NONE) },
                    label = { Text("Polos", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (overlayConfig.type == QrCenterOverlayType.NONE) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("overlay_none_chip")
                )
            }

            when (overlayConfig.type) {
                QrCenterOverlayType.INITIALS -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = overlayConfig.initials,
                            onValueChange = { onInitialsChange(it.take(3).uppercase()) },
                            label = { Text("Inisial (1-3 Huruf)") },
                            leadingIcon = { Icon(Icons.Outlined.Title, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("overlay_initials_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Visual circular badge preview
                        Surface(
                            shape = CircleShape,
                            color = Color(overlayConfig.backgroundColor),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color(overlayConfig.textColor)),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = overlayConfig.initials.ifBlank { "ID" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(overlayConfig.textColor)
                                )
                            }
                        }
                    }
                }
                QrCenterOverlayType.CUSTOM_LOGO -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_qr_logo_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (overlayConfig.logoBitmap != null) "Ganti Logo" else "Pilih File Logo (PNG/JPG)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (overlayConfig.logoBitmap != null) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlueLight),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Image(
                                    bitmap = overlayConfig.logoBitmap.asImageBitmap(),
                                    contentDescription = "Logo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }
                }
                QrCenterOverlayType.NONE -> {
                    Text(
                        text = "QR code akan digenerate standar tanpa badge di bagian tengah.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
