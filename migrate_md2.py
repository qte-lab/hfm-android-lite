#!/usr/bin/env python3
# 机械替换：MD3 -> MD2 文本级安全替换（不动 import 行，不动 material3-only 类名）
import os, re, glob

SRC = r"D:\chronie-app\hfm-android-lite\app\src\main\java"

# color role: MD3 -> MD2。MD2 原生字段保持不变。
ROLE_MAP = {
    "surfaceVariant": "surface",
    "onSurfaceVariant": "onSurface",
    "surfaceContainerLow": "surface",
    "surfaceContainer": "surface",
    "surfaceContainerHigh": "surface",
    "surfaceContainerHighest": "surface",
    "surfaceContainerLowest": "surface",
    "surfaceBright": "surface",
    "surfaceDim": "surface",
    "outline": "onSurface",
    "outlineVariant": "onSurface",
    "primaryContainer": "primary",
    "onPrimaryContainer": "onPrimary",
    "secondaryContainer": "secondary",
    "onSecondaryContainer": "onSecondary",
    "tertiaryContainer": "secondary",
    "onTertiaryContainer": "onSecondary",
    "tertiary": "secondary",
    "errorContainer": "error",
    "onErrorContainer": "onError",
    "inversePrimary": "primary",
    "inverseSurface": "surface",
    "inverseOnSurface": "onSurface",
    "scrim": "surface",
}

# typography: MD3 -> MD2
TYPO_MAP = {
    "displayLarge": "h1",
    "displayMedium": "h2",
    "displaySmall": "h3",
    "headlineLarge": "h4",
    "headlineMedium": "h5",
    "headlineSmall": "h5",
    "titleLarge": "h6",
    "titleMedium": "subtitle1",
    "titleSmall": "subtitle2",
    "bodyLarge": "body1",
    "bodyMedium": "body2",
    "bodySmall": "caption",
    "labelLarge": "button",
    "labelMedium": "caption",
    "labelSmall": "caption",
}

files = glob.glob(os.path.join(SRC, "**", "*.kt"), recursive=True)
total_changes = 0
for f in files:
    with open(f, "r", encoding="utf-8") as fh:
        text = fh.read()
    orig = text

    # 1) MaterialTheme.colorScheme -> MaterialTheme.colors
    text = text.replace("MaterialTheme.colorScheme", "MaterialTheme.colors")

    # 2) role mapping: MaterialTheme.colors.<role> -> MaterialTheme.colors.<mapped>
    for role, mapped in ROLE_MAP.items():
        text = text.replace(f"MaterialTheme.colors.{role}", f"MaterialTheme.colors.{mapped}")

    # 3) typography mapping
    for t, m in TYPO_MAP.items():
        text = text.replace(f"MaterialTheme.typography.{t}", f"MaterialTheme.typography.{m}")

    if text != orig:
        with open(f, "w", encoding="utf-8") as fh:
            fh.write(text)
        n = sum(1 for a, b in zip(orig, text) if a != b)
        total_changes += 1
        print(f"CHANGED: {os.path.relpath(f, SRC)}")

print(f"\nTotal files changed: {total_changes}")
