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
import com.chronie.homemoneylite.data.remote.api.GoldPigCoinApi
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
    private val gpcAccountManager: GpcAccountManager,
    private val gpcApi: GoldPigCoinApi
) {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var healthCheckJob: Job? = null
    private var consecutiveFailures = 0
    private val maxConsecutiveFailures = 3

    /**
     * EOL 强制管理页当前是否处于前台。
     * 由 EolManageActivity 在其 onCreate / onDestroy 维护。
     *
     * 用途：
     *  1) 避免对已在前台的强制页重复拉起（每 5 秒一次的健康检查若每次都 startActivity 会反复打断用户）；
     *  2) 用户通过多任务/返回等方式关闭强制页后，下次健康检查会重新拉起，
     *     实现「服务到期必须停留 EOL 管理页」的强制保持。
     *
     * 注意：原先使用一次性置位的 sunsetTriggered 标志，置位后便永久不再拉起，
     * 导致强制页被关闭一次后即彻底失效（正是本 bug 的根因）。
     */
    @Volatile
    var eolForcedActivityVisible: Boolean = false

    /**
     * 服务端时间偏移量：serverTimeOffset = 服务器 timestamp(ms) - 客户端本地时钟(ms)。
     * 所有 EOL 到期判断都必须基于服务端时间轴（serverNow = 本地时钟 + offset），
     * 而非客户端本地时钟，避免用户手动修改系统时间/时区导致误判。
     */
    @Volatile
    private var serverTimeOffset: Long = 0L

    /**
     * 以服务端时间轴返回的「当前时刻」(epoch 毫秒)。
     * EOL 到期 / 缓存新鲜度等判断一律调用此方法，切勿直接使用 System.currentTimeMillis()。
     */
    fun serverNowMillis(): Long = System.currentTimeMillis() + serverTimeOffset

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

            // 以服务器 timestamp 校准本地时间偏移量（防止用户篡改系统时钟影响 EOL 判断）
            val tsMs = parseUtcInstant(response.timestamp)
            if (tsMs != null) {
                serverTimeOffset = tsMs - System.currentTimeMillis()
                android.util.Log.d("HealthCheckService", "Calibrated serverTimeOffset=$serverTimeOffset ms")
            }

            // 服务到期强制退出检查（统一基于服务端时间轴）
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
     *  - 实时向 GPC 服务端查询该用户 EOL 延期状态（服务端为唯一真相源）；
     *    成功则以其返回值判定是否豁免，并刷新本地缓存；
     *  - 若实时查询失败（无网/超时），则回退到本地缓存的 EOL 到期日判定；
     *  - 本地缓存超过 7 个自然日未成功从服务器更新时，视为「必须重新联网刷新」，
     *    即便本地仍显示有效也不再豁免，跳转 EOL 管理页促使用户联网确认。
     *  - 否则不再崩溃退出，而是跳转至 EOL 管理页，用户可在该页查看状态 / 绑定账号 / 购买延期。
     */
    private suspend fun enforceServiceEndIfNeeded(timestamp: String?) {
        val tsMs = parseUtcInstant(timestamp) ?: return
        if (tsMs <= SERVICE_END_DEADLINE_MS) return

        // 统一以服务端时间轴判定「现在」
        val serverNow = serverNowMillis()

        // 强制页已在前台：不重复拉起（避免每 5 秒打断用户）。
        // 注意：若用户已关闭该页，eolForcedActivityVisible 会被置为 false，下次检查将重新拉起。
        if (eolForcedActivityVisible) return

        // 服务已到期：先尝试实时拉取 GPC 真相
        val uid = gpcAccountManager.getBoundUserId()
        var serverUntil: Long? = null
        if (!uid.isNullOrBlank()) {
            serverUntil = fetchEolUntilFromServer(uid)
        }

        if (serverUntil != null) {
            // 实时成功：以服务端返回为准（serverUntil 与服务端时间轴一致，直接用 serverNow 比较）
            if (serverUntil > serverNow) {
                android.util.Log.i("HealthCheckService", "Service deadline reached but EOL active per GPC server (until=$serverUntil), exempt.")
                return
            }
        } else {
            // 实时失败：回退本地缓存（但缓存过期则不可豁免）
            val cachedUntil = gpcAccountManager.getCachedEolUntil()
            if (cachedUntil != null && cachedUntil > serverNow
                && !gpcAccountManager.isEolCacheStale(serverNow)
            ) {
                android.util.Log.w("HealthCheckService", "Service deadline reached; GPC unreachable, fallback to fresh local cache (until=$cachedUntil), exempt.")
                return
            }
            if (gpcAccountManager.isEolCacheStale(serverNow)) {
                android.util.Log.w("HealthCheckService", "Local EOL cache stale (>7d) and GPC unreachable; cannot grant exemption.")
            }
        }

        android.util.Log.w("HealthCheckService", "Service deadline reached (timestamp=$timestamp), redirecting to EOL manage page instead of exit")
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, R.string.app_eol_redirect_toast, Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(context, EolManageActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // 标记为「强制模式」：用户必须停留在此页购买延期，不可返回退出
                    putExtra(EolManageActivity.EXTRA_FORCED, true)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e("HealthCheckService", "Failed to launch EolManageActivity", e)
            }
        }
    }

    /**
     * 实时向 GPC 服务端查询某 hfm 用户的 EOL 到期日（公开接口，无需 token）。
     * 成功返回 epoch 毫秒并刷新本地缓存；任何失败返回 null。
     */
    private suspend fun fetchEolUntilFromServer(hfmUserId: String): Long? {
        return try {
            val resp = withTimeout(3000.milliseconds) {
                gpcApi.getEolStatus(hfmUserId)
            }
            if (resp.isSuccessful && resp.body()?.success == true) {
                val d = resp.body()?.data
                val until = d?.eolUntil
                if (until != null && until > 0L) {
                    gpcAccountManager.cacheEolUntil(if (d.active) until else null)
                    until
                } else null
            } else null
        } catch (e: Exception) {
            android.util.Log.w("HealthCheckService", "fetchEolUntilFromServer failed: ${e.message}")
            null
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
