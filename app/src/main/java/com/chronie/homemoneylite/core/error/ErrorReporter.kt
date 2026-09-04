package com.chronie.homemoneylite.core.error

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 错误报告器类
 * 负责收集应用中的错误信息并保存到本地日志文件
 */
@Singleton
class ErrorReporter @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val logFileManager = LogFileManager(context)
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    companion object {
        private const val TAG = "ErrorReporter"
        private const val MAX_QUEUE_SIZE = 10
    }

    private val errorQueue = ArrayDeque<ErrorInfo>()

    /**
     * 初始化错误收集器：安装全局未捕获异常处理器。
     * 必须在 Application.onCreate() 中尽早调用。
     */
    fun initialize() {
        UncaughtExceptionHandler.install(this)
        Log.d(TAG, "Error reporter initialized")
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
     * 同步写出崩溃报告（应用内部目录 crash_logs/crash-*.txt）。
     * 必须在崩溃线程上同步完成，不能依赖协程调度器——进程随时会被系统默认处理器终止。
     */
    fun saveCrashReportSync(thread: Thread, throwable: Throwable): File? {
        return try {
            logFileManager.saveCrashLog(thread, throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash report", e)
            null
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
