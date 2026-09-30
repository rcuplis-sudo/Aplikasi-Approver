package com.example.model

enum class AppThemeMode(val title: String, val subtitle: String) {
    LIGHT("Terang & Bersih (Default)", "Latar belakang putih bersih, kontras tinggi, dan nyaman di mata"),
    DARK("Gelap Elegan (Dark Mode)", "Latar belakang obsidian pekat hemat baterai"),
    SYSTEM("Ikuti Sistem Android", "Mengikuti pengaturan tema perangkat Anda secara otomatis")
}

enum class ColorPaletteStyle(
    val title: String,
    val primaryHex: Long,
    val accentHex: Long,
    val description: String
) {
    OCEAN_BLUE("Royal Ocean Blue", 0xFF0284C7, 0xFF0EA5E9, "Biru laut cerah, segar, elegan & modern"),
    EMERALD_MINT("Fresh Emerald", 0xFF059669, 0xFF10B981, "Hijau zamrud bersih dan natural"),
    VIBRANT_INDIGO("Indigo Neo", 0xFF4F46E5, 0xFF7C3AED, "Indigo teknologi modern berkelas"),
    SUNSET_AMBER("Warm Gold & Amber", 0xFFD97706, 0xFFF59E0B, "Emas hangat bersahabat & berwibawa")
}
