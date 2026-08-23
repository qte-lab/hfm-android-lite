package com.chronie.homemoneylite.service

import android.content.SharedPreferences
import com.chronie.homemoneylite.R
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用停运相关的一次性提示：
 * - 首次进入应用时弹出停运通知对话框（仅展示一次，持久化标记控制）。
 */
@Singleton
class AppSunsetNotice @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val PREF_SUNSET_NOTICE_SHOWN = "pref_sunset_notice_shown"
    }

    /**
     * 若用户此前未看过停运通知，则弹出对话框。仅展示一次。
     */
    fun showFirstLaunchNotice(activity: FragmentActivity) {
        val alreadyShown = sharedPreferences.getBoolean(PREF_SUNSET_NOTICE_SHOWN, false)
        if (alreadyShown) return

        // 先落盘标记，避免极端情况下重复弹出
        sharedPreferences.edit().putBoolean(PREF_SUNSET_NOTICE_SHOWN, true).apply()

        AlertDialog.Builder(activity)
            .setTitle(R.string.sunset_notice_title)
            .setMessage(R.string.sunset_notice_message)
            .setPositiveButton(R.string.confirm) { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }
}
