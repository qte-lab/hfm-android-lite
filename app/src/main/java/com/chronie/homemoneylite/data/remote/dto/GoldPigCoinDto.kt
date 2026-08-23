package com.chronie.homemoneylite.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 金猪币（GPC）跨应用支付 - 后端响应 DTO。
 * 与 gold-pig-coin 的 { success, data, message } 结构对应。
 */

/** GET /api/pay/intents 返回的列表分页 */
data class PaymentIntentListDto(
    @SerializedName("items") val items: List<PaymentIntentDto> = emptyList(),
    @SerializedName("total") val total: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("limit") val limit: Int = 30,
    @SerializedName("pages") val pages: Int = 1
)

/** 单个跨应用支付单 */
data class PaymentIntentDto(
    @SerializedName("intentId") val intentId: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("amount") val amount: Double = 0.0,
    @SerializedName("merchantName") val merchantName: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("orderNo") val orderNo: String? = null,
    @SerializedName("createdAt") val createdAt: Long? = null,
    @SerializedName("expireAt") val expireAt: Long? = null,
    @SerializedName("paidAt") val paidAt: Long? = null,
    @SerializedName("txId") val txId: String? = null,
    @SerializedName("pendingTxId") val pendingTxId: String? = null
)

/** POST /api/pay/intent/:id/confirm 返回结果 */
data class PaymentConfirmResultDto(
    @SerializedName("intentId") val intentId: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("amount") val amount: Double = 0.0,
    @SerializedName("txId") val txId: String? = null,
    @SerializedName("orderNo") val orderNo: String? = null
)

/** confirm 请求体 */
data class PaymentConfirmRequestDto(
    @SerializedName("paymentPassword") val paymentPassword: String
)

/**
 * POST /api/pay/intent 商户侧创建支付单请求体。
 * 由 hfm 商户身份（clientSecret 签名）调用，无需终端用户 token。
 * 注意：ts 为数值时间戳，由调用方在构造时填入。
 */
data class CreateIntentRequestDto(
    @SerializedName("amount") val amount: Double,
    @SerializedName("orderNo") val orderNo: String,
    @SerializedName("description") val description: String,
    @SerializedName("callbackUrl") val callbackUrl: String? = null,
    @SerializedName("expireSec") val expireSec: Int = 600,
    @SerializedName("ts") val ts: Long
)

/** POST /api/pay/intent 返回（商户创建成功） */
data class CreateIntentResultDto(
    @SerializedName("intentId") val intentId: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("amount") val amount: Double = 0.0,
    @SerializedName("payUrl") val payUrl: String? = null,
    @SerializedName("expiresAt") val expiresAt: Long? = null,
    @SerializedName("monthlyPrice") val monthlyPrice: Double? = null,
    @SerializedName("months") val months: Int? = null
)

/** GET /api/pay/eol-status 返回 */
data class EolStatusDto(
    @SerializedName("hfmUserId") val hfmUserId: String = "",
    @SerializedName("eolUntil") val eolUntil: Long = 0L,
    @SerializedName("active") val active: Boolean = false,
    @SerializedName("purchasable") val purchasable: Boolean = false,
    @SerializedName("monthlyPrice") val monthlyPrice: Double? = null,
    @SerializedName("months") val months: Int? = null,
    @SerializedName("totalAmount") val totalAmount: Double? = null,
    @SerializedName("featurePortAvailable") val featurePortAvailable: Boolean = false
)

/** POST /api/oauth/token 返回（授权码换 token） */
data class OAuthTokenDto(
    @SerializedName("access_token") val accessToken: String = "",
    @SerializedName("token_type") val tokenType: String = "Bearer",
    @SerializedName("expires_in") val expiresIn: Int = 0,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("scope") val scope: String? = null,
    @SerializedName("userId") val userId: String = "",
    @SerializedName("username") val username: String? = null
)
