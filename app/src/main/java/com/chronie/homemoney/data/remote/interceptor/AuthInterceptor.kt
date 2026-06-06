package com.chronie.homemoney.data.remote.interceptor

import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * 认证拦截器 - 自动添加JWT令牌到请求头
 */
class AuthInterceptor @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : Interceptor {
    
    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val TOKEN_PREFIX = "Bearer "
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // 如果请求头中已经有Authorization，不覆盖
        if (originalRequest.header(HEADER_AUTHORIZATION) != null) {
            return chain.proceed(originalRequest)
        }
        
        // 从SharedPreferences获取token
        val token = sharedPreferences.getString(KEY_TOKEN, null)
        
        // 如果没有token，直接发送原始请求
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }
        
        // 添加Authorization头
        val newRequest = originalRequest.newBuilder()
            .header(HEADER_AUTHORIZATION, "$TOKEN_PREFIX$token")
            .build()
        
        return chain.proceed(newRequest)
    }
}
