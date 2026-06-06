package com.chronie.homemoney.data.remote.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * 日志拦截器 - 记录网络请求和响应
 */
class LoggingInterceptor : Interceptor {
    
    companion object {
        private const val TAG = "NetworkLog"
        private const val MAX_LOG_LENGTH = 4000
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // 记录请求
        val requestStartTime = System.currentTimeMillis()
        Log.d(TAG, "→ ${request.method} ${request.url}")
        
        // 记录请求头
        request.headers.forEach { (name, value) ->
            // 不记录敏感信息
            if (name.equals("Authorization", ignoreCase = true)) {
                Log.d(TAG, "  $name: [REDACTED]")
            } else {
                Log.d(TAG, "  $name: $value")
            }
        }
        
        // 记录请求体
        request.body?.let { body ->
            try {
                val buffer = Buffer()
                body.writeTo(buffer)
                val charset = body.contentType()?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
                val content = buffer.readString(charset)
                logLongString("  Request Body: $content")
            } catch (e: IOException) {
                Log.e(TAG, "  Failed to read request body", e)
            }
        }
        
        // 执行请求
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            Log.e(TAG, "← Request failed: ${e.message}", e)
            throw e
        }
        
        val requestDuration = System.currentTimeMillis() - requestStartTime
        
        // 记录响应
        Log.d(TAG, "← ${response.code} ${request.url} (${requestDuration}ms)")
        
        // 记录响应头
        response.headers.forEach { (name, value) ->
            Log.d(TAG, "  $name: $value")
        }
        
        // 记录响应体
        val responseBody = response.body
        if (responseBody != null) {
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            val buffer = source.buffer
            
            val charset: Charset = responseBody.contentType()?.charset(StandardCharsets.UTF_8) 
                ?: StandardCharsets.UTF_8
            
            if (responseBody.contentLength() != 0L) {
                val content = buffer.clone().readString(charset)
                logLongString("  Response Body: $content")
            }
        }
        
        return response
    }
    
    /**
     * 分段记录长字符串，避免超过Android Log的长度限制
     */
    private fun logLongString(message: String) {
        if (message.length <= MAX_LOG_LENGTH) {
            Log.d(TAG, message)
        } else {
            var i = 0
            while (i < message.length) {
                val end = minOf(i + MAX_LOG_LENGTH, message.length)
                Log.d(TAG, message.substring(i, end))
                i = end
            }
        }
    }
}
