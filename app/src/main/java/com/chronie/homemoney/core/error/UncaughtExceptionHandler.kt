package com.chronie.homemoney.core.error

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking

/**
 * 自定义未捕获异常处理器
 * 捕获应用中所有未被捕获的异常，记录到日志文件并上报
 */
class UncaughtExceptionHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler,
    private val errorReporter: ErrorReporter
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "UncaughtExceptionHandler"
        
        fun init(context: Context, errorReporter: ErrorReporter) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            val uncaughtExceptionHandler = UncaughtExceptionHandler(
                defaultHandler,
                errorReporter
            )
            Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler)
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val errorInfo = createErrorInfo(thread, throwable)
            
            saveErrorToLocal(errorInfo)
            
            reportErrorToServer(errorInfo)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle uncaught exception", e)
        } finally {
            defaultHandler.uncaughtException(thread, throwable)
        }
    }

    private fun createErrorInfo(thread: Thread, throwable: Throwable): ErrorInfo {
        return ErrorInfo(
            errorType = "UNCAUGHT_EXCEPTION",
            message = throwable.message ?: "Unknown error",
            stackTrace = getStackTraceString(throwable),
            threadName = thread.name,
            isMainThread = ThreadUtils.isMainThread(),
            timestamp = System.currentTimeMillis(),
            deviceInfo = DeviceInfoUtils.getDeviceInfo()
        )
    }

    private fun getStackTraceString(throwable: Throwable): String {
        return Log.getStackTraceString(throwable)
    }

    private fun saveErrorToLocal(errorInfo: ErrorInfo) {
        try {
            runBlocking {
                errorReporter.saveErrorToLocal(errorInfo)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save error to local file", e)
        }
    }

    private fun reportErrorToServer(errorInfo: ErrorInfo) {
        try {
            runBlocking {
                errorReporter.reportErrorToServer(errorInfo)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report error to server", e)
        }
    }
}
