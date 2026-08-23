package com.chronie.homemoneylite

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.chronie.homemoneylite.service.AppSunsetNotice
import com.chronie.homemoneylite.service.HealthCheckService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 传统 View 体系宿主：单 Activity + Navigation Component。
 * 页面结构见 res/navigation/nav_graph.xml。
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var syncScheduler: com.chronie.homemoneylite.data.sync.SyncScheduler

    @Inject
    lateinit var healthCheckService: HealthCheckService

    @Inject
    lateinit var appSunsetNotice: AppSunsetNotice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化同步调度器
        syncScheduler.initialize()

        // 应用启动时触发云同步尝试（允许失败）
        lifecycleScope.launch {
            try {
                syncScheduler.triggerImmediateSync()
            } catch (e: Exception) {
                // 同步失败不影响应用启动
                android.util.Log.w("MainActivity", "Failed to trigger sync on app start", e)
            }
        }

        // 从启动图主题切换到正常主题
        setTheme(R.style.AppTheme_NoActionBar)

        // 启动健康检查服务
        healthCheckService.start()

        setContentView(R.layout.activity_main)

        // 每次进入展示停运通知
        appSunsetNotice.showNotice(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        healthCheckService.stop()
    }
}
