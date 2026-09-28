package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signed_documents")
data class SignedDocumentEntity(
    @PrimaryKey
    val id: String,
    val documentTitle: String,
    val signerName: String,
    val timestamp: Long,
    val sha256Hash: String,
    val qrPayloadJson: String,
    val originalFileName: String,
    val signedFilePath: String,
    val pageIndex: Int,
    val posX: Float,
    val posY: Float,
    val detectionMethod: String,
    val fallbackUsed: Boolean,
    val detectionDetails: String
)
