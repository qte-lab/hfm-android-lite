package com.chronie.homemoney.domain.model

/**
 * 同步结果
 */
data class SyncResult(
    val success: Boolean,
    val uploadResult: UploadResult,
    val downloadResult: DownloadResult,
    val conflicts: List<SyncConflict> = emptyList(),
    val error: String? = null
)

/**
 * 上传结果
 */
data class UploadResult(
    val totalItems: Int,
    val successCount: Int,
    val failedCount: Int,
    val failedItems: List<FailedSyncItem> = emptyList()
)

/**
 * 下载结果
 */
data class DownloadResult(
    val totalItems: Int,
    val newItems: Int,
    val updatedItems: Int,
    val conflicts: List<SyncConflict> = emptyList()
)

/**
 * 同步冲突
 */
data class SyncConflict(
    val entityType: String,
    val entityId: String,
    val conflictType: ConflictType,
    val localTimestamp: Long,
    val serverTimestamp: Long,
    val resolution: ConflictResolution
)

/**
 * 冲突类型
 */
enum class ConflictType {
    UPDATE_CONFLICT,  // 本地和服务器都有更新
    DELETE_CONFLICT   // 一方删除，另一方更新
}

/**
 * 冲突解决方案
 */
enum class ConflictResolution {
    USE_LOCAL,        // 使用本地版本
    USE_SERVER,       // 使用服务器版本
    MERGE             // 合并（如果可能）
}

/**
 * 失败的同步项
 */
data class FailedSyncItem(
    val entityType: String,
    val entityId: String,
    val operation: String,
    val error: String
)

/**
 * 同步状态
 */
enum class SyncStatus {
    IDLE,             // 空闲
    SYNCING,          // 同步中
    SUCCESS,          // 成功
    FAILED,           // 失败
    CONFLICT          // 有冲突
}
