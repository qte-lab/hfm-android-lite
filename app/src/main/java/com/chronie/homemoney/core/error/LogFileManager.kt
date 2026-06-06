package com.chronie.homemoney.core.error

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日志文件管理器
 * 负责管理错误日志文件的创建和写入
 */
class LogFileManager(private val context: Context) {

    companion object {
        private const val TAG = "LogFileManager"
        private const val CRASH_LOG_DIR = "crash_logs"
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    /**
     * 获取日志目录
     */
    fun getLogDir(): File {
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            File(context.getExternalFilesDir(null), CRASH_LOG_DIR)
        } else {
            File(context.filesDir, CRASH_LOG_DIR)
        }
    }

    /**
     * 保存崩溃日志到文件
     */
    fun saveCrashLog(thread: Thread, throwable: Throwable): File? {
        return try {
            val logDir = getLogDir()
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            val logFileName = "crash-${dateFormat.format(Date())}.txt"
            val logFile = File(logDir, logFileName)

            FileWriter(logFile).use { fileWriter ->
                fileWriter.write("Crash Time: ${timestampFormat.format(Date())}\n\n")

                fileWriter.write("Device Information:\n")
                val deviceInfo = DeviceInfoUtils.getDeviceInfo()
                fileWriter.write("- OS Version: Android ${deviceInfo["osVersion"]} (API ${deviceInfo["sdkVersion"]})\n")
                fileWriter.write("- Device: ${deviceInfo["manufacturer"]} ${deviceInfo["deviceModel"]}\n")
                val appVersion = DeviceInfoUtils.getAppVersion(context)
                fileWriter.write("- App Version: ${appVersion.versionName} (${appVersion.versionCode})\n\n")

                fileWriter.write("Thread Information:\n")
                fileWriter.write("- Thread Name: ${thread.name}\n")
                fileWriter.write("- Thread ID: ${ThreadUtils.getThreadId(thread)}\n\n")

                fileWriter.write("Crash Stack Trace:\n")
                val stringWriter = StringWriter()
                val printWriter = PrintWriter(stringWriter)
                throwable.printStackTrace(printWriter)
                fileWriter.write(stringWriter.toString())
            }

            Log.d(TAG, "Crash log saved to: ${logFile.absolutePath}")
            logFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log", e)
            null
        }
    }

    /**
     * 保存错误信息到日志文件
     */
    fun saveErrorLog(errorInfo: ErrorInfo): File? {
        return try {
            val logDir = getLogDir()
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            val logFileName = "error-${dateFormat.format(Date())}.txt"
            val logFile = File(logDir, logFileName)

            FileWriter(logFile).use { fileWriter ->
                fileWriter.write(convertErrorInfoToText(errorInfo))
            }

            Log.d(TAG, "Error log saved to: ${logFile.absolutePath}")
            logFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save error log", e)
            null
        }
    }

    /**
     * 将错误信息转换为文本格式
     */
    private fun convertErrorInfoToText(errorInfo: ErrorInfo): String {
        val timestampStr = timestampFormat.format(Date(errorInfo.timestamp))
        val deviceInfoStr = errorInfo.deviceInfo.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        val additionalInfoStr = errorInfo.additionalInfo?.entries?.joinToString(", ") { "${it.key}: ${it.value}" }

        return "[${timestampStr}] ${errorInfo.errorType}: ${errorInfo.message}\n" +
               "Thread: ${errorInfo.threadName} (${if (errorInfo.isMainThread) "Main" else "Worker"})\n" +
               "Device: $deviceInfoStr\n" +
               (additionalInfoStr?.let { "Additional info: $it\n" } ?: "") +
               "Stack trace:\n${errorInfo.stackTrace}\n"
    }

    /**
     * 获取所有日志文件
     */
    fun getLogFiles(): List<File> {
        val logDir = getLogDir()
        if (!logDir.exists()) {
            return emptyList()
        }
        return logDir.listFiles()?.toList() ?: emptyList()
    }

    /**
     * 清除所有日志文件
     */
    fun clearLogFiles(): Boolean {
        return try {
            val logDir = getLogDir()
            if (!logDir.exists()) {
                return true
            }
            logDir.listFiles()?.forEach { it.delete() }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear log files", e)
            false
        }
    }
}
