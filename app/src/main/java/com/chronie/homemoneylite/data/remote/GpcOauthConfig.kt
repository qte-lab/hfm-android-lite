package com.chronie.homemoneylite.data.remote

/**
 * 金猪币（GPC）第三方授权登录（OAuth2 授权码流程）客户端配置。
 *
 * 说明：clientSecret 内置在客户端仅适用于演示/自研项目；生产环境应由 hfm 自有后端
 * （端口 3010）代理 token 兑换，避免密钥被反编译泄露。
 *
 * 回调地址使用 GPC 深链，由 hfm 在 AndroidManifest 注册 intent-filter 拦截：
 *   gpc://oauth/callback?code=...&state=...
 */
object GpcOauthConfig {
    /** hfm 在 GPC 注册的 OAuth clientId（与 GPC seed/管理后台一致） */
    const val CLIENT_ID = "oauth_hfm_lite"

    /** hfm 的 OAuth clientSecret（用于 code → token 兑换） */
    const val CLIENT_SECRET = "hfm_lite_oauth_secret_change_me"

    /** GPC 授权端点（GPC App 内 WebView / 浏览器打开） */
    const val AUTHORIZE_URL = "http://192.168.10.9:3000/api/oauth/authorize"

    /** GPC token 兑换端点 */
    const val TOKEN_URL = "http://192.168.10.9:3000/api/oauth/token"

    /** GPC 深链 scheme（用于拉起 GPC App 授权页 / 回跳） */
    const val GPC_SCHEME = "gpc"

    /** 授权回跳深链（GPC 在用户同意后将 code 通过此深链回传 hfm） */
    const val REDIRECT_URI = "gpc://oauth/callback"

    /** 申请的授权范围（对应 GPC 侧 oauth client 的 scopes） */
    const val SCOPE = "eol"
}
