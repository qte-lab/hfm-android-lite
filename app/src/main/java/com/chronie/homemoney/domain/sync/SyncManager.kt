package com.chronie.homemoney.domain.sync

import com.chronie.homemoney.domain.model.SyncConflict
import com.chronie.homemoney.domain.model.SyncResult
import com.chronie.homemoney.domain.model.UploadResult
import com.chronie.homemoney.domain.model.DownloadResult
import kotlinx.coroutines.flow.Flow

/**
 * 数据同步管理器接口
 */
interface SyncManager {
    
    /**
     * 执行完整同步
     */
    suspend fun performFullSync(): Result<SyncResult>
    
    /**
     * 上传本地更改
     */
    suspend fun uploadLocalChanges(): Result<UploadResult>
    
    /**
     * 下载服务器更新
     */
    suspend fun downloadServerUpdates(): Result<DownloadResult>
    
    /**
     * 解决同步冲突
     */
    suspend fun resolveConflicts(conflicts: List<SyncConflict>): Result<Unit>
    
    /**
     * 获取最后同步时间
     */
    fun getLastSyncTime(): Long?
    
    /**
     * 设置最后同步时间
     */
    suspend fun setLastSyncTime(timestamp: Long)
    
    /**
     * 获取待同步项数量
     */
    suspend fun getPendingSyncCount(): Int
    
    /**
     * 观察同步状态
     */
    fun observeSyncStatus(): Flow<com.chronie.homemoney.domain.model.SyncStatus>
    
    /**
     * 获取设备同步管理器（仅支持局域网同步）
     */
    fun getDeviceSyncManager(): DeviceSyncManager
}
