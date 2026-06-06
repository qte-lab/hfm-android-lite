package com.chronie.homemoney.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    fun clearUsername() {
        prefs.edit().remove(KEY_USERNAME).apply()
    }

    fun isLoggedIn(): Boolean {
        return !getUsername().isNullOrEmpty()
    }

    fun setSkippedLogin(skipped: Boolean) {
        prefs.edit().putBoolean(KEY_SKIPPED_LOGIN, skipped).apply()
    }

    fun hasSkippedLogin(): Boolean {
        return prefs.getBoolean(KEY_SKIPPED_LOGIN, false)
    }

    fun shouldSkipWelcome(): Boolean {
        return isLoggedIn() || hasSkippedLogin()
    }
    
    // 会员订阅信息缓存（用于离线模式）
    fun saveMembershipStatus(isActive: Boolean, planName: String?, endDate: Long?) {
        prefs.edit().apply {
            putBoolean(KEY_MEMBERSHIP_ACTIVE, isActive)
            putString(KEY_MEMBERSHIP_PLAN, planName)
            putLong(KEY_MEMBERSHIP_END_DATE, endDate ?: 0L)
            putLong(KEY_MEMBERSHIP_LAST_CHECK, System.currentTimeMillis())
            apply()
        }
    }
    
    fun isMembershipActive(): Boolean {
        val isActive = prefs.getBoolean(KEY_MEMBERSHIP_ACTIVE, false)
        val endDate = prefs.getLong(KEY_MEMBERSHIP_END_DATE, 0L)
        
        // 如果有结束日期，检查是否过期
        if (endDate > 0) {
            return isActive && System.currentTimeMillis() < endDate
        }
        
        return isActive
    }
    
    fun getMembershipPlanName(): String? {
        return prefs.getString(KEY_MEMBERSHIP_PLAN, null)
    }
    
    fun getMembershipEndDate(): Long? {
        val endDate = prefs.getLong(KEY_MEMBERSHIP_END_DATE, 0L)
        return if (endDate > 0) endDate else null
    }
    
    fun getMembershipLastCheckTime(): Long {
        return prefs.getLong(KEY_MEMBERSHIP_LAST_CHECK, 0L)
    }
    
    fun clearMembershipStatus() {
        prefs.edit().apply {
            remove(KEY_MEMBERSHIP_ACTIVE)
            remove(KEY_MEMBERSHIP_PLAN)
            remove(KEY_MEMBERSHIP_END_DATE)
            remove(KEY_MEMBERSHIP_LAST_CHECK)
            apply()
        }
    }

    // 头像相关功能
    fun saveAvatar(avatar: String) {
        prefs.edit().putString(KEY_AVATAR, avatar).apply()
    }

    fun getAvatar(): String? {
        return prefs.getString(KEY_AVATAR, null)
    }

    fun clearAvatar() {
        prefs.edit().remove(KEY_AVATAR).apply()
    }

    companion object {
        private const val PREFS_NAME = "home_money_prefs"
        private const val KEY_USERNAME = "username"
        private const val KEY_AVATAR = "avatar"
        private const val KEY_SKIPPED_LOGIN = "skipped_login"
        private const val KEY_MEMBERSHIP_ACTIVE = "membership_active"
        private const val KEY_MEMBERSHIP_PLAN = "membership_plan"
        private const val KEY_MEMBERSHIP_END_DATE = "membership_end_date"
        private const val KEY_MEMBERSHIP_LAST_CHECK = "membership_last_check"
    }
}
