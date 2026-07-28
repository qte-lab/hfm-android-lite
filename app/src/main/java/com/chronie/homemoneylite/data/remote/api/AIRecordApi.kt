package com.chronie.homemoneylite.data.remote.api

import com.chronie.homemoneylite.data.remote.dto.AIRecordRequest
import com.chronie.homemoneylite.data.remote.dto.AIRecordResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * AI 记录识别 API（Ollama 原生 chat 端点）
 *
 * 使用 /api/chat 而非 OpenAI 兼容的 /v1/chat/completions，
 * 以支持 think=false 关闭 qwen3 系列的思考模式（大幅缩短响应时间）。
 */
interface AIRecordApi {
    
    /**
     * 调用 AI 模型进行记录识别
     */
    @POST("api/chat")
    suspend fun parseRecord(
        @Body request: AIRecordRequest
    ): Response<AIRecordResponse>
}
