package com.chronie.homemoneylite.di

import android.content.Context
import android.net.wifi.WifiManager
import com.chronie.homemoneylite.data.local.dao.ExpenseDao
import com.chronie.homemoneylite.data.local.dao.SyncQueueDao
import com.chronie.homemoneylite.data.remote.api.ExpenseApi
import com.chronie.homemoneylite.data.sync.DeviceSyncManagerFactory
import com.chronie.homemoneylite.data.sync.SyncManagerImpl
import com.chronie.homemoneylite.data.sync.SyncScheduler
import com.chronie.homemoneylite.core.network.NetworkMonitor
import com.chronie.homemoneylite.domain.sync.SyncManager
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 同步模块
 * 提供数据同步相关的依赖（仅支持局域网同步）
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    
    @Provides
    @Singleton
    fun provideSyncManager(
        @ApplicationContext context: Context,
        expenseDao: ExpenseDao,
        syncQueueDao: SyncQueueDao,
        expenseApi: ExpenseApi,
        deviceSyncManagerFactory: DeviceSyncManagerFactory
    ): SyncManager {
        return SyncManagerImpl(
            context = context,
            expenseDao = expenseDao,
            syncQueueDao = syncQueueDao,
            expenseApi = expenseApi,
            deviceSyncManagerFactory = deviceSyncManagerFactory
        )
    }
    
    @Provides
    @Singleton
    fun provideDeviceSyncManagerFactory(
        @ApplicationContext context: Context,
        expenseDao: ExpenseDao,
        gson: Gson,
        wifiManager: WifiManager
    ): DeviceSyncManagerFactory {
        return DeviceSyncManagerFactory(
            context = context,
            expenseDao = expenseDao,
            gson = gson,
            wifiManager = wifiManager
        )
    }
    
    @Provides
    @Singleton
    fun provideWifiManager(
        @ApplicationContext context: Context
    ): WifiManager {
        return context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    
    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor {
        return NetworkMonitor(context)
    }
    
    @Provides
    @Singleton
    fun provideSyncScheduler(
        @ApplicationContext context: Context,
        networkMonitor: NetworkMonitor,
        syncManager: SyncManager
    ): SyncScheduler {
        return SyncScheduler(
            context = context,
            networkMonitor = networkMonitor,
            syncManager = syncManager
        )
    }
}
