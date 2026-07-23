package com.chronie.homemoneylite.ui.components

import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.chronie.homemoneylite.ui.theme.*

@Composable
fun ExpressiveLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
    backgroundColor: Color = MaterialTheme.colors.surfaceVariant,
    amplitude: Float = 0.5f
) {
    LinearProgressIndicator(
        progress = progress.coerceIn(0f, 1f),
        modifier = modifier,
        color = color,
        backgroundColor = backgroundColor,
    )
}

@Composable
fun ExpressiveLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
    backgroundColor: Color = MaterialTheme.colors.surfaceVariant,
    amplitude: Float = 0.5f
) {
    LinearProgressIndicator(
        progress = progress(),
        modifier = modifier,
        color = color,
        backgroundColor = backgroundColor,
    )
}