package com.chronie.homemoney.core.error

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 错误收集系统测试类
 * 提供方法用于测试错误收集系统的各种功能
 * 仅在开发环境使用，生产环境应当禁用
 */
@Singleton
class ErrorReporterTest @Inject constructor(
    private val errorReporter: ErrorReporter
) {

    companion object {
        private const val TAG = "ErrorReporterTest"
    }

    /**
     * 测试记录普通错误
     * 模拟应用中的一个自定义错误
     */
    fun testLogError() {
        Log.d(TAG, "Testing log error functionality")
        try {
            errorReporter.logError(
                tag = "Test",
                message = "This is a test error message",
                throwable = Exception("Test exception for error logging")
            )
            Log.d(TAG, "Log error test completed")
        } catch (e: Exception) {
            Log.e(TAG, "Log error test failed", e)
        }
    }

    /**
     * 测试记录网络错误
     * 模拟一个网络请求失败的场景
     */
    fun testNetworkError() {
        Log.d(TAG, "Testing network error functionality")
        try {
            errorReporter.logNetworkError(
                endpoint = "/api/test",
                errorCode = 404,
                message = "Resource not found",
                throwable = Exception("Test network exception")
            )
            Log.d(TAG, "Network error test completed")
        } catch (e: Exception) {
            Log.e(TAG, "Network error test failed", e)
        }
    }

    /**
     * 测试未捕获异常处理
     * 注意：此方法会导致应用崩溃，仅用于测试环境
     * 警告：请谨慎使用，不要在生产环境调用
     */
    fun testUncaughtException() {
        Log.d(TAG, "Testing uncaught exception handling")
        Log.w(TAG, "WARNING: This will crash the app. Only call in test environment!")
        // 抛出一个未捕获的异常，应该被我们的UncaughtExceptionHandler捕获
        throw RuntimeException("Test uncaught exception")
    }

    /**
     * 测试不带异常的错误日志
     */
    fun testErrorWithoutException() {
        Log.d(TAG, "Testing error logging without exception")
        try {
            errorReporter.logError(
                tag = "Test",
                message = "This is a test error without exception"
            )
            Log.d(TAG, "Error without exception test completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error without exception test failed", e)
        }
    }

    /**
     * 测试在不同线程中记录错误
     */
    fun testErrorInDifferentThreads() {
        Log.d(TAG, "Testing error logging in different threads")
        
        // 在主线程记录一个错误
        errorReporter.logError(
            tag = "MainThread",
            message = "Error logged from main thread"
        )
        
        // 在工作线程记录一个错误
        Thread {
            errorReporter.logError(
                tag = "WorkerThread",
                message = "Error logged from worker thread"
            )
        }.start()
        
        Log.d(TAG, "Multi-thread error logging test completed")
    }
}