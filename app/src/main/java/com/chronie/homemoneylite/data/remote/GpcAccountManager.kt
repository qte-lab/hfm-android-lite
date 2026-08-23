package com.chronie.homemoneylite.data.remote

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GPC（金猪币）账号绑定管理。
 *
 * 绑定方式：hfm 通过 OAuth2 授权码流程，由用户在 GPC App 内登录并「授权 hfm」，
 * GPC 通过深链 gpc://oauth/callback 回传授权码，hfm 用 clientSecret 兑换
 * access_token 与 userId，本地持久化（即为「绑定」）。解绑即清除本地记录。
 *
 * 绑定的 userId 即 hfmUserId，用于与 GPC 服务端交互 EOL 延期状态/购买。
 */
@Singleton
class GpcAccountManager @Inject constructor(
    private val prefs: SharedPreferences
) {
    companion object {
        private const val KEY_HFM_USER_ID = "gpc_hfm_user_id"
        private const val KEY_GPC_USERNAME = "gpc_username"
        private const val KEY_ACCESS_TOKEN = "gpc_access_token"
        private const val KEY_EOL_UNTIL = "gpc_eol_until"
        /** 最后一次成功从 GPC 服务器更新 EOL 缓存的时间戳（epoch 毫秒），用于 7 日强制刷新判断 */
        private const val KEY_EOL_CACHE_UPDATED_AT = "gpc_eol_cache_updated_at"
        /** EOL 缓存最长有效天数：超过该天数必须再次成功从服务器刷新 */
        const val EOL_CACHE_MAX_AGE_DAYS = 7L
    }

    /** 是否已绑定 GPC 账号（OAuth 授权成功） */
    val isBound: Boolean
        get() = getBoundUserId() != null

    /** 返回当前绑定的 hfmUserId（即 GPC 用户 id），未绑定返回 null */
    fun getBoundUserId(): String? {
        val v = prefs.getString(KEY_HFM_USER_ID, null)
        return if (v.isNullOrBlank()) null else v
    }

    /** 返回绑定的 GPC 用户名（用于展示），未绑定返回 null */
    fun getBoundUsername(): String? {
        val v = prefs.getString(KEY_GPC_USERNAME, null)
        return if (v.isNullOrBlank()) null else v
    }

    /** 返回持久化的 access_token（OAuth），未绑定返回 null */
    fun getAccessToken(): String? {
        val v = prefs.getString(KEY_ACCESS_TOKEN, null)
        return if (v.isNullOrBlank()) null else v
    }

    /**
     * 以 OAuth 授权结果完成绑定（保存 userId / username / access_token）。
     * @param userId  GPC 用户 id（即 hfmUserId）
     * @param username GPC 用户名（展示用）
     * @param accessToken OAuth access_token
     */
    fun bindOAuth(userId: String, username: String?, accessToken: String?) {
        prefs.edit()
            .putString(KEY_HFM_USER_ID, userId.trim())
            .putString(KEY_GPC_USERNAME, username?.trim() ?: "")
            .putString(KEY_ACCESS_TOKEN, accessToken ?: "")
            .apply()
    }

    /** 兼容：手动绑定（仅演示/兜底用） */
    fun bind(userId: String) {
        prefs.edit().putString(KEY_HFM_USER_ID, userId.trim()).apply()
    }

    /** 解绑 GPC 账号（同时清除 token 与本地缓存的 EOL 到期日） */
    fun unbind() {
        prefs.edit()
            .remove(KEY_HFM_USER_ID)
            .remove(KEY_GPC_USERNAME)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_EOL_UNTIL)
            .apply()
    }

    /** 读取本地缓存的 EOL 到期日（epoch 毫秒），无返回 null */
    fun getCachedEolUntil(): Long? {
        val v = prefs.getLong(KEY_EOL_UNTIL, 0L)
        return if (v > 0) v else null
    }

    /** 更新本地缓存的 EOL 到期日，并顺带记录本次更新时间戳 */
    fun cacheEolUntil(until: Long?) {
        if (until == null) {
            prefs.edit().remove(KEY_EOL_UNTIL).apply()
        } else {
            prefs.edit()
                .putLong(KEY_EOL_UNTIL, until)
                .putLong(KEY_EOL_CACHE_UPDATED_AT, System.currentTimeMillis())
                .apply()
        }
    }

    /** 读取最后一次成功从服务器更新 EOL 缓存的时间戳（epoch 毫秒），无记录返回 0 */
    fun getEolCacheUpdatedAt(): Long = prefs.getLong(KEY_EOL_CACHE_UPDATED_AT, 0L)

    /** 判断本地 EOL 缓存是否已过期（距上次成功更新 ≥ EOL_CACHE_MAX_AGE_DAYS 天） */
    fun isEolCacheStale(): Boolean {
        val last = getEolCacheUpdatedAt()
        if (last <= 0) return true
        val ageMs = System.currentTimeMillis() - last
        return ageMs >= EOL_CACHE_MAX_AGE_DAYS * 24 * 60 * 60 * 1000L
    }
}
