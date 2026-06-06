package com.chronie.homemoney.core.error

import retrofit2.Response

/**
 * ErrorReportApi的模拟实现类
 * 用于在开发和测试环境中模拟错误上报功能
 */
class MockErrorReportApi : ErrorReportApi {
    
    override suspend fun reportError(request: ErrorReportRequest): Response<Unit> {
        println("Mock error report: ${request.errorType} - ${request.message}")
        println("App Version: ${request.appVersion} (${request.appBuild})")
        println("Environment: ${request.environment}")
        println("Device Info: ${request.deviceInfo}")
        
        return Response.success(Unit)
    }
}
