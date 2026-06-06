package com.chronie.homemoney.core.error

import android.os.Build
import android.os.Looper

/**
 * 线程工具类
 * 提供线程相关的工具方法，处理不同Android版本的兼容性问题
 */
object ThreadUtils {

    /**
     * 获取线程ID
     * 兼容Android 14以下版本
     */
    fun getThreadId(thread: Thread): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            thread.threadId()
        } else {
            @Suppress("DEPRECATION")
            thread.id
        }
    }

    /**
     * 检查当前线程是否是主线程
     */
    fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }

    /**
     * 获取主线程ID
     */
    fun getMainThreadId(): Long {
        return getThreadId(Looper.getMainLooper().thread)
    }
}
