package com.chronie.homemoneylite

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.chronie.homemoneylite.core.common.AppLanguageManager
import com.chronie.homemoneylite.core.error.ErrorReporter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HomeMoneyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var errorReporter: ErrorReporter

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // 语言必须在上下文建立前注入，否则首屏资源（含 Activity 标题）会用系统语言解析
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguageManager.wrapContext(base))
    }

    override fun onCreate() {
        super.onCreate()

        // 初始化错误收集系统
        try {
            errorReporter.initialize()
            Log.d("HomeMoneyApplication", "Error reporting system initialized")
        } catch (e: Exception) {
            // 即使错误收集系统初始化失败，也要确保应用能正常运行
            Log.e("HomeMoneyApplication", "Failed to initialize error reporting system", e)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
