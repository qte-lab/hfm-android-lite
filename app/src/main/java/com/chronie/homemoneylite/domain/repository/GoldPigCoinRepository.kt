package com.chronie.homemoneylite.domain.repository

import com.chronie.homemoneylite.domain.model.CreateIntentResult
import com.chronie.homemoneylite.domain.model.EolStatus
import com.chronie.homemoneylite.domain.model.GpcOAuthResult

/**
 * 金猪币（GPC）跨应用支付仓库接口。
 * 说明：hfm 客户端仅负责「以商户身份创建支付单」并拿回 intentId，
 * 后续由 GPC App 完成登录与支付确认。
 */
interface GoldPigCoinRepository {

    /**
     * 以 hfm 商户身份创建跨应用支付单（HMAC 签名，无需终端用户 token）。
     * @return 含 intentId 的创建结果，由 UI 以深链拉起 GPC App 完成支付。
     */
    suspend fun createIntent(
        amount: Double,
        orderNo: String,
        description: String,
        months: Int = 1,
        hfmUserId: String? = null
    ): Result<CreateIntentResult>

    /** 查询某 hfm 用户的 EOL 延期状态 */
    suspend fun getEolStatus(hfmUserId: String): Result<EolStatus>

    /** OAuth2 授权码兑换 access_token（并得到 GPC 用户标识） */
    suspend fun exchangeCode(code: String): Result<GpcOAuthResult>
}
