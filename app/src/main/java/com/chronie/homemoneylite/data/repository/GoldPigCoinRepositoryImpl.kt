package com.chronie.homemoneylite.data.repository

import com.chronie.homemoneylite.data.remote.GpcMerchantSigner
import com.chronie.homemoneylite.data.remote.GpcOauthConfig
import com.chronie.homemoneylite.data.remote.api.GoldPigCoinApi
import com.chronie.homemoneylite.data.remote.dto.CreateIntentResultDto
import com.chronie.homemoneylite.data.remote.dto.EolStatusDto
import com.chronie.homemoneylite.domain.model.CreateIntentResult
import com.chronie.homemoneylite.domain.model.EolStatus
import com.chronie.homemoneylite.domain.model.GpcOAuthResult
import com.chronie.homemoneylite.domain.repository.GoldPigCoinRepository
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * 金猪币跨应用支付仓库实现（商户侧创建支付单）。
 *
 * 采用与 gold-pig-coin server/src/crypto.js 完全一致的签名契约：
 *  - 请求体字段按 key 升序排序后 JSON 序列化
 *  - 对 { ...body, clientId } 做 HMAC-SHA256(base64)
 *  - 通过 X-Client-Id / X-Signature / X-Ts 头传递
 * 请求体使用原始 JSONObject 字符串发送，确保与签名所用内容逐字节一致。
 */
@Singleton
class GoldPigCoinRepositoryImpl @Inject constructor(
    private val api: GoldPigCoinApi
) : GoldPigCoinRepository {

    override suspend fun createIntent(
        amount: Double,
        orderNo: String,
        description: String,
        months: Int,
        hfmUserId: String?
    ): Result<CreateIntentResult> = runCatchingNet {
        val ts = System.currentTimeMillis()
        // 1) 构造请求体（注意：不含 signature 字段）
        val bodyJson = JSONObject().apply {
            put("amount", amount)
            put("orderNo", orderNo)
            put("description", description)
            put("callbackUrl", JSONObject.NULL)
            put("expireSec", 600)
            put("ts", ts)
            put("months", months)
            if (!hfmUserId.isNullOrBlank()) put("hfmUserId", hfmUserId)
        }
        // 2) 按 { ...body, clientId } 签名（与服务端验证一致）
        val payload = JSONObject(bodyJson.toString()).apply {
            put("clientId", GpcMerchantSigner.CLIENT_ID)
        }
        val signature = GpcMerchantSigner.sign(payload)
        val requestBody =
            bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val resp = withTimeout(8.seconds) {
            api.createIntent(
                clientId = GpcMerchantSigner.CLIENT_ID,
                signature = signature,
                ts = ts.toString(),
                body = requestBody
            )
        }
        if (resp.isSuccessful && resp.body() != null && resp.body()!!.success) {
            val d = resp.body()!!.data
            if (d != null) {
                Result.success(
                    CreateIntentResult(
                        intentId = d.intentId,
                        status = d.status,
                        amount = d.amount,
                        payUrl = d.payUrl,
                        expiresAt = d.expiresAt,
                        monthlyPrice = d.monthlyPrice,
                        months = d.months
                    )
                )
            } else {
                Result.failure(Exception("创建支付单失败"))
            }
        } else {
            val msg = resp.body()?.message ?: "服务器错误 ${resp.code()}"
            Result.failure(Exception(msg))
        }
    }

    override suspend fun getEolStatus(hfmUserId: String): Result<EolStatus> = runCatchingNet {
        val resp = withTimeout(8.seconds) {
            api.getEolStatus(hfmUserId)
        }
        if (resp.isSuccessful && resp.body() != null && resp.body()!!.success) {
            val d: EolStatusDto? = resp.body()!!.data
            if (d != null) {
                Result.success(
                    EolStatus(
                        hfmUserId = d.hfmUserId,
                        eolUntil = d.eolUntil,
                        active = d.active,
                        purchasable = d.purchasable,
                        monthlyPrice = d.monthlyPrice,
                        months = d.months ?: 1,
                        totalAmount = d.totalAmount,
                        featurePortAvailable = d.featurePortAvailable
                    )
                )
            } else {
                Result.failure(Exception("查询 EOL 状态失败"))
            }
        } else {
            val msg = resp.body()?.message ?: "服务器错误 ${resp.code()}"
            Result.failure(Exception(msg))
        }
    }

    override suspend fun exchangeCode(code: String): Result<GpcOAuthResult> = runCatchingNet {
        val resp = withTimeout(8.seconds) {
            api.exchangeToken(
                grantType = "authorization_code",
                code = code,
                clientId = GpcOauthConfig.CLIENT_ID,
                clientSecret = GpcOauthConfig.CLIENT_SECRET,
                redirectUri = GpcOauthConfig.REDIRECT_URI
            )
        }
        if (resp.isSuccessful && resp.body() != null && resp.body()!!.success) {
            val d = resp.body()!!.data
            if (d != null && d.userId.isNotBlank() && d.accessToken.isNotBlank()) {
                Result.success(
                    GpcOAuthResult(
                        userId = d.userId,
                        username = d.username,
                        accessToken = d.accessToken
                    )
                )
            } else {
                Result.failure(Exception("授权登录失败"))
            }
        } else {
            // OAuth 错误体可能为 { error, error_description }
            val msg = resp.body()?.message ?: "服务器错误 ${resp.code()}"
            Result.failure(Exception(msg))
        }
    }

    private suspend fun <T> runCatchingNet(block: suspend () -> Result<T>): Result<T> = try {
        block()
    } catch (e: Exception) {
        Result.failure(e)
    }
}
