package com.example.model

enum class BatchItemStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED
}

data class BatchFileItem(
    val id: String,
    val fileName: String,
    val source: Any, // Uri or File
    val status: BatchItemStatus = BatchItemStatus.PENDING,
    val signaturesCount: Int = 0,
    val details: String = "Menunggu antrean...",
    val durationMs: Long = 0L,
    val errorMessage: String? = null,
    val signedFilePath: String? = null
)

data class BatchProgressState(
    val isRunning: Boolean = false,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val totalSignaturesPlaced: Int = 0,
    val items: List<BatchFileItem> = emptyList(),
    val currentFileName: String? = null,
    val isCompleted: Boolean = false
) {
    val progressFraction: Float
        get() = if (totalCount > 0) currentIndex.toFloat() / totalCount.toFloat() else 0f
}
