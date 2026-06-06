package com.chronie.homemoney.ui.components

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.chronie.homemoney.R

data class ColorOption(
    val value: Int,
    @StringRes val nameResId: Int
) {
    val color: Color get() = Color(value.toLong() and 0xFFFFFFFFL)
}

data class ColorGroup(
    @StringRes val nameResId: Int,
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
        nameResId = R.string.color_group_red,
        colors = listOf(
            ColorOption(0xFFEF9A9A.toInt(), R.string.color_red_light),
            ColorOption(0xFFE57373.toInt(), R.string.color_red_bright),
            ColorOption(0xFFEF5350.toInt(), R.string.color_red_pure),
            ColorOption(0xFFF44336.toInt(), R.string.color_red_standard),
            ColorOption(0xFFE53935.toInt(), R.string.color_red_deep),
            ColorOption(0xFFD32F2F.toInt(), R.string.color_red_dark)
        )
    ),
    ColorGroup(
        nameResId = R.string.color_group_orange,
        colors = listOf(
            ColorOption(0xFFFFCC80.toInt(), R.string.color_orange_light),
            ColorOption(0xFFFFB74D.toInt(), R.string.color_orange_bright),
            ColorOption(0xFFFFA726.toInt(), R.string.color_orange_pure),
            ColorOption(0xFFFF9800.toInt(), R.string.color_orange_standard),
            ColorOption(0xFFFB8C00.toInt(), R.string.color_orange_deep),
            ColorOption(0xFFF57C00.toInt(), R.string.color_orange_dark)
        )
    ),
    ColorGroup(
        nameResId = R.string.color_group_yellow,
        colors = listOf(
            ColorOption(0xFFFFF176.toInt(), R.string.color_yellow_light),
            ColorOption(0xFFFFEE58.toInt(), R.string.color_yellow_bright),
            ColorOption(0xFFFFEB3B.toInt(), R.string.color_yellow_pure),
            ColorOption(0xFFFFD54F.toInt(), R.string.color_yellow_amber),
            ColorOption(0xFFFFCA28.toInt(), R.string.color_yellow_gold),
            ColorOption(0xFFFFC107.toInt(), R.string.color_yellow_standard)
        )
    ),
    ColorGroup(
        nameResId = R.string.color_group_yellow_green,
        colors = listOf(
            ColorOption(0xFFE6EE9C.toInt(), R.string.color_yellow_green_light),
            ColorOption(0xFFDCE775.toInt(), R.string.color_yellow_green_bright),
            ColorOption(0xFFCDDC39.toInt(), R.string.color_yellow_green_pure),
            ColorOption(0xFF9CCC65.toInt(), R.string.color_yellow_green_standard),
            ColorOption(0xFF8BC34A.toInt(), R.string.color_yellow_green_deep)
        )
    ),
    ColorGroup(
        nameResId = R.string.color_group_green,
        colors = listOf(
            ColorOption(0xFFC8E6C9.toInt(), R.string.color_green_light),
            ColorOption(0xFFA5D6A7.toInt(), R.string.color_green_bright),
            ColorOption(0xFF81C784.toInt(), R.string.color_green_pure),
            ColorOption(0xFF66BB6A.toInt(), R.string.color_green_standard),
            ColorOption(0xFF4CAF50.toInt(), R.string.color_green_deep),
            ColorOption(0xFF43A047.toInt(), R.string.color_green_dark),
            ColorOption(0xFF388E3C.toInt(), R.string.color_green_forest)
        )
    ),
    ColorGroup(
        nameResId = R.string.color_group_cyan,
        colors = listOf(
            ColorOption(0xFFB2EBF2.toInt(), R.string.color_cyan_light),
            ColorOption(0xFF80DEEA.toInt(), R.string.color_cyan_bright),
            ColorOption(0xFF4DD0E1.toInt(), R.string.color_cyan_pure),
            ColorOption(0xFF26C6DA.toInt(), R.string.color_cyan_standard),
            ColorOption(0xFF00ACC1.toInt(), R.string.color_cyan_deep),
            ColorOption(0xFF0097A7.toInt(), R.string.color_cyan_dark)
        )
    ),
    ColorGroup(
        nameResId = R.string.color_group_blue,
        colors = listOf(
            ColorOption(0xFFB3E5FC.toInt(), R.string.color_blue_light),
            ColorOption(0xFF81D4FA.toInt(), R.string.color_blue_bright),
            ColorOption(0xFF4FC3F7.toInt(), R.string.color_blue_pure),
            ColorOption(0xFF29B6F6.toInt(), R.string.color_blue_sky),
            ColorOption(0xFF42A5F5.toInt(), R.string.color_blue_standard),
            ColorOption(0xFF2196F3.toInt(), R.string.color_blue_deep),
            ColorOption(0xFF1565C0.toInt(), R.string.color_blue_dark)
        )
    ),
    ColorGroup(
        nameResId = R.string.color_group_indigo,
        colors = listOf(
            ColorOption(0xFFC5CAE9.toInt(), R.string.color_indigo_light),
            ColorOption(0xFF9FA8DA.toInt(), R.string.color_indigo_bright),
            ColorOption(0xFF7986CB.toInt(), R.string.color_indigo_pure),
            ColorOption(0xFF5C6BC0.toInt(), R.string.color_indigo_standard),
            ColorOption(0xFF3F51B5.toInt(), R.string.color_indigo_deep),
            ColorOption(0xFF283593.toInt(), R.string.color_indigo_dark)
        )
    ),
    ColorGroup(
        nameResId = R.string.color_group_purple,
        colors = listOf(
            ColorOption(0xFFE1BEE7.toInt(), R.string.color_purple_light),
            ColorOption(0xFFCE93D8.toInt(), R.string.color_purple_bright),
            ColorOption(0xFFBA68C8.toInt(), R.string.color_purple_pure),
            ColorOption(0xFFAB47BC.toInt(), R.string.color_purple_standard),
            ColorOption(0xFF9C27B0.toInt(), R.string.color_purple_deep),
            ColorOption(0xFF8E24AA.toInt(), R.string.color_purple_dark)
        )
    ),
    ColorGroup(
        nameResId = R.string.color_group_pink,
        colors = listOf(
            ColorOption(0xFFF8BBD0.toInt(), R.string.color_pink_light),
            ColorOption(0xFFF48FB1.toInt(), R.string.color_pink_bright),
            ColorOption(0xFFF06292.toInt(), R.string.color_pink_pure),
            ColorOption(0xFFEC407A.toInt(), R.string.color_pink_standard),
            ColorOption(0xFFE91E63.toInt(), R.string.color_pink_deep)
        )
    ),
    ColorGroup(
        nameResId = R.string.color_group_brown,
        colors = listOf(
            ColorOption(0xFFD7CCC8.toInt(), R.string.color_brown_light),
            ColorOption(0xFFBCAAA4.toInt(), R.string.color_brown_bright),
            ColorOption(0xFFA1887F.toInt(), R.string.color_brown_pure),
            ColorOption(0xFF8D6E63.toInt(), R.string.color_brown_standard),
            ColorOption(0xFF795548.toInt(), R.string.color_brown_deep),
            ColorOption(0xFF6D4C41.toInt(), R.string.color_brown_dark)
        )
    ),
    ColorGroup(
        nameResId = R.string.color_group_gray,
        colors = listOf(
            ColorOption(0xFFFAFAFA.toInt(), R.string.color_gray_very_light),
            ColorOption(0xFFF5F5F5.toInt(), R.string.color_gray_light),
            ColorOption(0xFFEEEEEE.toInt(), R.string.color_gray_bright),
            ColorOption(0xFFE0E0E0.toInt(), R.string.color_gray_standard_light),
            ColorOption(0xFFBDBDBD.toInt(), R.string.color_gray_medium),
            ColorOption(0xFF9E9E9E.toInt(), R.string.color_gray_standard),
            ColorOption(0xFF757575.toInt(), R.string.color_gray_deep),
            ColorOption(0xFF616161.toInt(), R.string.color_gray_dark)
        )
    ),
    ColorGroup(
        nameResId = R.string.color_group_special,
        colors = listOf(
            ColorOption(0xFFFF8A80.toInt(), R.string.color_special_coral_light),
            ColorOption(0xFFFF5252.toInt(), R.string.color_special_coral),
            ColorOption(0xFFFF1744.toInt(), R.string.color_special_coral_bright),
            ColorOption(0xFFA7FFEB.toInt(), R.string.color_special_mint_light),
            ColorOption(0xFF64FFDA.toInt(), R.string.color_special_mint),
            ColorOption(0xFF1DE9B6.toInt(), R.string.color_special_mint_deep),
            ColorOption(0xFFD1C4E9.toInt(), R.string.color_special_lavender_light),
            ColorOption(0xFFB39DDB.toInt(), R.string.color_special_lavender),
            ColorOption(0xFF9575CD.toInt(), R.string.color_special_lavender_deep),
            ColorOption(0xFF4DB6AC.toInt(), R.string.color_special_turquoise),
            ColorOption(0xFF26A69A.toInt(), R.string.color_special_sapphire),
            ColorOption(0xFF00897B.toInt(), R.string.color_special_malachite),
            ColorOption(0xFF69F0AE.toInt(), R.string.color_special_neon_green),
            ColorOption(0xFF18FFFF.toInt(), R.string.color_special_neon_cyan),
            ColorOption(0xFF536DFE.toInt(), R.string.color_special_neon_blue),
            ColorOption(0xFFFF4081.toInt(), R.string.color_special_neon_pink),
            ColorOption(0xFFFF9100.toInt(), R.string.color_special_neon_orange)
        )
    )
)
