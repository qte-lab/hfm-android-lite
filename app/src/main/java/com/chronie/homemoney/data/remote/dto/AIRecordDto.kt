package com.chronie.homemoney.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * AI 记录请求 DTO
 */
data class AIRecordRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("messages")
    val messages: List<AIMessage>,
    @SerializedName("temperature")
    val temperature: Double = 0.2,
    @SerializedName("stream")
    val stream: Boolean = false
)

/**
 * AI 消息
 */
data class AIMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: Any // 可以是 String 或 List<AIMessageContent>
)

/**
 * AI 消息内容（用于多模态）
 */
data class AIMessageContent(
    @SerializedName("type")
    val type: String, // "text" 或 "image_url"
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("image_url")
    val imageUrl: AIImageUrl? = null
)

/**
 * AI 图片 URL
 */
data class AIImageUrl(
    @SerializedName("url")
    val url: String
)

/**
 * AI 记录响应 DTO
 */
data class AIRecordResponse(
    @SerializedName("choices")
    val choices: List<AIChoice>
)

/**
 * AI 选择
 */
data class AIChoice(
    @SerializedName("message")
    val message: AIResponseMessage
)

/**
 * AI 响应消息
 */
data class AIResponseMessage(
    @SerializedName("content")
    val content: String
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
