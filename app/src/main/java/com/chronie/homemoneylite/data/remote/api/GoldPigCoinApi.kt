package com.chronie.homemoneylite.data.remote.api

import com.chronie.homemoneylite.data.remote.dto.ApiResponse
import com.chronie.homemoneylite.data.remote.dto.CreateIntentResultDto
import com.chronie.homemoneylite.data.remote.dto.EolStatusDto
import com.chronie.homemoneylite.data.remote.dto.OAuthTokenDto
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 金猪币（GPC）跨应用支付服务接口。
 * 基础路径走 NetworkModule 的 GPC_BASE_URL（gold-pig-coin 服务地址，端口 3000）。
 *
 * 注意：创建支付单走商户接口（/api/pay/intent），使用 hfm 商户 clientSecret 做 HMAC 签名，
 * 不需要终端用户 token。请求体以 RequestBody 原样发送，确保与签名所用 JSON 完全一致。
 */
interface GoldPigCoinApi {

    /**
     * 商户侧创建跨应用支付单。
     * @param clientId 商户标识（X-Client-Id）
     * @param signature HMAC-SHA256 base64 签名（X-Signature）
     * @param ts 时间戳字符串（X-Ts，与 body.ts 一致）
     * @param body 已签名的 JSON 请求体
     */
    @POST("api/pay/intent")
    suspend fun createIntent(
        @Header("X-Client-Id") clientId: String,
        @Header("X-Signature") signature: String,
        @Header("X-Ts") ts: String,
        @Body body: RequestBody
    ): Response<ApiResponse<CreateIntentResultDto>>

    /**
     * 查询某 hfm 用户的 EOL 延期状态（公开接口，无需签名/token）。
     * @param hfmUserId 绑定的 GPC 用户标识
     */
    @GET("api/pay/eol-status")
    suspend fun getEolStatus(
        @Query("hfmUserId") hfmUserId: String
    ): Response<ApiResponse<EolStatusDto>>

    /**
     * OAuth2 授权码兑换 access_token。
     * @param grantType 固定 "authorization_code"
     * @param code 授权码（来自 gpc://oauth/callback 深链）
     * @param clientId hfm 的 OAuth clientId
     * @param clientSecret hfm 的 OAuth clientSecret
     * @param redirectUri 回跳地址（与授权时一致）
     */
    @FormUrlEncoded
    @POST("api/oauth/token")
    suspend fun exchangeToken(
        @Field("grant_type") grantType: String,
        @Field("code") code: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("redirect_uri") redirectUri: String
    ): Response<ApiResponse<OAuthTokenDto>>
}
