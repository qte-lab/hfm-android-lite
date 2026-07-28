package com.chronie.homemoneylite.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Ollama 原生 /api/chat 请求 DTO
 *
 * 注意：使用原生端点而非 OpenAI 兼容端点（/v1/chat/completions），
 * 因为只有原生端点支持根级 think 参数——qwen3 系列默认开启 thinking，
 * 会先输出大段推理过程导致等待时间过长，必须显式 think=false 关闭。
 */
data class AIRecordRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("messages")
    val messages: List<AIMessage>,
    @SerializedName("stream")
    val stream: Boolean = false,
    /** 关闭思考模式：直接输出答案，大幅缩短响应时间 */
    @SerializedName("think")
    val think: Boolean = false,
    @SerializedName("options")
    val options: AIOptions = AIOptions()
)

/**
 * Ollama 运行时参数（对应 OpenAI 的顶级 temperature 等）
 */
data class AIOptions(
    @SerializedName("temperature")
    val temperature: Double = 0.2
)

/**
 * AI 消息（原生 chat 端点 content 只支持字符串）
 */
data class AIMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
)

/**
 * Ollama 原生 /api/chat 响应 DTO（非流式）
 */
data class AIRecordResponse(
    @SerializedName("message")
    val message: AIResponseMessage?,
    @SerializedName("done")
    val done: Boolean = true
)

/**
 * AI 响应消息
 */
data class AIResponseMessage(
    @SerializedName("content")
    val content: String,
    /** think=true 时的推理轨迹；已关闭 thinking，正常为 null */
    @SerializedName("thinking")
    val thinking: String? = null
)

/**
 * AI 识别的支出记录 DTO
 */
data class AIExpenseRecordDto(
    @SerializedName("type")
    val type: String,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("date")
    val date: String,
    @SerializedName("remark")
    val remark: String
)
