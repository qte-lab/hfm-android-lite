package com.chronie.homemoneylite.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

/**
 * ML Kit 端上中文 OCR 服务
 *
 * 使用 bundled 中文识别模型（离线、毫秒~秒级），将账单/小票图片转成纯文本，
 * 再交给本地 LLM（qwen3.5:2b）做结构化解析。
 *
 * 识别策略（两级）：
 * 1. 直接用原图识别（InputImage.fromFilePath 自带 EXIF 旋转处理）
 * 2. 若结果为空——常见于被裁剪/压缩后文字过小的图片——将图片放大到
 *    短边约 [TARGET_SHORT_SIDE] 像素后重试。ML Kit 对文字高度有下限要求
 *    （约 16~24px），放大能显著提升小图识别率。
 */
@Singleton
class MlKitOcrService @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "MlKitOcrService"
        /** 放大重试的目标短边像素 */
        private const val TARGET_SHORT_SIDE = 1600
        /** 放大后的最长边上限，防止 OOM */
        private const val MAX_LONG_SIDE = 4096
    }

    // 中文识别器同时支持中文+拉丁字符（金额、日期数字均可识别）
    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    /**
     * 识别单张图片，返回按行拼接的文本。
     * 识别引擎报错（如原生库缺失、模型初始化失败）时抛出异常暴露真实原因；
     * 引擎正常但确实没有文字时返回空字符串。
     */
    suspend fun recognize(uri: Uri): String {
        // 第一级：原图直接识别
        var engineError: Exception? = null
        val firstPass = try {
            runRecognition(InputImage.fromFilePath(context, uri))
        } catch (e: Exception) {
            Log.w(TAG, "First-pass OCR failed for $uri, will retry with upscaled bitmap", e)
            engineError = e
            ""
        }
        if (firstPass.isNotBlank()) {
            Log.d(TAG, "OCR first pass success: ${firstPass.length} chars")
            return firstPass
        }

        // 第二级：图片可能被压缩/裁剪导致文字过小，放大后重试
        val upscaled = withContext(Dispatchers.IO) { loadUpscaledBitmap(uri) }
        if (upscaled == null) {
            // 无法放大重试：若第一级是引擎错误则抛出，让上层看到真实原因
            engineError?.let { throw it }
            return firstPass
        }
        return try {
            val secondPass = runRecognition(InputImage.fromBitmap(upscaled, 0))
            Log.d(TAG, "OCR second pass (upscaled ${upscaled.width}x${upscaled.height}): ${secondPass.length} chars")
            secondPass
        } catch (e: Exception) {
            Log.e(TAG, "Second-pass OCR failed", e)
            // 两级都因引擎错误失败——抛出，不再静默吞掉
            throw engineError ?: e
        } finally {
            upscaled.recycle()
        }
    }

    /**
     * 批量识别多张图片，带序号标记拼接为一段文本。
     * 若所有图片都识别失败且存在引擎错误，抛出最后一个错误（暴露根因）。
     */
    suspend fun recognizeAll(uris: List<Uri>): String {
        val sb = StringBuilder()
        var lastError: Exception? = null
        uris.forEachIndexed { index, uri ->
            val text = try {
                recognize(uri)
            } catch (e: Exception) {
                Log.w(TAG, "OCR failed for image ${index + 1}", e)
                lastError = e
                ""
            }
            if (text.isNotBlank()) {
                if (uris.size > 1) {
                    sb.append("【图片${index + 1}】\n")
                }
                sb.append(text.trim()).append("\n\n")
            }
        }
        val result = sb.toString().trim()
        if (result.isBlank() && lastError != null) {
            throw lastError
        }
        return result
    }

    /**
     * 执行一次 ML Kit 识别，按 textBlock/line 顺序拼接文本
     */
    private suspend fun runRecognition(image: InputImage): String {
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // 保留行结构便于 LLM 理解表格式小票
                    val text = visionText.textBlocks.joinToString("\n") { block ->
                        block.lines.joinToString("\n") { it.text }
                    }
                    cont.resume(text)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }

    /**
     * 解码图片并放大到目标尺寸（短边 ~TARGET_SHORT_SIDE），同时按 EXIF 方向旋转。
     * 返回 null 表示解码失败或图片本身已足够大（无需放大重试）。
     */
    private fun loadUpscaledBitmap(uri: Uri): Bitmap? {
        return try {
            // 读取原始尺寸
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW <= 0 || srcH <= 0) return null

            // 读取 EXIF 旋转角
            val rotation = context.contentResolver.openInputStream(uri)?.use { input ->
                when (ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            // 计算放大倍数：短边放大到 TARGET_SHORT_SIDE，且不超过最长边上限
            val shortSide = min(srcW, srcH)
            val longSide = max(srcW, srcH)
            var scale = TARGET_SHORT_SIDE.toFloat() / shortSide
            scale = min(scale, MAX_LONG_SIDE.toFloat() / longSide)
            // 图片已够大就不再放大（略放大 1.5x 以内没有收益）
            if (scale <= 1.2f) scale = if (shortSide < TARGET_SHORT_SIDE) 1.5f else return null

            val source = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return null

            val matrix = Matrix().apply {
                postScale(scale, scale)
                if (rotation != 0f) postRotate(rotation)
            }
            val result = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            if (result != source) source.recycle()
            result
        } catch (e: Exception) {
            Log.e(TAG, "loadUpscaledBitmap failed", e)
            null
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "loadUpscaledBitmap OOM", oom)
            null
        }
    }
}
