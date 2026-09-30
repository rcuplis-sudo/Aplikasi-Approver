package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppThemeMode
import com.example.model.ColorPaletteStyle
import com.example.ui.MainViewModel
import com.example.ui.components.QrCenterOverlaySettingsCard

enum class SettingsSubpage {
    MAIN,
    THEME_APPEARANCE,
    SIGNER_DEFAULTS,
    QR_CUSTOMIZATION,
    BACKUP_RESTORE,
    ALGORITHM_SPECS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var currentSubpage by remember { mutableStateOf(SettingsSubpage.MAIN) }

    when (currentSubpage) {
        SettingsSubpage.MAIN -> {
            SettingsMainDashboard(
                viewModel = viewModel,
                onNavigateTo = { currentSubpage = it },
                modifier = modifier
            )
        }
        SettingsSubpage.THEME_APPEARANCE -> {
            ThemeAppearanceSubpage(
                viewModel = viewModel,
                onBack = { currentSubpage = SettingsSubpage.MAIN },
                modifier = modifier
            )
        }
        SettingsSubpage.SIGNER_DEFAULTS -> {
            SignerDefaultsSubpage(
                viewModel = viewModel,
                onBack = { currentSubpage = SettingsSubpage.MAIN },
                modifier = modifier
            )
        }
        SettingsSubpage.QR_CUSTOMIZATION -> {
            QrCustomizationSubpage(
                viewModel = viewModel,
                onBack = { currentSubpage = SettingsSubpage.MAIN },
                modifier = modifier
            )
        }
        SettingsSubpage.BACKUP_RESTORE -> {
            BackupRestoreSubpage(
                viewModel = viewModel,
                onBack = { currentSubpage = SettingsSubpage.MAIN },
                modifier = modifier
            )
        }
        SettingsSubpage.ALGORITHM_SPECS -> {
            AlgorithmSpecsSubpage(
                onBack = { currentSubpage = SettingsSubpage.MAIN },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun SettingsMainDashboard(
    viewModel: MainViewModel,
    onNavigateTo: (SettingsSubpage) -> Unit,
    modifier: Modifier = Modifier
) {
    val signerName by viewModel.signerName.collectAsState()
    val targetPlaceholder by viewModel.targetPlaceholder.collectAsState()
    val qrOverlayConfig by viewModel.qrOverlayConfig.collectAsState()
    val historyDocuments by viewModel.historyDocuments.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val paletteStyle by viewModel.paletteStyle.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Pusat Pengaturan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Kelola tema terang, identitas penandatangan, gaya QR, & cadangan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section 1: Tema & Tampilan Visual
        Text(
            text = "TEMA & TAMPILAN",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        SettingsMenuCard(
            icon = Icons.Outlined.Palette,
            title = "Tema Aplikasi & Warna Aksen",
            subtitle = "${themeMode.title} • ${paletteStyle.title}",
            onClick = { onNavigateTo(SettingsSubpage.THEME_APPEARANCE) },
            testTag = "setting_item_theme_appearance"
        )

        // Section 2: Profil Penandatangan & Template
        Text(
            text = "PENANDATANGAN & DOKUMEN",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        val signerSubtitle = if (signerName.isNotBlank()) signerName else "Belum diatur (Kosong)"
        SettingsMenuCard(
            icon = Icons.Outlined.Person,
            title = "Identitas Penandatangan & Placeholder",
            subtitle = "$signerSubtitle • $targetPlaceholder",
            onClick = { onNavigateTo(SettingsSubpage.SIGNER_DEFAULTS) },
            testTag = "setting_item_signer_defaults"
        )

        // Section 3: Tampilan & Kustomisasi QR Code
        Text(
            text = "TAMPILAN QR CODE",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        val overlayLabel = when (qrOverlayConfig.type) {
            com.example.model.QrCenterOverlayType.NONE -> "Standar Polos (Tanpa Logo)"
            com.example.model.QrCenterOverlayType.INITIALS -> "Inisial Tengah: \"${qrOverlayConfig.initials}\""
            com.example.model.QrCenterOverlayType.CUSTOM_LOGO -> "Logo / Cap Gambar Khusus"
        }

        SettingsMenuCard(
            icon = Icons.Outlined.QrCode2,
            title = "Kustomisasi Logo & Inisial QR",
            subtitle = overlayLabel,
            onClick = { onNavigateTo(SettingsSubpage.QR_CUSTOMIZATION) },
            testTag = "setting_item_qr_customization"
        )

        // Section 4: Data & Penyimpanan
        Text(
            text = "DATA & PENYIMPANAN",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        SettingsMenuCard(
            icon = Icons.Outlined.Archive,
            title = "Cadangkan & Pulihkan (Backup & Restore)",
            subtitle = "${historyDocuments.size} arsip dokumen tersimpan di Room database",
            onClick = { onNavigateTo(SettingsSubpage.BACKUP_RESTORE) },
            testTag = "setting_item_backup_restore"
        )

        // Section 5: Panduan & Informasi Sistem
        Text(
            text = "INFORMASI SISTEM",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        SettingsMenuCard(
            icon = Icons.Outlined.Info,
            title = "Spesifikasi Algoritma & Alur Teknis",
            subtitle = "AcroForm, Text Position Stripper, SHA-256 & Touch-and-Drag",
            onClick = { onNavigateTo(SettingsSubpage.ALGORITHM_SPECS) },
            testTag = "setting_item_algorithm_specs"
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "PDF QR Signer v1.0 • Offline Local Database • Modern Bright M3",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

// Subpage: Theme & Appearance
@Composable
private fun ThemeAppearanceSubpage(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val paletteStyle by viewModel.paletteStyle.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SubpageHeader(
            title = "Tema & Warna Tampilan",
            onBack = onBack
        )

        // Mode Seleksi: Terang vs Gelap vs Sistem
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Mode Tampilan Layar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                AppThemeMode.entries.forEach { mode ->
                    val isSelected = themeMode == mode
                    Card(
                        onClick = { viewModel.updateThemeMode(mode) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.updateThemeMode(mode) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = mode.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Seleksi Palet Warna Aksen
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Palet Warna Utama",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pilih aksen warna cerah dan modern untuk seluruh tombol, lencana, dan kartu:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ColorPaletteStyle.entries.forEach { palette ->
                    val isSelected = paletteStyle == palette
                    Card(
                        onClick = { viewModel.updatePaletteStyle(palette) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color circle swatch preview
                            Surface(
                                shape = CircleShape,
                                color = Color(palette.primaryHex),
                                modifier = Modifier.size(36.dp),
                                shadowElevation = 2.dp
                            ) {
                                if (isSelected) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = palette.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = palette.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Terapkan & Kembali")
        }
    }
}

@Composable
private fun SettingsMenuCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Subpage 1: Signer Defaults & Placeholder
@Composable
private fun SignerDefaultsSubpage(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val signerName by viewModel.signerName.collectAsState()
    val targetPlaceholder by viewModel.targetPlaceholder.collectAsState()
    val docTitle by viewModel.documentTitle.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SubpageHeader(
            title = "Pengaturan Penandatangan",
            onBack = onBack
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Konfigurasi Bawaan (Default)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pengaturan ini akan menjadi nilai otomatis setiap kali Anda membuka atau memproses berkas baru.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = signerName,
                    onValueChange = { viewModel.updateSignerName(it) },
                    label = { Text("Nama Penandatangan (Signer)") },
                    placeholder = { Text("Masukkan nama Anda...") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    supportingText = {
                        Text(
                            if (signerName.isNotBlank()) "Tersimpan otomatis untuk setiap kali menandatangani dokumen"
                            else "Dikosongkan - ketik nama Anda untuk disimpan otomatis",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )

                OutlinedTextField(
                    value = targetPlaceholder,
                    onValueChange = { viewModel.updateTargetPlaceholder(it) },
                    label = { Text("Target Placeholder Text / AcroForm") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    supportingText = {
                        Text("Contoh: \${ttd_pengirim1}, ttd_pengirim1, dll.")
                    }
                )

                OutlinedTextField(
                    value = docTitle,
                    onValueChange = { viewModel.updateDocumentTitle(it) },
                    label = { Text("Judul Dokumen Default") },
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        }

        if (signerName.isNotBlank()) {
            OutlinedButton(
                onClick = { viewModel.updateSignerName("") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Kosongkan Nama Penandatangan")
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Simpan & Kembali")
        }
    }
}

// Subpage 2: QR Center Logo / Initials Customization
@Composable
private fun QrCustomizationSubpage(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val qrOverlayConfig by viewModel.qrOverlayConfig.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SubpageHeader(
            title = "Tampilan & Logo QR Code",
            onBack = onBack
        )

        QrCenterOverlaySettingsCard(
            overlayConfig = qrOverlayConfig,
            onTypeChange = { viewModel.updateOverlayType(it) },
            onInitialsChange = { viewModel.updateOverlayInitials(it) },
            onLogoSelected = { uri, bitmap -> viewModel.updateOverlayLogo(uri, bitmap) }
        )

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Terapkan & Kembali")
        }
    }
}

// Subpage 3: Backup & Restore
@Composable
private fun BackupRestoreSubpage(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cadangkan & Pulihkan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        BackupRestoreScreen(
            viewModel = viewModel,
            modifier = Modifier.weight(1f)
        )
    }
}

// Subpage 4: Algorithm Specs
@Composable
private fun AlgorithmSpecsSubpage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Spesifikasi & Algoritma",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        AlgorithmInfoScreen(
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SubpageHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
