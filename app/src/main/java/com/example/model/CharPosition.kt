package com.example.model

data class CharPosition(
    val char: String,
    val x: Float,
    val y: Float, // from top
    val width: Float,
    val height: Float,
    val pageIndex: Int,
    val pageHeight: Float
)
