package com.chronie.homemoney.data.local.dao

import androidx.room.*
import com.chronie.homemoney.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * 同步队列数据访问对象
 */
@Dao
interface SyncQueueDao {
    
    @Query("SELECT * FROM sync_queue ORDER BY created_at ASC")
    fun getAllSyncQueue(): Flow<List<SyncQueueEntity>>
    
    @Query("SELECT * FROM sync_queue WHERE entity_type = :entityType ORDER BY created_at ASC")
    suspend fun getSyncQueueByType(entityType: String): List<SyncQueueEntity>
    
    @Query("SELECT * FROM sync_queue ORDER BY created_at ASC LIMIT :limit")
    suspend fun getNextSyncItems(limit: Int): List<SyncQueueEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(item: SyncQueueEntity)
    
    @Update
    suspend fun updateSyncItem(item: SyncQueueEntity)
    
    @Delete
    suspend fun deleteSyncItem(item: SyncQueueEntity)
    
    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteSyncItemById(id: Long)
    
    @Query("DELETE FROM sync_queue WHERE entity_id = :entityId AND entity_type = :entityType")
    suspend fun deleteSyncItemsByEntity(entityId: String, entityType: String)
    
    @Query("DELETE FROM sync_queue")
    suspend fun deleteAllSyncQueue()
    
    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getSyncQueueCount(): Int
}
