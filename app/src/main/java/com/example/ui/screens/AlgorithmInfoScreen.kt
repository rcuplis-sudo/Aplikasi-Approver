package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AlgorithmInfoScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Alur & Spesifikasi Teknis",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        InfoCard(
            stepNumber = "1",
            title = "Menerima Dokumen & Hashing SHA-256",
            description = "Aplikasi menerima PDF melalui Storage Access Framework (SAF) atau file generator. Sebelum dimodifikasi, SHA-256 dokumen asli dihitung sebagai fingerprint integritas dokumen."
        )

        InfoCard(
            stepNumber = "2",
            title = "Pencarian Placeholder \${ttd_pengirim1}",
            description = "Sistem menerapkan deteksi 2 tahap:\n" +
                    "• Tahap A (AcroForm): Memeriksa field formulir interaktif di PDF Catalog (nama field 'ttd_pengirim1' atau '\${ttd_pengirim1}').\n" +
                    "• Tahap B (Text Search): Menggunakan custom PDFTextStripper untuk melacak TextPosition setiap glif karakter dan menghitung bounding box (X, Y, Width, Height) dengan konversi koordinat bottom-up PDF."
        )

        InfoCard(
            stepNumber = "3",
            title = "Handling Fallback Jika Placeholder Tidak Ada",
            description = "Jika kedua tahap tidak menemukan placeholder, sistem tidak gagal (crash/abort), melainkan beralih ke mode Fallback:\n" +
                    "• QR otomatis ditempatkan pada halaman terakhir.\n" +
                    "• Koordinat aman diatur pada sudut kanan bawah (X: mediaBox.width - 150, Y: 50).\n" +
                    "• Status fallback dicatat di database Room dan ditampilkan di UI dengan indikator oranye/amber."
        )

        InfoCard(
            stepNumber = "4",
            title = "Pembuatan QR Code ZXing & Payload JSON",
            description = "QR Code di-generate dengan ZXing (BarcodeFormat.QR_CODE) dengan payload JSON standar:\n" +
                    "{\n  \"doc\": \"Judul Dokumen\",\n  \"signer\": \"Nama Penandatangan\",\n  \"ts\": 1727481234567,\n  \"id\": \"uuid-v4-string\",\n  \"hash\": \"sha256-hash-string\"\n}"
        )

        InfoCard(
            stepNumber = "5",
            title = "Penempelan Gambar QR (PDFBox-Android)",
            description = "Dengan LosslessFactory dan PDPageContentStream(AppendMode.APPEND), QR disematkan pada koordinat yang tepat tanpa merusak teks asli dokumen."
        )

        InfoCard(
            stepNumber = "6",
            title = "Penyimpanan Riwayat Metadata di Room",
            description = "Semua riwayat penandatanganan (ID unik, judul, penandatangan, timestamp, SHA-256, path file, posisi X/Y, metode deteksi, status fallback) disimpan persisten ke SQLite via Room DAO & Entity."
        )
    }
}

@Composable
private fun InfoCard(
    stepNumber: String,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stepNumber,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
