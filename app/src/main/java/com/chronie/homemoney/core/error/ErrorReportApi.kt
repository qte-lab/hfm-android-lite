package com.chronie.homemoney.core.error

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 错误上报API接口
 * 定义向后端服务器上报错误信息的端点
 */
interface ErrorReportApi {

    /**
     * 上报错误信息到服务器
     */
    @POST("api/error/report")
    suspend fun reportError(@Body request: ErrorReportRequest): Response<Unit>
}

/**
 * 错误上报请求数据类
 * 包含所有需要上报到服务器的错误信息
 */
data class ErrorReportRequest(
    /**
     * 错误类型
     */
    val errorType: String,

    /**
     * 错误消息
     */
    val message: String,

    /**
     * 堆栈跟踪
     */
    val stackTrace: String? = null,

    /**
     * 错误发生的时间戳
     */
    val timestamp: Long,

    /**
     * 设备信息
     */
    val deviceInfo: Map<String, String>? = null,

    /**
     * 应用版本名称
     */
    val appVersion: String? = null,

    /**
     * 应用构建版本号
     */
    val appBuild: String? = null,

    /**
     * 环境（如：production、development）
     */
    val environment: String? = null,

    /**
     * 会员ID（可选）
     */
    val memberId: String? = null,

    /**
     * 额外信息（可选）
     */
    val additionalInfo: Map<String, String>? = null
)
