package com.chronie.homemoneylite.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    containerVisible: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme

    if (containerVisible) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(size * 0.6f),
                color = colorScheme.onPrimaryContainer,
                strokeWidth = size * 0.08f
            )
        }
    } else {
        CircularProgressIndicator(
            modifier = modifier.size(size),
            color = colorScheme.primary,
            strokeWidth = size * 0.08f
        )
    }
}
