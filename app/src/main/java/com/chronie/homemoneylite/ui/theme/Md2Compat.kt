package com.chronie.homemoneylite.ui.theme

import androidx.compose.material.Colors
import androidx.compose.material.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * MD2 兼容层：为 androidx.compose.material 的 Colors / Typography 增补
 * Material 3 中常用的角色名与排版名，统一映射到 MD2 等价项。
 * 这样消费者代码只需把 MaterialTheme.colorScheme -> MaterialTheme.colors，
 * 无需逐条改写颜色角色与排版字段。
 */

// ---- Colors 扩展：MD3 角色名 -> MD2 等价色 ----
val Colors.surfaceVariant: Color get() = surface
val Colors.onSurfaceVariant: Color get() = onSurface
val Colors.surfaceContainer: Color get() = surface
val Colors.surfaceContainerLow: Color get() = surface
val Colors.surfaceContainerHigh: Color get() = surface
val Colors.surfaceContainerHighest: Color get() = surface
val Colors.surfaceContainerLowest: Color get() = surface
val Colors.surfaceBright: Color get() = surface
val Colors.surfaceDim: Color get() = surface
val Colors.outline: Color get() = onSurface.copy(alpha = 0.12f)
val Colors.outlineVariant: Color get() = onSurface.copy(alpha = 0.12f)
val Colors.primaryContainer: Color get() = primary
val Colors.onPrimaryContainer: Color get() = onPrimary
val Colors.secondaryContainer: Color get() = secondary
val Colors.onSecondaryContainer: Color get() = onSecondary
val Colors.tertiary: Color get() = secondary
val Colors.tertiaryContainer: Color get() = secondary
val Colors.onTertiaryContainer: Color get() = onSecondary
val Colors.errorContainer: Color get() = error
val Colors.onErrorContainer: Color get() = onError
val Colors.inversePrimary: Color get() = primary
val Colors.inverseSurface: Color get() = surface
val Colors.inverseOnSurface: Color get() = onSurface
val Colors.scrim: Color get() = surface

// ---- Typography 扩展：MD3 风格字段名 -> MD2 等价样式 ----
val Typography.displayLarge: TextStyle get() = h1
val Typography.displayMedium: TextStyle get() = h2
val Typography.displaySmall: TextStyle get() = h3
val Typography.headlineLarge: TextStyle get() = h4
val Typography.headlineMedium: TextStyle get() = h5
val Typography.headlineSmall: TextStyle get() = h5
val Typography.titleLarge: TextStyle get() = h6
val Typography.titleMedium: TextStyle get() = subtitle1
val Typography.titleSmall: TextStyle get() = subtitle2
val Typography.bodyLarge: TextStyle get() = body1
val Typography.bodyMedium: TextStyle get() = body2
val Typography.bodySmall: TextStyle get() = caption
val Typography.labelLarge: TextStyle get() = button
val Typography.labelMedium: TextStyle get() = caption
val Typography.labelSmall: TextStyle get() = caption
