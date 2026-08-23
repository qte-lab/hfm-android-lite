package com.chronie.homemoneylite.domain.model

/**
 * 金猪币（GPC）跨应用支付 - 领域模型。
 */

/** 支付单状态（与 gold-pig-coin 状态集对应） */
enum class PaymentStatus {
    PENDING,          // 待支付
    PAID,             // 已支付
    PENDING_REVIEW,   // 待审核（小额）
    EXPIRED,          // 已过期
    CANCELLED,        // 已取消
    REJECTED;         // 已驳回

    companion object {
        fun from(value: String?): PaymentStatus = when (value) {
            "pending" -> PENDING
            "paid" -> PAID
            "pending_review" -> PENDING_REVIEW
            "expired" -> EXPIRED
            "cancelled" -> CANCELLED
            "rejected" -> REJECTED
            else -> PENDING
        }
    }
}

/** 单个跨应用支付单 */
data class PaymentIntent(
    val intentId: String,
    val status: PaymentStatus,
    val amount: Double,
    val merchantName: String? = null,
    val description: String? = null,
    val orderNo: String? = null,
    val createdAt: Long? = null,
    val expireAt: Long? = null,
    val paidAt: Long? = null,
    val txId: String? = null,
    val pendingTxId: String? = null
)

/** 确认支付结果 */
data class PaymentConfirmResult(
    val intentId: String,
    val status: PaymentStatus,
    val amount: Double,
    val txId: String? = null,
    val orderNo: String? = null
)

/** 商户创建支付单的返回（拿到 intentId 后以深链拉起 GPC App 完成支付） */
data class CreateIntentResult(
    val intentId: String,
    val status: String,
    val amount: Double,
    val payUrl: String? = null,
    val expiresAt: Long? = null,
    val monthlyPrice: Double? = null,
    val months: Int? = null
)

/** GPC 侧某 hfm 用户的 EOL 延期状态 */
data class EolStatus(
    val hfmUserId: String,
    val eolUntil: Long,
    val active: Boolean,
    val purchasable: Boolean,
    val monthlyPrice: Double? = null,
    /** 服务端决定的购买月数（客户端不可改） */
    val months: Int = 1,
    /** 本次应付总价（服务端计算） */
    val totalAmount: Double? = null,
    /** 新功能移植是否可购买（2027 起停售） */
    val featurePortAvailable: Boolean = true
)

/** OAuth 授权结果（授权码换 token 后得到） */
data class GpcOAuthResult(
    val userId: String,
    val username: String?,
    val accessToken: String
)

/** 可购买商品类型 */
enum class GpcProductType {
    EOL_EXTEND,    // 延长 EOL 支持期（按月订阅）
    FEATURE_PORT   // 新功能移植（自定义金额）
}

/** 金猪币可购买商品 */
data class GpcProduct(
    val type: GpcProductType,
    val title: String,
    val description: String,
    val amount: Double,
    val customAmount: Boolean,
    val minCustom: Double = 0.0,
    val maxCustom: Double = 0.0,
    val customNotice: String? = null
) {
    /**
     * 生成商户侧订单号（与 gold-pig-coin 脚本 create-hfm-payment.js 格式一致）：
     * HFM-EOL-<YYYYMMDD>-<rand> / HFM-PORT-<YYYYMMDD>-<rand>
     */
    fun orderNo(): String {
        val tag = if (type == GpcProductType.EOL_EXTEND) "EOL" else "PORT"
        val stamp = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
            .format(java.util.Date())
        val rand = java.util.Random().nextInt(0xFFFF + 1)
        val randHex = java.lang.Integer.toHexString(rand).uppercase().padStart(4, '0')
        return "HFM-$tag-$stamp-$randHex"
    }
}
