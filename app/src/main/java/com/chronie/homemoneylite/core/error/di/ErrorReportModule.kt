package com.chronie.homemoneylite.core.error.di

import android.content.Context
import com.chronie.homemoneylite.core.error.LogFileManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 错误收集模块
 * 提供错误收集相关的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object ErrorReportModule {

    /**
     * 提供LogFileManager实例
     */
    @Provides
    @Singleton
    fun provideLogFileManager(@ApplicationContext context: Context): LogFileManager {
        return LogFileManager(context)
    }
}
