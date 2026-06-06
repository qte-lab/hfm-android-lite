package com.chronie.homemoney.core.error

/**
 * 错误信息数据类
 * 包含错误的所有相关信息
 */
data class ErrorInfo(
    val errorType: String,
    val message: String,
    val stackTrace: String,
    val threadName: String,
    val isMainThread: Boolean,
    val timestamp: Long,
    val deviceInfo: Map<String, String>,
    val additionalInfo: Map<String, String>? = null
)
