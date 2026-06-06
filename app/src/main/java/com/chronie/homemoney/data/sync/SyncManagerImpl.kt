package com.chronie.homemoney.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.chronie.homemoney.data.local.dao.ExpenseDao
import com.chronie.homemoney.data.local.dao.SyncQueueDao
import com.chronie.homemoney.data.mapper.ExpenseMapper
import com.chronie.homemoney.data.remote.api.ExpenseApi
import com.chronie.homemoney.data.remote.dto.SyncRequestDto
import com.chronie.homemoney.domain.model.*
import com.chronie.homemoney.domain.sync.DeviceSyncManager
import com.chronie.homemoney.domain.sync.SyncManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val expenseDao: ExpenseDao,
    private val syncQueueDao: SyncQueueDao,
    private val expenseApi: ExpenseApi,
    private val deviceSyncManagerFactory: DeviceSyncManagerFactory
) : SyncManager {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "sync_prefs",
        Context.MODE_PRIVATE
    )
    
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    
    companion object {
        private const val TAG = "SyncManager"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
    }
    
    override suspend fun performFullSync(): Result<SyncResult> {
        return try {
            _syncStatus.value = SyncStatus.SYNCING
            Log.d(TAG, "Starting full sync")
            
            val lastSyncTime = getLastSyncTime()
            val pendingChanges = expenseDao.getPendingChanges()
            val localIds = expenseDao.getAllIds()
            
            Log.d(TAG, "Pending changes: ${pendingChanges.size}, lastSyncTime: $lastSyncTime, localIds: ${localIds.size}")
            
            val changesDto = pendingChanges.map { ExpenseMapper.toDto(ExpenseMapper.toDomain(it)) }
            
            val response = expenseApi.syncExpenses(
                SyncRequestDto(
                    lastSyncTime = lastSyncTime,
                    changes = changesDto.ifEmpty { null },
                    localIds = localIds.ifEmpty { null }
                )
            )
            
            if (!response.isSuccessful) {
                throw Exception("Sync API failed: ${response.code()}")
            }
            
            val syncResponse = response.body()!!
            Log.d(TAG, "Server changes: ${syncResponse.serverChanges.size}, Conflicts: ${syncResponse.conflicts.size}")
            
            for (serverChange in syncResponse.serverChanges) {
                val domainModel = ExpenseMapper.toDomain(serverChange)
                val entity = ExpenseMapper.toEntity(domainModel.copy(isSynced = true))
                expenseDao.upsertExpense(entity)
            }
            
            for (change in pendingChanges) {
                if (change.deletedAt != null) {
                    expenseDao.deleteExpenseById(change.id)
                } else {
                    expenseDao.updateExpense(change.copy(isSynced = true))
                }
            }
            
            setLastSyncTime(syncResponse.syncTime)
            
            val conflicts = syncResponse.conflicts.map { conflict ->
                SyncConflict(
                    entityType = "expense",
                    entityId = conflict.id,
                    conflictType = ConflictType.UPDATE_CONFLICT,
                    localTimestamp = conflict.clientUpdatedAt,
                    serverTimestamp = conflict.serverUpdatedAt,
                    resolution = ConflictResolution.USE_SERVER
                )
            }
            
            val syncResult = SyncResult(
                success = true,
                uploadResult = UploadResult(
                    totalItems = pendingChanges.size,
                    successCount = pendingChanges.size - syncResponse.conflicts.size,
                    failedCount = syncResponse.conflicts.size
                ),
                downloadResult = DownloadResult(
                    totalItems = syncResponse.serverChanges.size,
                    newItems = syncResponse.serverChanges.count { expenseDao.getExpenseById(it.id!!) == null },
                    updatedItems = syncResponse.serverChanges.count { expenseDao.getExpenseById(it.id!!) != null },
                    conflicts = conflicts
                ),
                conflicts = conflicts
            )
            
            _syncStatus.value = if (conflicts.isNotEmpty()) {
                SyncStatus.CONFLICT
            } else {
                SyncStatus.SUCCESS
            }
            
            Log.d(TAG, "Full sync completed successfully")
            Result.success(syncResult)
        } catch (e: Exception) {
            Log.e(TAG, "Full sync failed", e)
            _syncStatus.value = SyncStatus.FAILED
            Result.failure(e)
        }
    }
    
    override suspend fun uploadLocalChanges(): Result<UploadResult> {
        return performFullSync().map { it.uploadResult }
    }
    
    override suspend fun downloadServerUpdates(): Result<DownloadResult> {
        return performFullSync().map { it.downloadResult }
    }
    
    override suspend fun resolveConflicts(conflicts: List<SyncConflict>): Result<Unit> {
        return try {
            for (conflict in conflicts) {
                when (conflict.resolution) {
                    ConflictResolution.USE_LOCAL -> {
                        val entity = expenseDao.getExpenseById(conflict.entityId)
                        if (entity != null) {
                            val updated = entity.copy(
                                version = entity.version + 1,
                                updatedAt = System.currentTimeMillis(),
                                isSynced = false
                            )
                            expenseDao.updateExpense(updated)
                        }
                    }
                    ConflictResolution.USE_SERVER -> {
                        // Server version already applied during sync
                    }
                    ConflictResolution.MERGE -> {
                        // Custom merge logic would go here
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getLastSyncTime(): Long? {
        val time = prefs.getLong(KEY_LAST_SYNC_TIME, 0)
        return if (time == 0L) null else time
    }
    
    override suspend fun setLastSyncTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, timestamp).apply()
    }
    
    override suspend fun getPendingSyncCount(): Int {
        return expenseDao.getPendingChanges().size
    }
    
    override fun observeSyncStatus(): Flow<SyncStatus> = _syncStatus.asStateFlow()
    
    override fun getDeviceSyncManager(): DeviceSyncManager {
        return deviceSyncManagerFactory.createDeviceSyncManager()
    }
}
