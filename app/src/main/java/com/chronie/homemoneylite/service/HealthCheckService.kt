package com.chronie.homemoneylite.service

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.data.remote.GpcAccountManager
import com.chronie.homemoneylite.data.remote.api.MemberApi
import com.chronie.homemoneylite.ui.eol.EolManageActivity
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class HealthCheckService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:javax.inject.Named("HealthCheckApi") private val memberApi: MemberApi,
    private val gpcAccountManager: GpcAccountManager
) {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var healthCheckJob: Job? = null
    private var consecutiveFailures = 0
    private val maxConsecutiveFailures = 3

    /** 防止重复触发强制退出 */
    @Volatile
    private var sunsetTriggered = false

    companion object {
        private const val CHECK_INTERVAL = 5000L // 5秒
        private const val HEALTH_CHECK_TIMEOUT = 2000L // 2秒超时

        /**
         * 服务截止时间（按 UTC 解释，与服务器 timestamp 字段的 'Z' 时区对齐）。
         * 当服务器返回的 timestamp（UTC 瞬时）大于该值时，判定服务已停止并强制退出。
         * 注意：若服务器 timestamp 实际表示北京时间，请将此处改为 "2026-08-31T15:59:59"（对应 UTC）。
         */
        private const val SERVICE_END_DEADLINE_UTC = "2026-08-31T23:59:59"
        private val SERVICE_END_DEADLINE_MS: Long by lazy {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            fmt.parse(SERVICE_END_DEADLINE_UTC)?.time ?: Long.MAX_VALUE
        }
    }

    fun start() {
        if (healthCheckJob?.isActive == true) {
            android.util.Log.d("HealthCheckService", "Service already running")
            return
        }
        startHealthCheck()
    }

    fun stop() {
        healthCheckJob?.cancel()
        healthCheckJob = null
        consecutiveFailures = 0
    }

    private fun startHealthCheck() {
        android.util.Log.i("HealthCheckService", "Starting health check service")
        healthCheckJob = serviceScope.launch {
            while (isActive) {
                val hasNetwork = isNetworkAvailable()
                android.util.Log.d("HealthCheckService", "Network available: $hasNetwork")
                
                if (hasNetwork) {
                    checkServerHealth()
                } else {
                    android.util.Log.w("HealthCheckService", "No network connection, skipping health check")
                }
                delay(CHECK_INTERVAL.milliseconds)
            }
        }
    }

    private suspend fun checkServerHealth() {
        try {
            android.util.Log.d("HealthCheckService", "Checking server health...")
            
            // 使用超时机制，避免长时间阻塞
            val response = withTimeout(HEALTH_CHECK_TIMEOUT.milliseconds) {
                memberApi.checkHealth()
            }
            
            android.util.Log.d("HealthCheckService", "Health check response: status=${response.status}, database=${response.database}")

            // 服务到期强制退出检查
            enforceServiceEndIfNeeded(response.timestamp)
            
            if (response.status == "OK" && response.database == "connected") {
                if (consecutiveFailures > 0) {
                    android.util.Log.i("HealthCheckService", "Server connection restored")
                    showToast(context.getString(R.string.server_connection_restored))
                }
                consecutiveFailures = 0
            } else {
                android.util.Log.w("HealthCheckService", "Health check failed: status=${response.status}, database=${response.database}")
                handleHealthCheckFailure()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            android.util.Log.e("HealthCheckService", "Health check timeout after ${HEALTH_CHECK_TIMEOUT}ms")
            handleHealthCheckFailure()
        } catch (e: Exception) {
            android.util.Log.e("HealthCheckService", "Health check exception: ${e.message}", e)
            handleHealthCheckFailure()
        }
    }

    private fun handleHealthCheckFailure() {
        consecutiveFailures++
        android.util.Log.w("HealthCheckService", "Consecutive failures: $consecutiveFailures/$maxConsecutiveFailures")
        
        if (consecutiveFailures == maxConsecutiveFailures) {
            android.util.Log.e("HealthCheckService", "Max consecutive failures reached, showing toast")
            showToast(context.getString(R.string.server_connection_error_message))
        }
    }
    
    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 若服务器 timestamp 已超过服务截止时间：
     *  - 若本地缓存的 EOL 延期仍有效（eolUntil > now），则视为已购买延期，正常放行；
     *  - 否则不再崩溃退出，而是跳转至 EOL 管理页，用户可在该页查看状态 / 绑定账号 / 购买延期。
     */
    private fun enforceServiceEndIfNeeded(timestamp: String?) {
        if (sunsetTriggered) return
        val tsMs = parseUtcInstant(timestamp) ?: return
        if (tsMs > SERVICE_END_DEADLINE_MS) {
            // 已购买延期且本地缓存仍有效 → 豁免
            val cachedUntil = gpcAccountManager.getCachedEolUntil()
            if (cachedUntil != null && cachedUntil > System.currentTimeMillis()) {
                android.util.Log.i("HealthCheckService", "Service deadline reached but EOL extension active (until=$cachedUntil), exempt.")
                return
            }
            sunsetTriggered = true
            android.util.Log.w("HealthCheckService", "Service deadline reached (timestamp=$timestamp), redirecting to EOL manage page instead of exit")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, R.string.app_eol_redirect_toast, Toast.LENGTH_LONG).show()
                try {
                    val intent = Intent(context, EolManageActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("HealthCheckService", "Failed to launch EolManageActivity", e)
                }
            }
        }
    }

    /** 将形如 "2026-08-15T23:17:40Z" 的 UTC 时间解析为 epoch 毫秒；解析失败返回 null */
    private fun parseUtcInstant(s: String?): Long? {
        if (s.isNullOrEmpty()) return null
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            fmt.parse(s)?.time
        } catch (e: Exception) {
            android.util.Log.w("HealthCheckService", "Failed to parse timestamp: $s", e)
            null
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                   capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo?.isConnected == true
        }
    }

}
