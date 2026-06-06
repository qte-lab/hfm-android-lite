package com.chronie.homemoney.ui.components

import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    containerVisible: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    
    if (containerVisible) {
        ContainedLoadingIndicator(
            modifier = modifier,
            containerColor = colorScheme.secondaryContainer,
            indicatorColor = colorScheme.onPrimaryContainer
        )
    } else {
        LoadingIndicator(
            modifier = modifier,
            color = colorScheme.primary
        )
    }
}
