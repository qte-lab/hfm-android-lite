package com.chronie.homemoney.di

import android.content.Context
import android.content.SharedPreferences
import com.chronie.homemoney.data.remote.api.*
import com.chronie.homemoney.data.remote.interceptor.AuthInterceptor
import com.chronie.homemoney.data.remote.interceptor.ErrorHandlingInterceptor
import com.chronie.homemoney.data.remote.interceptor.LoggingInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 网络模块 - 提供Retrofit和API接口实例
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    // 注意：BASE_URL不包含/api/，因为各个API接口会自己添加路径前缀
    private const val BASE_URL = "http://192.168.10.9:3010/"
    private const val CONNECT_TIMEOUT = 5L
    private const val READ_TIMEOUT = 5L
    private const val WRITE_TIMEOUT = 5L
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()
    }
    
    @Provides
    @Singleton
    fun provideAuthInterceptor(
        sharedPreferences: SharedPreferences
    ): AuthInterceptor {
        return AuthInterceptor(sharedPreferences)
    }
    
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): LoggingInterceptor {
        return LoggingInterceptor()
    }
    
    @Provides
    @Singleton
    fun provideErrorHandlingInterceptor(): ErrorHandlingInterceptor {
        return ErrorHandlingInterceptor()
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: LoggingInterceptor,
        errorHandlingInterceptor: ErrorHandlingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(errorHandlingInterceptor) // 错误处理放在最前面
            .addInterceptor(authInterceptor) // 认证拦截器
            .addInterceptor(loggingInterceptor) // 日志拦截器放在最后
            .retryOnConnectionFailure(true) // 连接失败时重试
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideExpenseApi(retrofit: Retrofit): ExpenseApi {
        return retrofit.create(ExpenseApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideMemberApi(retrofit: Retrofit): MemberApi {
        return retrofit.create(MemberApi::class.java)
    }
    
    @Provides
    @Singleton
    @javax.inject.Named("HealthCheckClient")
    fun provideHealthCheckOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: LoggingInterceptor,
        errorHandlingInterceptor: ErrorHandlingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS) // 健康检查使用更短的超时
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .addInterceptor(errorHandlingInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(false) // 健康检查不重试
            .build()
    }
    
    @Provides
    @Singleton
    @javax.inject.Named("HealthCheckRetrofit")
    fun provideHealthCheckRetrofit(
        @javax.inject.Named("HealthCheckClient") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    @javax.inject.Named("HealthCheckApi")
    fun provideHealthCheckMemberApi(
        @javax.inject.Named("HealthCheckRetrofit") retrofit: Retrofit
    ): MemberApi {
        return retrofit.create(MemberApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }
}
