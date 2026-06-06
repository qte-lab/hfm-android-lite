package com.chronie.homemoney.data.sync

import android.content.Context
import android.net.wifi.WifiManager
import com.chronie.homemoney.data.local.dao.ExpenseDao
import com.chronie.homemoney.domain.sync.DeviceSyncManager
import com.google.gson.Gson

/**
 * 设备同步管理器工厂类
 * 仅支持局域网同步
 */
class DeviceSyncManagerFactory(
    private val context: Context,
    private val expenseDao: ExpenseDao,
    private val gson: Gson,
    private val wifiManager: WifiManager
) {

    // 单例实例，确保服务器只启动一次
    private val lanDeviceSyncManager: LanDeviceSyncManager by lazy {
        LanDeviceSyncManager(context, expenseDao, gson, wifiManager).apply {
            // 启动同步服务器，监听其他设备的连接请求
            startSyncServer()
        }
    }

    /**
     * 创建设备同步管理器
     * 仅支持局域网(LAN)同步
     * 返回单例实例，确保服务器保持运行
     */
    fun createDeviceSyncManager(): DeviceSyncManager {
        return lanDeviceSyncManager
    }
}
