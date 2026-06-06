package com.chronie.homemoney.data.remote.api

import com.chronie.homemoney.data.remote.dto.AIRecordRequest
import com.chronie.homemoney.data.remote.dto.AIRecordResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * AI 记录识别 API
 */
interface AIRecordApi {
    
    /**
     * 调用 AI 模型进行记录识别
     */
    @POST("v1/chat/completions")
    suspend fun parseRecord(
        @Body request: AIRecordRequest
    ): Response<AIRecordResponse>
}
