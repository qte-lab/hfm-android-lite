package com.chronie.homemoneylite.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 唯一保留的主题主色：绿色 (0xFF66BB6A)
private val GreenPrimaryLight = Color(0xFF66BB6A)
private val GreenPrimaryVariantLight = Color(0xFF43A047)
private val GreenSecondaryLight = Color(0xFF81C784)
private val GreenSecondaryVariantLight = Color(0xFF519657)

private val GreenPrimaryDark = Color(0xFF81C784)
private val GreenPrimaryVariantDark = Color(0xFF66BB6A)
private val GreenSecondaryDark = Color(0xFF81C784)
private val GreenSecondaryVariantDark = Color(0xFF66BB6A)

private val LightColorPalette = lightColors(
    primary = GreenPrimaryLight,
    primaryVariant = GreenPrimaryVariantLight,
    secondary = GreenSecondaryLight,
    secondaryVariant = GreenSecondaryVariantLight,
    background = Color(0xFFF2F7F3),
    surface = Color(0xFFFFFFFF),
    error = Color(0xFFB00020),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF000000),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    onError = Color(0xFFFFFFFF)
)

private val DarkColorPalette = darkColors(
    primary = GreenPrimaryDark,
    primaryVariant = GreenPrimaryVariantDark,
    secondary = GreenSecondaryDark,
    secondaryVariant = GreenSecondaryVariantDark,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    error = Color(0xFFCF6679),
    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onError = Color(0xFF000000)
)

@Composable
fun HomeMoneyTheme(
    content: @Composable () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController?.isAppearanceLightStatusBars = !darkTheme
            window.navigationBarColor = if (darkTheme) AndroidColor.BLACK else AndroidColor.WHITE
            window.statusBarColor = if (darkTheme) AndroidColor.BLACK else AndroidColor.WHITE
            insetsController?.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes(),
        content = content
    )
}
