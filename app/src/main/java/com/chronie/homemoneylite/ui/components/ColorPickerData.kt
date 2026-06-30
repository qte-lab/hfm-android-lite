package com.chronie.homemoneylite.ui.components

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.chronie.homemoneylite.R

data class ColorOption(
    val value: Int,
    @param:StringRes val nameResId: Int
) {
    val color: Color get() = Color(value.toLong() and 0xFFFFFFFFL)
}

data class ColorGroup(
    @param:StringRes val nameResId: Int,
    val colors: List<ColorOption>
)

fun getColorGroups(): List<ColorGroup> = listOf(
    ColorGroup(
        nameResId = R.string.color_group_default,
        colors = listOf(
            ColorOption(0xFF6750A4.toInt(), R.string.color_group_default)
        )
    ),
    ColorGroup(
        nameResId = R.string.color_group_pink,
        colors = listOf(
            ColorOption(0xFFEC407A.toInt(), R.string.color_pink_standard)
        )
    ),
    ColorGroup(
        nameResId = R.string.color_group_blue,
        colors = listOf(
            ColorOption(0xFF29B6F6.toInt(), R.string.color_blue_sky)
        )
    ),
    ColorGroup(
        nameResId = R.string.color_group_cyan,
        colors = listOf(
            ColorOption(0xFF4DD0E1.toInt(), R.string.color_cyan_pure)
        )
    ),
    ColorGroup(
        nameResId = R.string.color_group_green,
        colors = listOf(
            ColorOption(0xFF66BB6A.toInt(), R.string.color_green_standard)
        )
    )
)
