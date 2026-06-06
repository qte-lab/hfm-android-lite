package com.chronie.homemoney.domain.sync

import com.chronie.homemoney.domain.model.SyncResult
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 同步进度信息
 */
data class SyncProgressInfo(
    @SerializedName("progress")
    val progress: Float = 0f,
    @SerializedName("message")
    val message: String = "",
    @SerializedName("isActive")
    val isActive: Boolean = false,
    @SerializedName("deviceName")
    val deviceName: String = ""
)

/**
 * 同步请求信息
 */
data class SyncRequestInfo(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("deviceName")
    val deviceName: String,
    @SerializedName("address")
    val address: String,
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 同步请求回调接口
 */
interface SyncRequestCallback {
    /**
     * 当收到同步请求时调用
     * @return true 表示接受同步，false 表示拒绝
     */
    suspend fun onSyncRequest(requestInfo: SyncRequestInfo): Boolean
}

/**
 * 设备间同步管理器接口
 * 用于实现不同设备之间的数据同步
 */
interface DeviceSyncManager {

    /**
     * 同步进度状态流
     */
    val syncProgress: StateFlow<SyncProgressInfo>

    /**
     * 搜索附近可用的设备
     */
    fun searchDevices(): Flow<DeviceInfo>

    /**
     * 与指定设备建立连接
     */
    suspend fun connect(device: DeviceInfo): Boolean

    /**
     * 断开与当前设备的连接
     */
    suspend fun disconnect(): Boolean

    /**
     * 向连接的设备发送数据
     */
    suspend fun sendData(data: DeviceSyncData): Boolean

    /**
     * 从连接的设备接收数据
     */
    suspend fun receiveData(): DeviceSyncData?

    /**
     * 执行与设备的双向同步
     */
    suspend fun syncWithDevice(device: DeviceInfo): SyncResult

    /**
     * 更新同步进度（用于服务器端通知UI）
     */
    fun updateSyncProgress(progress: Float, message: String, isActive: Boolean = true)

    /**
     * 清除同步进度
     */
    fun clearSyncProgress()

    /**
     * 设置同步请求回调（用于服务器端接收同步请求时通知UI）
     */
    fun setSyncRequestCallback(callback: SyncRequestCallback?)

    /**
     * 响应同步请求（接受或拒绝）
     */
    fun respondToSyncRequest(accepted: Boolean)
}

/**
 * 设备间同步数据模型
 */
data class DeviceSyncData(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("deviceName")
    val deviceName: String,
    @SerializedName("syncTimestamp")
    val syncTimestamp: Long,
    @SerializedName("entities")
    val entities: List<SyncEntity>
)

/**
 * 同步实体
 */
data class SyncEntity(
    @SerializedName("entityType")
    val entityType: String,
    @SerializedName("entityId")
    val entityId: String,
    @SerializedName("operation")
    val operation: String, // "CREATE", "UPDATE", "DELETE"
    @SerializedName("data")
    val data: String, // JSON格式的数据
    @SerializedName("timestamp")
    val timestamp: Long
)

/**
 * 设备信息
 */
data class DeviceInfo(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("deviceName")
    val deviceName: String,
    @SerializedName("deviceType")
    val deviceType: String, // "ANDROID", "IOS", "WEB"
    @SerializedName("connectionType")
    val connectionType: String, // "LAN", "BLUETOOTH", "NFC"
    @SerializedName("address")
    val address: String, // 设备地址
    @SerializedName("signalStrength")
    val signalStrength: Int // 信号强度 (0-100)
)
