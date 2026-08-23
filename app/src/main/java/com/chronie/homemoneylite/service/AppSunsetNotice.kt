package com.chronie.homemoneylite.service

import com.chronie.homemoneylite.R
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用停运提示：每次进入应用时弹出停运通知对话框。
 */
@Singleton
class AppSunsetNotice @Inject constructor() {

    /**
     * 每次进入应用时弹出停运通知对话框。
     */
    fun showNotice(activity: FragmentActivity) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.sunset_notice_title)
            .setMessage(R.string.sunset_notice_message)
            .setPositiveButton(R.string.confirm) { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }
}
