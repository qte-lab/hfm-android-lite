package com.chronie.homemoney.core.error

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 错误报告器类
 * 负责收集应用中的错误信息，保存到本地日志文件，并上报到后端
 */
@Singleton
class ErrorReporter @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val errorReportApi = MockErrorReportApi()
    private val logFileManager = LogFileManager(context)
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val mainThreadId = ThreadUtils.getMainThreadId()

    companion object {
        private const val TAG = "ErrorReporter"
        private const val MAX_QUEUE_SIZE = 10
        private const val RETRY_COUNT = 3
    }

    private val errorQueue = ArrayDeque<ErrorInfo>()

    /**
     * 初始化错误收集器
     */
    fun initialize() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler(
            UncaughtExceptionHandler(defaultHandler ?: Thread.UncaughtExceptionHandler { _, _ -> }, this)
        )

        Log.d(TAG, "Error reporter initialized")
    }

    /**
     * 公开的错误上报方法，供UncaughtExceptionHandler调用
     */
    @WorkerThread
    suspend fun reportErrorToServer(errorInfo: ErrorInfo) {
        reportErrorToServerInternal(errorInfo)
    }

    /**
     * 记录自定义错误
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val errorInfo = ErrorInfo(
            errorType = "CUSTOM_ERROR",
            message = "[$tag] $message",
            stackTrace = throwable?.let { getStackTraceString(it) } ?: getCurrentStackTrace(),
            threadName = Thread.currentThread().name,
            isMainThread = ThreadUtils.isMainThread(),
            timestamp = System.currentTimeMillis(),
            deviceInfo = DeviceInfoUtils.getDeviceInfo()
        )

        addToQueue(errorInfo)
        saveErrorToLocalAsync(errorInfo)
        reportErrorToServerAsync(errorInfo)
    }

    /**
     * 记录网络错误
     */
    fun logNetworkError(endpoint: String, errorCode: Int, message: String, throwable: Throwable? = null) {
        val errorInfo = ErrorInfo(
            errorType = "NETWORK_ERROR",
            message = "Network error at $endpoint: $errorCode - $message",
            stackTrace = throwable?.let { getStackTraceString(it) } ?: getCurrentStackTrace(),
            threadName = Thread.currentThread().name,
            isMainThread = ThreadUtils.isMainThread(),
            timestamp = System.currentTimeMillis(),
            deviceInfo = DeviceInfoUtils.getDeviceInfo(),
            additionalInfo = mapOf(
                "endpoint" to endpoint,
                "errorCode" to errorCode.toString()
            )
        )

        addToQueue(errorInfo)
        saveErrorToLocalAsync(errorInfo)
        reportErrorToServerAsync(errorInfo)
    }

    /**
     * 保存错误到本地文件
     */
    @WorkerThread
    suspend fun saveErrorToLocal(errorInfo: ErrorInfo) {
        withContext(Dispatchers.IO) {
            try {
                logFileManager.saveErrorLog(errorInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save error to local file", e)
            }
        }
    }

    /**
     * 上报错误到服务器的内部实现方法
     */
    @WorkerThread
    private suspend fun reportErrorToServerInternal(errorInfo: ErrorInfo) {
        Log.d(TAG, "Preparing to report error: ${errorInfo.message}")

        var retryCount = 0
        var success = false

        while (retryCount < RETRY_COUNT && !success) {
            try {
                val appVersionInfo = DeviceInfoUtils.getAppVersion(context)
                val result = errorReportApi.reportError(
                    ErrorReportRequest(
                        errorType = errorInfo.errorType,
                        message = errorInfo.message,
                        stackTrace = errorInfo.stackTrace,
                        timestamp = errorInfo.timestamp,
                        deviceInfo = errorInfo.deviceInfo,
                        appVersion = appVersionInfo.versionName,
                        appBuild = appVersionInfo.versionCode,
                        environment = getEnvironment(),
                        additionalInfo = errorInfo.additionalInfo
                    )
                )
                
                success = result.isSuccessful
                if (success) {
                    Log.d(TAG, "Error reported successfully")
                } else {
                    Log.e(TAG, "Failed to report error, response code: ${result.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception when reporting error, retry $retryCount", e)
            } finally {
                retryCount++
                if (!success && retryCount < RETRY_COUNT) {
                    Thread.sleep(1000L * retryCount)
                }
            }
        }
    }

    /**
     * 获取堆栈跟踪字符串
     */
    private fun getStackTraceString(throwable: Throwable): String {
        return Log.getStackTraceString(throwable)
    }

    /**
     * 获取当前线程的堆栈跟踪
     */
    private fun getCurrentStackTrace(): String {
        return Thread.currentThread().stackTrace.joinToString("\n") { it.toString() }
    }

    /**
     * 获取环境信息
     */
    private fun getEnvironment(): String {
        return if (isDebugMode()) "development" else "production"
    }

    /**
     * 检查是否为调试模式
     */
    private fun isDebugMode(): Boolean {
        return try {
            val appInfo = context.applicationInfo
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 添加到错误队列
     */
    private fun addToQueue(errorInfo: ErrorInfo) {
        synchronized(errorQueue) {
            if (errorQueue.size >= MAX_QUEUE_SIZE) {
                errorQueue.removeFirst()
            }
            errorQueue.add(errorInfo)
        }
    }

    /**
     * 异步保存到本地
     */
    private fun saveErrorToLocalAsync(errorInfo: ErrorInfo) {
        executorService.execute {
            kotlinx.coroutines.runBlocking {
                saveErrorToLocal(errorInfo)
            }
        }
    }

    /**
     * 异步上报到服务器
     */
    private fun reportErrorToServerAsync(errorInfo: ErrorInfo) {
        if (!ThreadUtils.isMainThread()) {
            executorService.execute {
                kotlinx.coroutines.runBlocking {
                    reportErrorToServerInternal(errorInfo)
                }
            }
        } else {
            handler.post {
                executorService.execute {
                    kotlinx.coroutines.runBlocking {
                        reportErrorToServerInternal(errorInfo)
                    }
                }
            }
        }
    }

    /**
     * 获取错误队列中的错误数量
     */
    fun getErrorQueueSize(): Int {
        synchronized(errorQueue) {
            return errorQueue.size
        }
    }

    /**
     * 清空错误队列
     */
    fun clearErrorQueue() {
        synchronized(errorQueue) {
            errorQueue.clear()
        }
    }

    /**
     * 获取所有日志文件
     */
    fun getLogFiles() = logFileManager.getLogFiles()

    /**
     * 清除所有日志文件
     */
    fun clearLogFiles() = logFileManager.clearLogFiles()
}
