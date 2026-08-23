package com.chronie.homemoneylite.data.remote

import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * 金猪币（GPC）商户侧 HMAC 签名工具。
 *
 * 契约（与 gold-pig-coin server/src/crypto.js 一致）：
 *  - canonicalize：按 key 升序排序后 JSON.stringify（数字不带引号，无多余空格）
 *  - 签名对象：{ ...请求 body, clientId }
 *  - 算法：HMAC-SHA256，输出 base64（标准 base64，含 '=' 填充）
 *  - 通过请求头 X-Client-Id / X-Signature / X-Ts 传递
 *
 * 说明：clientSecret 内置在客户端仅适用于演示/自研项目；生产环境应由自有后端代理，
 * 避免密钥被反编译泄露。
 */
object GpcMerchantSigner {

    /** hfm 商户标识（与 gold-pig-coin m_hfm_lite 对应） */
    const val CLIENT_ID = "m_hfm_lite"

    /** hfm 商户密钥（与 gold-pig-coin ensureHfmMerchant 中写死的 secret 一致） */
    const val CLIENT_SECRET = "hfm_lite_merchant_secret_change_me"

    /**
     * 对请求体签名，返回待放入 X-Signature 头的 base64 串。
     * @param body 已序列化为 JSONObject 的请求体（不得含 signature 字段）
     */
    fun sign(body: JSONObject): String {
        // 复制并按 key 升序排序，等价于 JS 的 Object.keys().sort()
        val sorted = JSONObject()
        body.keys().asSequence().toList().sorted().forEach { k ->
            sorted.put(k, body.get(k))
        }
        val canonical = sorted.toString()
        val mac = Mac.getInstance("HmacSHA256")
        val key = SecretKeySpec(CLIENT_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(key)
        val raw = mac.doFinal(canonical.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(raw, Base64.NO_WRAP)
    }
}
