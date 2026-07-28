package com.chronie.homemoneylite.di

import android.content.Context
import com.chronie.homemoneylite.data.remote.api.AIRecordApi
import com.chronie.homemoneylite.data.repository.AIRecordRepositoryImpl
import com.chronie.homemoneylite.domain.repository.AIRecordRepository
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * AI 模块依赖注入
 * 已从 SiliconFlow 云端 API 切换为本地 Ollama 服务（OpenAI 兼容接口）
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AIModule {
    
    /**
     * 绑定 AIRecordRepository
     */
    @Binds
    @Singleton
    abstract fun bindAIRecordRepository(
        impl: AIRecordRepositoryImpl
    ): AIRecordRepository
    
    companion object {
        @Qualifier
        @Retention(AnnotationRetention.BINARY)
        annotation class AIRetrofit
        
        @Qualifier
        @Retention(AnnotationRetention.BINARY)
        annotation class AIOkHttpClient
        
        /** 默认 Ollama 服务地址（局域网） */
        private const val DEFAULT_OLLAMA_BASE_URL = "http://192.168.10.9:11434/"
        
        /** SharedPreferences 键名 */
        private const val PREFS_NAME = "ai_settings"
        private const val KEY_OLLAMA_URL = "ollama_base_url"
        
        /**
         * 提供 AI API 的 OkHttpClient
         * Ollama 不需要 Authorization，仅设置 Content-Type 与超时
         */
        @Provides
        @Singleton
        @AIOkHttpClient
        fun provideAIOkHttpClient(
            @ApplicationContext context: Context
        ): OkHttpClient {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            return OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val original = chain.request()
                    
                    // Ollama 不需要 API Key，但保留读取逻辑以兼容未来切换回云端
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val apiKey = prefs.getString("siliconflow_api_key", "") ?: ""
                    
                    val request = original.newBuilder()
                        .header("Content-Type", "application/json")
                        .apply {
                            if (apiKey.isNotEmpty()) {
                                header("Authorization", "Bearer $apiKey")
                            }
                        }
                        .method(original.method, original.body)
                        .build()
                    
                    chain.proceed(request)
                }
                .addInterceptor(loggingInterceptor)
                .connectTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        }
        
        /**
         * 提供 AI API 的 Retrofit
         * baseUrl 从 SharedPreferences 读取，默认指向本地 Ollama
         */
        @Provides
        @Singleton
        @AIRetrofit
        fun provideAIRetrofit(
            @AIOkHttpClient okHttpClient: OkHttpClient,
            @ApplicationContext context: Context,
            gson: Gson
        ): Retrofit {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val baseUrl = prefs.getString(KEY_OLLAMA_URL, DEFAULT_OLLAMA_BASE_URL)
                ?: DEFAULT_OLLAMA_BASE_URL
            
            // 确保 baseUrl 以 / 结尾（Retrofit 要求）
            val normalizedUrl = if (!baseUrl.endsWith("/")) "$baseUrl/" else baseUrl
            
            return Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        
        /**
         * 提供 AIRecordApi
         */
        @Provides
        @Singleton
        fun provideAIRecordApi(
            @AIRetrofit retrofit: Retrofit
        ): AIRecordApi {
            return retrofit.create(AIRecordApi::class.java)
        }
    }
}
