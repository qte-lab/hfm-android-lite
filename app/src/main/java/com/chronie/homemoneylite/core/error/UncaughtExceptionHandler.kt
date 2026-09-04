package com.chronie.homemoneylite.core.error

import android.util.Log

/**
 * 自定义未捕获异常处理器
 * 捕获应用中所有未被捕获的异常，在进程被系统终止前同步写出崩溃报告到应用内部目录。
 */
class UncaughtExceptionHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler,
    private val errorReporter: ErrorReporter
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "UncaughtExceptionHandler"

        /**
         * 安装全局异常处理器。
         * 必须在 Application.onCreate() 中尽早调用；会保留系统原有默认处理器，
         * 在写完崩溃报告后再交给它，保证原生“应用已停止”行为与进程终止逻辑不受影响。
         */
        fun install(errorReporter: ErrorReporter) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
                ?: Thread.UncaughtExceptionHandler { _, _ -> }
            val handler = UncaughtExceptionHandler(defaultHandler, errorReporter)
            Thread.setDefaultUncaughtExceptionHandler(handler)
            Log.d(TAG, "Global uncaught exception handler installed")
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // 先尽量把崩溃报告同步写到应用内部目录；即便失败也要继续走系统默认处理器，
        // 不能因为记日志而吞掉崩溃。
        try {
            errorReporter.saveCrashReportSync(thread, throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash report", e)
        } finally {
            defaultHandler.uncaughtException(thread, throwable)
        }
    }
}
