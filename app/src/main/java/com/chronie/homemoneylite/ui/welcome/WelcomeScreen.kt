package com.chronie.homemoneylite.ui.welcome

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chronie.homemoneylite.R

@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit
) {
    val context = LocalContext.current
    val adUrl = "https://m.tb.cn/h.RlwomIHlT9pVpOi"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(adUrl))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                onGetStartedClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ad_splash),
            contentDescription = "广告",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Text(
            text = "点击前往淘宝",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
