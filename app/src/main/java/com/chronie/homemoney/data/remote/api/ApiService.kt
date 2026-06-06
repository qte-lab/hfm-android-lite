package com.chronie.homemoney.data.remote.api

import com.chronie.homemoney.data.remote.dto.HealthDto
import retrofit2.Response
import retrofit2.http.GET

/**
 * 通用API服务接口
 */
interface ApiService {
    
    /**
     * 健康检查
     */
    @GET("api/health/lite")
    suspend fun checkHealth(): HealthDto
}
