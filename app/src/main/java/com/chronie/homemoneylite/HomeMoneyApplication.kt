package com.chronie.homemoneylite

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import coil.Coil
import coil.ImageLoader
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.chronie.homemoneylite.core.error.ErrorReporter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HomeMoneyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var errorReporter: ErrorReporter

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // 低端机图片加载优化：
        // - availableMemoryPercentage(0.10)：收紧内存缓存占比，低内存设备降低 OOM 风险
        // - bitmapPoolingEnabled(true)：复用 Bitmap 对象，显著减少 GC 抖动（低端机关键）
        // - bitmapConfig(RGB_565)：图片内存占用减半（缩略图无透明通道，肉眼几乎无差异）
        // - crossfade(false)：关闭淡入动画，减少每帧绘制开销
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .availableMemoryPercentage(0.10)
                .bitmapPoolingEnabled(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .crossfade(false)
                .build()
        )

        // 初始化错误收集系统
        try {
            errorReporter.initialize()
            Log.d("HomeMoneyApplication", "Error reporting system initialized")
        } catch (e: Exception) {
            // 即使错误收集系统初始化失败，也要确保应用能正常运行
            Log.e("HomeMoneyApplication", "Failed to initialize error reporting system", e)
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}
