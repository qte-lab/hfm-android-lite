package com.chronie.homemoney.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.chronie.homemoney.R
import com.chronie.homemoney.data.remote.api.MemberApi
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

@Singleton
class HealthCheckService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:javax.inject.Named("HealthCheckApi") private val memberApi: MemberApi
) {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var healthCheckJob: Job? = null
    private var consecutiveFailures = 0
    private val maxConsecutiveFailures = 3

    companion object {
        private const val CHECK_INTERVAL = 5000L // 5秒
        private const val HEALTH_CHECK_TIMEOUT = 2000L // 2秒超时
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
                delay(CHECK_INTERVAL)
            }
        }
    }

    private suspend fun checkServerHealth() {
        try {
            android.util.Log.d("HealthCheckService", "Checking server health...")
            
            // 使用超时机制，避免长时间阻塞
            val response = withTimeout(HEALTH_CHECK_TIMEOUT) {
                memberApi.checkHealth()
            }
            
            android.util.Log.d("HealthCheckService", "Health check response: status=${response.status}, database=${response.database}")
            
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
