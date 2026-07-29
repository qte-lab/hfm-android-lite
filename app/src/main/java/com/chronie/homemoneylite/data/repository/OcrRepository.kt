package com.chronie.homemoneylite.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 服务端 OCR 仓库
 *
 * OCR 任务已迁移到后端 wallet-server.js（端口 5010，tesseract.js 中文识别）。
 * App 把裁剪后的图片以 base64 JSON 形式上传到 POST /api/ocr，服务端返回识别文字，
 * 再交由本地 LLM 做结构化解析。
 *
 * 设备端仍保留「识别文字确认弹窗」：本仓库只负责拿到文字，是否发送 AI 由视图层决定，
 * 用户可在弹窗里查看/修改文字。
 */
@Singleton
class OcrRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "OcrRepository"

        /** 后端 OCR 服务地址（与钱包服务同机同端口，硬编码） */
        private const val OCR_BASE_URL = "http://192.168.10.9:5010"

        /** 识别超时：tesseract.js 首次需下载语言包，给足时间 */
        private const val OCR_TIMEOUT_SECONDS = 60L
    }

    private data class OcrResponse(
        @SerializedName("ok") val ok: Boolean = false,
        @SerializedName("text") val text: String? = null,
        @SerializedName("message") val message: String? = null
    )

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(OCR_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(OCR_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * 识别一组图片，按「【图片N】」分段拼接为一段文本。
     * 单张失败不影响其他图片；若全部失败且存在错误则抛出异常。
     */
    suspend fun recognize(uris: List<Uri>): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        var lastError: String? = null
        uris.forEachIndexed { index, uri ->
            try {
                val b64 = uriToBase64(uri)
                val payload = gson.toJson(mapOf("image" to b64))
                val request = Request.Builder()
                    .url("$OCR_BASE_URL/api/ocr")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { resp ->
                    val raw = resp.body.string().takeIf { it.isNotBlank() }
                        ?: throw IllegalStateException("空响应")
                    val result = gson.fromJson(raw, OcrResponse::class.java)
                    val text = result.text?.takeIf { it.isNotBlank() }
                    if (!result.ok || text == null) {
                        lastError = result.message ?: "OCR 未返回文字"
                        Log.w(TAG, "OCR empty/failed for image ${index + 1}: ${result.message}")
                        return@forEachIndexed
                    }
                    if (uris.size > 1) sb.append("【图片${index + 1}】\n")
                    sb.append(text.trim()).append("\n\n")
                }
            } catch (e: Exception) {
                Log.e(TAG, "OCR failed for image ${index + 1}", e)
                lastError = e.message ?: e.javaClass.simpleName
            }
        }
        val result = sb.toString().trim()
        if (result.isBlank() && lastError != null) {
            throw IllegalStateException(lastError)
        }
        result
    }

    /**
     * 将本地图片 URI 读取为 base64 字符串（无 data: 前缀）
     */
    private fun uriToBase64(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("无法读取图片: $uri")
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
