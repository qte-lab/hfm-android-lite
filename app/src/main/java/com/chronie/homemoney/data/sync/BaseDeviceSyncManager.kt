package com.chronie.homemoney.data.sync

import android.util.Log
import com.chronie.homemoney.data.local.dao.ExpenseDao
import com.chronie.homemoney.data.local.entity.ExpenseEntity
import com.chronie.homemoney.data.mapper.ExpenseMapper.toEntity
import com.chronie.homemoney.domain.model.DownloadResult
import com.chronie.homemoney.domain.model.UploadResult
import com.chronie.homemoney.domain.sync.DeviceInfo
import com.chronie.homemoney.domain.sync.DeviceSyncData
import com.chronie.homemoney.domain.sync.DeviceSyncManager
import com.chronie.homemoney.domain.sync.SyncEntity
import com.chronie.homemoney.domain.sync.SyncManager
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * 设备间同步管理器抽象类
 * 提供通用的同步逻辑
 */
abstract class BaseDeviceSyncManager(
    protected val expenseDao: ExpenseDao,
    protected val gson: Gson
) : DeviceSyncManager {
    
    protected val TAG = this::class.java.simpleName
    protected var isConnected = false
    protected var currentDevice: DeviceInfo? = null
    
    override fun searchDevices(): Flow<DeviceInfo> = flow {
        // 默认实现，子类需重写
        Log.d(TAG, "Default searchDevices implementation")
    }
    
    override suspend fun connect(device: DeviceInfo): Boolean {
        Log.d(TAG, "Connecting to device: ${device.deviceName}")
        isConnected = true
        currentDevice = device
        return true
    }
    
    override suspend fun disconnect(): Boolean {
        Log.d(TAG, "Disconnecting from device")
        isConnected = false
        currentDevice = null
        return true
    }
    
    override suspend fun sendData(data: DeviceSyncData): Boolean {
        Log.d(TAG, "Sending data to device: ${data.deviceName}")
        return true
    }
    
    override suspend fun receiveData(): DeviceSyncData? {
        Log.d(TAG, "Receiving data from device")
        return null
    }
    
    override suspend fun syncWithDevice(device: DeviceInfo): com.chronie.homemoney.domain.model.SyncResult {
        Log.d(TAG, "Starting sync with device: ${device.deviceName}")
        
        return try {
            // 1. 建立连接
            if (!connect(device)) {
                return createFailedSyncResult("Failed to connect to device")
            }
            
            // 2. 获取本地数据
            val localData = prepareLocalData()
            
            // 3. 发送本地数据到设备
            if (!sendData(localData)) {
                disconnect()
                return createFailedSyncResult("Failed to send data to device")
            }
            
            // 4. 接收设备数据
            val deviceData = receiveData()
            if (deviceData == null) {
                disconnect()
                return createFailedSyncResult("Failed to receive data from device")
            }
            
            // 5. 处理设备数据
            val downloadResult = processDeviceData(deviceData)
            
            // 6. 断开连接
            disconnect()
            
            // 7. 返回同步结果
            com.chronie.homemoney.domain.model.SyncResult(
                success = true,
                uploadResult = UploadResult(
                    totalItems = localData.entities.size,
                    successCount = localData.entities.size,
                    failedCount = 0
                ),
                downloadResult = downloadResult,
                conflicts = downloadResult.conflicts
            )
        } catch (e: Exception) {
            Log.e(TAG, "Sync with device failed", e)
            disconnect()
            createFailedSyncResult(e.message ?: "Unknown error")
        }
    }
    
    /**
     * 准备本地数据用于同步
     */
    protected suspend fun prepareLocalData(): DeviceSyncData {
        val allExpenses = expenseDao.getAllExpenses().first()
        val entities = mutableListOf<SyncEntity>()
        
        for (expense in allExpenses) {
            val jsonData = gson.toJson(expense)
            entities.add(
                SyncEntity(
                    entityType = "expense",
                    entityId = expense.id,
                    operation = "CREATE",
                    data = jsonData,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        return DeviceSyncData(
            deviceId = "local_device",
            deviceName = "Local Android Device",
            syncTimestamp = System.currentTimeMillis(),
            entities = entities
        )
    }
    
    /**
     * 处理从设备接收的数据
     */
    protected suspend fun processDeviceData(deviceData: DeviceSyncData): DownloadResult {
        val conflicts = mutableListOf<com.chronie.homemoney.domain.model.SyncConflict>()
        var newItems = 0
        var updatedItems = 0
        
        Log.d(TAG, "Processing ${deviceData.entities.size} entities from device ${deviceData.deviceName}")
        
        val totalEntities = deviceData.entities.size
        var processedCount = 0
        
        for (entity in deviceData.entities) {
            if (entity.entityType == "expense") {
                try {
                    Log.d(TAG, "Processing expense entity: ${entity.entityId}, data type: ${entity.data?.javaClass?.simpleName}")
                    Log.d(TAG, "Entity data (first 200 chars): ${entity.data?.take(200)}")
                    val expenseEntity = gson.fromJson(entity.data, ExpenseEntity::class.java)
                    Log.d(TAG, "Parsed expense entity: ${expenseEntity.id}, type: ${expenseEntity.type}, amount: ${expenseEntity.amount}")
                    val localExpense = expenseDao.getExpenseById(entity.entityId)
                    
                    if (localExpense == null) {
                        // 新记录，直接插入
                        expenseDao.insertExpense(expenseEntity)
                        newItems++
                        Log.d(TAG, "Added new expense from device: ${expenseEntity.id}")
                    } else {
                        // 已存在记录，使用较新的版本
                        // 使用当前时间作为比较基准
                        val localTimestamp = System.currentTimeMillis()
                        if (entity.timestamp > localTimestamp) {
                            expenseDao.insertExpense(expenseEntity)
                            updatedItems++
                            Log.d(TAG, "Updated expense from device: ${expenseEntity.id}")
                        } else {
                            // 冲突：本地版本更新
                            conflicts.add(
                                com.chronie.homemoney.domain.model.SyncConflict(
                                    entityType = "expense",
                                    entityId = entity.entityId,
                                    conflictType = com.chronie.homemoney.domain.model.ConflictType.UPDATE_CONFLICT,
                                    localTimestamp = localTimestamp,
                                    serverTimestamp = entity.timestamp,
                                    resolution = com.chronie.homemoney.domain.model.ConflictResolution.USE_LOCAL
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process expense entity", e)
                    Log.e(TAG, "Entity data: ${entity.data}")
                }
            }
            
            processedCount++
            // 每100个实体更新一次进度
            if (processedCount % 100 == 0 || processedCount == totalEntities) {
                val progress = 0.3f + (processedCount.toFloat() / totalEntities * 0.5f)
                updateSyncProgress(progress, "正在处理数据... ($processedCount/$totalEntities)", true)
            }
        }
        
        return DownloadResult(
            totalItems = deviceData.entities.size,
            newItems = newItems,
            updatedItems = updatedItems,
            conflicts = conflicts
        )
    }
    
    /**
     * 创建失败的同步结果
     */
    private fun createFailedSyncResult(error: String): com.chronie.homemoney.domain.model.SyncResult {
        return com.chronie.homemoney.domain.model.SyncResult(
            success = false,
            uploadResult = UploadResult(0, 0, 0),
            downloadResult = DownloadResult(0, 0, 0),
            error = error
        )
    }
}