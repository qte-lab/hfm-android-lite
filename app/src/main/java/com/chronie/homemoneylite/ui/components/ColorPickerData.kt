package com.chronie.homemoneylite.ui.components

import androidx.compose.ui.graphics.Color

data class ColorOption(
    val value: Int
) {
    val color: Color get() = Color(value.toLong() and 0xFFFFFFFFL)
}

fun getColorOptions(): List<ColorOption> = listOf(
    ColorOption(0xFF29B6F6.toInt()), // Default - Sky Blue
    ColorOption(0xFFEC407A.toInt()), // Standard Pink
    ColorOption(0xFF4DD0E1.toInt()), // Pure Cyan
    ColorOption(0xFF66BB6A.toInt())  // Standard Green
)
