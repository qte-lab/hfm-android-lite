package com.chronie.homemoneylite.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 钱包仓库（远程版）
 * 余额、扣费、封禁数据全部存储在后端钱包服务（wallet-server.js，端口 5010），
 * App 通过 HTTP 访问，本地不再保存钱包数据。
 *
 * 服务器主机自动复用设置页配置的 Ollama 地址的主机名。
 */
@Singleton
class WalletRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "WalletRepository"

        /** 每次 AI 识别扣费金额（元） */
        const val RECOGNITION_COST = 0.1

        /** 钱包服务端口 */
        private const val WALLET_PORT = 5010

        /** 默认服务器主机（与 Ollama 同机） */
        private const val DEFAULT_HOST = "192.168.10.9"

        private const val PREFS_NAME = "wallet_prefs"
        private const val KEY_DEVICE_ID = "device_id"
    }

    /** 钱包信息 */
    data class WalletInfo(
        @SerializedName("ok") val ok: Boolean = false,
        @SerializedName("deviceId") val deviceId: String = "",
        @SerializedName("balance") val balance: Double = 0.0,
        @SerializedName("isBanned") val isBanned: Boolean = false,
        @SerializedName("code") val code: String? = null,
        @SerializedName("message") val message: String? = null
    )

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * 钱包服务 baseUrl：主机硬编码（与 Ollama 同机），端口固定 5010
     */
    private fun walletBaseUrl(): String {
        return "http://$DEFAULT_HOST:$WALLET_PORT"
    }

    /**
     * 获取当前设备 ID（首次调用时自动生成并持久化）
     */
    suspend fun getDeviceId(): String = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = generateDeviceId()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        deviceId
    }

    /**
     * 查询钱包信息（服务端不存在时自动创建，余额 0）
     * 网络失败时抛出异常
     */
    suspend fun getWalletInfo(): WalletInfo = withContext(Dispatchers.IO) {
        val deviceId = getDeviceId()
        val request = Request.Builder()
            .url("${walletBaseUrl()}/api/wallet/$deviceId")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IllegalStateException("空响应")
            gson.fromJson(body, WalletInfo::class.java)
        }
    }

    /**
     * 检查是否可以执行 AI 识别
     * @return Triple(是否允许, 当前余额, 错误信息)
     */
    suspend fun canRecognize(): Triple<Boolean, Double, String?> = withContext(Dispatchers.IO) {
        try {
            val wallet = getWalletInfo()
            when {
                wallet.isBanned -> Triple(false, wallet.balance, "账户已被封禁，无法使用 AI 识别功能")
                wallet.balance < RECOGNITION_COST -> Triple(
                    false,
                    wallet.balance,
                    "余额不足（当前 ¥${String.format("%.2f", wallet.balance)}，需要 ¥${String.format("%.2f", RECOGNITION_COST)}）"
                )
                else -> Triple(true, wallet.balance, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reach wallet server", e)
            Triple(false, 0.0, "无法连接钱包服务（${walletBaseUrl()}），请确认服务已启动")
        }
    }

    /**
     * 扣除一次 AI 识别费用（服务端原子操作：检查封禁/余额后扣费）
     * @return 扣费后的余额；扣费被服务端拒绝或网络失败时返回 -1（不影响已完成的识别结果）
     */
    suspend fun deductRecognitionFee(): Double = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId()
            val request = Request.Builder()
                .url("${walletBaseUrl()}/api/wallet/$deviceId/charge")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext -1.0
                val result = gson.fromJson(body, WalletInfo::class.java)
                if (result.ok) {
                    Log.d(TAG, "Deducted ¥$RECOGNITION_COST, new balance: ¥${result.balance}")
                    result.balance
                } else {
                    Log.w(TAG, "Charge rejected: ${result.message}")
                    -1.0
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to charge", e)
            -1.0
        }
    }

    /**
     * 生成设备唯一 ID（基于 Android ID + 随机数）
     */
    private fun generateDeviceId(): String {
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        val randomSuffix = java.util.UUID.randomUUID().toString().take(8)
        return "${androidId}_$randomSuffix"
    }
}
