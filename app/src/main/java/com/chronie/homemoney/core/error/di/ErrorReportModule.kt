package com.chronie.homemoney.core.error.di

import android.content.Context
import com.chronie.homemoney.core.error.ErrorReportApi
import com.chronie.homemoney.core.error.ErrorReporter
import com.chronie.homemoney.core.error.LogFileManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * 错误收集模块
 * 提供错误收集相关的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object ErrorReportModule {

    /**
     * 提供ErrorReportApi实例
     * 复用应用中已有的Retrofit实例
     */
    @Provides
    fun provideErrorReportApi(retrofit: Retrofit): ErrorReportApi {
        return retrofit.create(ErrorReportApi::class.java)
    }

    /**
     * 提供LogFileManager实例
     */
    @Provides
    @Singleton
    fun provideLogFileManager(@ApplicationContext context: Context): LogFileManager {
        return LogFileManager(context)
    }
}