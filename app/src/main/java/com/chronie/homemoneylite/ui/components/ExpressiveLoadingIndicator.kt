package com.chronie.homemoneylite.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chronie.homemoneylite.ui.theme.*

@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    containerVisible: Boolean = true
) {
    val colors = MaterialTheme.colors

    if (containerVisible) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(colors.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(size * 0.6f),
                color = colors.onPrimaryContainer,
                strokeWidth = size * 0.08f
            )
        }
    } else {
        CircularProgressIndicator(
            modifier = modifier.size(size),
            color = colors.primary,
            strokeWidth = size * 0.08f
        )
    }
}
