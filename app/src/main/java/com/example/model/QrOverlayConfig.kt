package com.example.model

import android.graphics.Bitmap
import android.net.Uri

enum class QrCenterOverlayType {
    NONE,
    INITIALS,
    CUSTOM_LOGO
}

data class QrOverlayConfig(
    val type: QrCenterOverlayType = QrCenterOverlayType.INITIALS,
    val initials: String = "HW",
    val logoUri: Uri? = null,
    val logoBitmap: Bitmap? = null,
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val textColor: Int = 0xFF0F2B5C.toInt() // Navy primary
)
