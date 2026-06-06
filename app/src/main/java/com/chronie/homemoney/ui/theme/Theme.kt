package com.chronie.homemoney.ui.theme

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.Stable
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.scheme.SchemeContent
import com.kyant.m3color.scheme.SchemeExpressive
import com.kyant.m3color.scheme.SchemeFidelity
import com.kyant.m3color.scheme.SchemeFruitSalad
import com.kyant.m3color.scheme.SchemeMonochrome
import com.kyant.m3color.scheme.SchemeNeutral
import com.kyant.m3color.scheme.SchemeRainbow
import com.kyant.m3color.scheme.SchemeTonalSpot
import com.kyant.m3color.scheme.SchemeVibrant

// 颜色方案样式枚举
enum class PaletteStyle {
    TonalSpot,
    Neutral,
    Vibrant,
    Expressive,
    Rainbow,
    FruitSalad,
    Monochrome,
    Fidelity,
    Content,
}

// 创建主题设置的LocalContext
val LocalThemeSettings = staticCompositionLocalOf<MutableState<ThemeSettings>> {
    error("No ThemeSettings provided")
}

// 主题设置数据类
class ThemeSettings(
    val useDynamicColor: Boolean,
    val primaryColor: Int,
    val paletteStyle: PaletteStyle
)

// 从SharedPreferences加载主题设置
fun loadThemeSettings(context: Context): ThemeSettings {
    val prefs: SharedPreferences = context.getSharedPreferences("theme_settings", Context.MODE_PRIVATE)
    val useDynamicColor = prefs.getBoolean("use_dynamic_color", true)
    val primaryColor = prefs.getInt("primary_color", 0xFF6750A4.toInt()) // 默认紫色
    val paletteStyleValue = prefs.getInt("palette_style", PaletteStyle.Expressive.ordinal)
    val paletteStyle = PaletteStyle.values().getOrElse(paletteStyleValue) { PaletteStyle.Expressive }
    return ThemeSettings(useDynamicColor, primaryColor, paletteStyle)
}

// 更新主题设置到SharedPreferences
fun updateThemeSettings(context: Context, settings: ThemeSettings) {
    val prefs: SharedPreferences = context.getSharedPreferences("theme_settings", Context.MODE_PRIVATE)
    prefs.edit().apply {
        putBoolean("use_dynamic_color", settings.useDynamicColor)
        putInt("primary_color", settings.primaryColor)
        putInt("palette_style", settings.paletteStyle.ordinal)
    }.apply()
}

// 使用m3color库生成颜色方案
@Stable
fun dynamicColorScheme(
    keyColor: Color,
    isDark: Boolean,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = 0.0
): ColorScheme {
    val hct = Hct.fromInt(keyColor.toArgb())
    val scheme = when (style) {
        PaletteStyle.TonalSpot -> SchemeTonalSpot(hct, isDark, contrastLevel)
        PaletteStyle.Neutral -> SchemeNeutral(hct, isDark, contrastLevel)
        PaletteStyle.Vibrant -> SchemeVibrant(hct, isDark, contrastLevel)
        PaletteStyle.Expressive -> SchemeExpressive(hct, isDark, contrastLevel)
        PaletteStyle.Rainbow -> SchemeRainbow(hct, isDark, contrastLevel)
        PaletteStyle.FruitSalad -> SchemeFruitSalad(hct, isDark, contrastLevel)
        PaletteStyle.Monochrome -> SchemeMonochrome(hct, isDark, contrastLevel)
        PaletteStyle.Fidelity -> SchemeFidelity(hct, isDark, contrastLevel)
        PaletteStyle.Content -> SchemeContent(hct, isDark, contrastLevel)
    }

    val primaryColors = derivePrimaryColors(keyColor, isDark, style)

    return ColorScheme(
        background = scheme.background.toColor(),
        error = scheme.error.toColor(),
        errorContainer = scheme.errorContainer.toColor(),
        inverseOnSurface = scheme.inverseOnSurface.toColor(),
        inversePrimary = primaryColors.inversePrimary,
        inverseSurface = scheme.inverseSurface.toColor(),
        onBackground = scheme.onBackground.toColor(),
        onError = scheme.onError.toColor(),
        onErrorContainer = scheme.onErrorContainer.toColor(),
        onPrimary = primaryColors.onPrimary,
        onPrimaryContainer = primaryColors.onPrimaryContainer,
        onSecondary = scheme.onSecondary.toColor(),
        onSecondaryContainer = scheme.onSecondaryContainer.toColor(),
        onSurface = scheme.onSurface.toColor(),
        onSurfaceVariant = scheme.onSurfaceVariant.toColor(),
        onTertiary = scheme.onTertiary.toColor(),
        onTertiaryContainer = scheme.onTertiaryContainer.toColor(),
        outline = scheme.outline.toColor(),
        outlineVariant = scheme.outlineVariant.toColor(),
        primary = primaryColors.primary,
        primaryContainer = primaryColors.primaryContainer,
        scrim = scheme.scrim.toColor(),
        secondary = scheme.secondary.toColor(),
        secondaryContainer = scheme.secondaryContainer.toColor(),
        surface = scheme.surface.toColor(),
        surfaceBright = scheme.surfaceBright.toColor(),
        surfaceContainer = scheme.surfaceContainer.toColor(),
        surfaceContainerLow = scheme.surfaceContainerLow.toColor(),
        surfaceContainerLowest = scheme.surfaceContainerLowest.toColor(),
        surfaceContainerHigh = scheme.surfaceContainerHigh.toColor(),
        surfaceContainerHighest = scheme.surfaceContainerHighest.toColor(),
        surfaceDim = scheme.surfaceDim.toColor(),
        surfaceTint = primaryColors.primary,
        surfaceVariant = scheme.surfaceVariant.toColor(),
        tertiary = scheme.tertiary.toColor(),
        tertiaryContainer = scheme.tertiaryContainer.toColor(),
        primaryFixed = primaryColors.primaryFixed,
        primaryFixedDim = primaryColors.primaryFixedDim,
        onPrimaryFixed = primaryColors.onPrimaryFixed,
        onPrimaryFixedVariant = primaryColors.onPrimaryFixedVariant,
        secondaryFixed = scheme.secondaryContainer.toColor(),
        secondaryFixedDim = scheme.secondaryContainer.toColor(),
        onSecondaryFixed = scheme.onSecondaryContainer.toColor(),
        onSecondaryFixedVariant = scheme.onSecondary.toColor(),
        tertiaryFixed = scheme.tertiaryContainer.toColor(),
        tertiaryFixedDim = scheme.tertiaryContainer.toColor(),
        onTertiaryFixed = scheme.onTertiaryContainer.toColor(),
        onTertiaryFixedVariant = scheme.onTertiary.toColor(),
    )
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Int.toColor(): Color = Color(this)

// 手动派生 primary 颜色的数据类
data class PrimaryColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val primaryFixed: Color,
    val primaryFixedDim: Color,
    val onPrimaryFixed: Color,
    val onPrimaryFixedVariant: Color,
    val inversePrimary: Color
)

// 手动派生 primary 颜色的函数
@Stable
fun derivePrimaryColors(
    keyColor: Color,
    isDark: Boolean,
    style: PaletteStyle = PaletteStyle.TonalSpot
): PrimaryColors {
    val hct = Hct.fromInt(keyColor.toArgb())
    val hue = hct.hue
    val chroma = hct.chroma
    val tone = hct.tone

    // 根据不同的调色板风格调整色度
    val adjustedChroma = when (style) {
        PaletteStyle.TonalSpot -> chroma
        PaletteStyle.Neutral -> chroma * 0.5
        PaletteStyle.Vibrant -> chroma * 1.5
        PaletteStyle.Expressive -> chroma * 1.2
        PaletteStyle.Rainbow -> chroma * 1.3
        PaletteStyle.FruitSalad -> chroma * 1.4
        PaletteStyle.Monochrome -> chroma * 0.3
        PaletteStyle.Fidelity -> chroma * 1.1
        PaletteStyle.Content -> chroma * 0.8
    }.coerceAtMost(150.0).coerceAtLeast(0.0)

    // 根据亮色/暗色模式设置不同的色调值
    if (isDark) {
        // 暗色模式的色调设置
        val primaryTone = when {
            tone >= 60 -> 80.0
            tone >= 40 -> 70.0
            tone >= 20 -> 60.0
            else -> 50.0
        }
        val onPrimaryTone = 20.0
        val primaryContainerTone = when {
            tone >= 60 -> 30.0
            tone >= 40 -> 30.0
            tone >= 20 -> 30.0
            else -> 30.0
        }
        val onPrimaryContainerTone = 90.0
        val primaryFixedTone = 90.0
        val primaryFixedDimTone = 80.0
        val onPrimaryFixedTone = 10.0
        val onPrimaryFixedVariantTone = 30.0
        val inversePrimaryTone = 40.0

        return PrimaryColors(
            primary = Hct.from(hue, adjustedChroma, primaryTone).toInt().toColor(),
            onPrimary = Hct.from(hue, adjustedChroma, onPrimaryTone).toInt().toColor(),
            primaryContainer = Hct.from(hue, adjustedChroma, primaryContainerTone).toInt().toColor(),
            onPrimaryContainer = Hct.from(hue, adjustedChroma, onPrimaryContainerTone).toInt().toColor(),
            primaryFixed = Hct.from(hue, adjustedChroma, primaryFixedTone).toInt().toColor(),
            primaryFixedDim = Hct.from(hue, adjustedChroma, primaryFixedDimTone).toInt().toColor(),
            onPrimaryFixed = Hct.from(hue, adjustedChroma, onPrimaryFixedTone).toInt().toColor(),
            onPrimaryFixedVariant = Hct.from(hue, adjustedChroma, onPrimaryFixedVariantTone).toInt().toColor(),
            inversePrimary = Hct.from(hue, adjustedChroma, inversePrimaryTone).toInt().toColor()
        )
    } else {
        // 亮色模式的色调设置
        val primaryTone = when {
            tone >= 60 -> 40.0
            tone >= 40 -> 40.0
            tone >= 20 -> 40.0
            else -> 40.0
        }
        val onPrimaryTone = 100.0
        val primaryContainerTone = when {
            tone >= 60 -> 90.0
            tone >= 40 -> 90.0
            tone >= 20 -> 90.0
            else -> 90.0
        }
        val onPrimaryContainerTone = 10.0
        val primaryFixedTone = 90.0
        val primaryFixedDimTone = 80.0
        val onPrimaryFixedTone = 10.0
        val onPrimaryFixedVariantTone = 30.0
        val inversePrimaryTone = 80.0

        return PrimaryColors(
            primary = Hct.from(hue, adjustedChroma, primaryTone).toInt().toColor(),
            onPrimary = Hct.from(hue, adjustedChroma, onPrimaryTone).toInt().toColor(),
            primaryContainer = Hct.from(hue, adjustedChroma, primaryContainerTone).toInt().toColor(),
            onPrimaryContainer = Hct.from(hue, adjustedChroma, onPrimaryContainerTone).toInt().toColor(),
            primaryFixed = Hct.from(hue, adjustedChroma, primaryFixedTone).toInt().toColor(),
            primaryFixedDim = Hct.from(hue, adjustedChroma, primaryFixedDimTone).toInt().toColor(),
            onPrimaryFixed = Hct.from(hue, adjustedChroma, onPrimaryFixedTone).toInt().toColor(),
            onPrimaryFixedVariant = Hct.from(hue, adjustedChroma, onPrimaryFixedVariantTone).toInt().toColor(),
            inversePrimary = Hct.from(hue, adjustedChroma, inversePrimaryTone).toInt().toColor()
        )
    }
}

// 根据主题和颜色设置创建颜色方案
fun createColorScheme(
    context: Context,
    darkTheme: Boolean,
    dynamicColor: Boolean,
    primaryColor: Int,
    paletteStyle: PaletteStyle
): ColorScheme {
    val userPrimaryColor = Color(primaryColor)
    
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // 使用系统生成的完整动态颜色方案
        return if (darkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    } else {
        // 使用m3color库生成颜色方案
        return dynamicColorScheme(
            keyColor = userPrimaryColor,
            isDark = darkTheme,
            style = paletteStyle
        )
    }
}



@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeMoneyTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // 实时获取系统主题模式，确保能响应系统主题变化
    val darkTheme = isSystemInDarkTheme()
    
    // 创建可观察的主题设置
    val themeSettings = remember {
        mutableStateOf(loadThemeSettings(context))
    }
    
    // 监听SharedPreferences变化，确保主题设置能够实时更新
    DisposableEffect(Unit) {
        val prefs = context.getSharedPreferences("theme_settings", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            themeSettings.value = loadThemeSettings(context)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        
        // 清理监听器
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    // 当主题设置或系统主题模式变化时，自动重新计算颜色方案
    val colorScheme = remember(themeSettings.value, darkTheme) {
        createColorScheme(
            context = context,
            darkTheme = darkTheme,
            dynamicColor = themeSettings.value.useDynamicColor,
            primaryColor = themeSettings.value.primaryColor,
            paletteStyle = themeSettings.value.paletteStyle
        )
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            // 设置导航栏颜色为基本白/黑色
            window.navigationBarColor = if (darkTheme) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            // 设置状态栏颜色为基本白/黑色，在高版本安卓系统上，状态栏颜色会被系统忽略（由系统自动管理）
            window.statusBarColor = if (darkTheme) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            // 确保导航栏图标颜色正确
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    // 将主题设置提供给所有子组件
    CompositionLocalProvider(
        LocalThemeSettings provides themeSettings
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            content = content
        )
    }
}
