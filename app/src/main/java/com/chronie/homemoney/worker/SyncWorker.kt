package com.chronie.homemoney.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chronie.homemoney.domain.sync.SyncManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 后台同步 Worker
 * 使用 WorkManager 定期执行数据同步
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(appContext, workerParams) {
    
    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME = "sync_work"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting background sync")
        
        return try {
            // 执行完整同步
            val syncResult = syncManager.performFullSync()
            
            if (syncResult.isSuccess) {
                val result = syncResult.getOrNull()
                if (result?.success == true) {
                    Log.d(TAG, "Background sync completed successfully")
                    Result.success()
                } else {
                    Log.w(TAG, "Background sync completed with errors: ${result?.error}")
                    Result.retry()
                }
            } else {
                Log.e(TAG, "Background sync failed", syncResult.exceptionOrNull())
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Background sync failed with exception", e)
            Result.retry()
        }
    }
}
