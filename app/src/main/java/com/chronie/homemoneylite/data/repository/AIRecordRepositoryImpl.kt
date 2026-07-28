package com.chronie.homemoneylite.data.repository

import android.net.Uri
import android.util.Log
import com.chronie.homemoneylite.data.local.dao.ExpenseDao
import com.chronie.homemoneylite.data.local.dao.SyncQueueDao
import com.chronie.homemoneylite.data.local.entity.ExpenseEntity
import com.chronie.homemoneylite.data.local.entity.SyncQueueEntity
import com.chronie.homemoneylite.data.mapper.AIRecordMapper
import com.chronie.homemoneylite.data.mapper.ExpenseMapper
import com.chronie.homemoneylite.data.ocr.MlKitOcrService
import com.chronie.homemoneylite.data.remote.api.AIRecordApi
import com.chronie.homemoneylite.data.remote.dto.*
import com.chronie.homemoneylite.domain.model.AIExpenseRecord
import com.chronie.homemoneylite.domain.repository.AIRecordRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 记录仓库实现
 * - 图片识别：先用 ML Kit 端上 OCR 提取文字（毫秒级），再交给本地 LLM 做结构化解析，
 *   避免把 base64 大图传给视觉模型导致长时间等待
 * - 已集成钱包扣费系统：每次识别前检查余额，成功后扣 0.1 元
 */
@Singleton
class AIRecordRepositoryImpl @Inject constructor(
    private val aiRecordApi: AIRecordApi,
    private val expenseDao: ExpenseDao,
    private val syncQueueDao: SyncQueueDao,
    private val walletRepository: WalletRepository,
    private val ocrService: MlKitOcrService,
    private val gson: Gson
) : AIRecordRepository {
    
    companion object {
        private const val TAG = "AIRecordRepository"
        /** Ollama 本地模型名（文本与多模态共用，qwen3.5:2b 支持视觉识别） */
        private const val MODEL_NAME = "qwen3.5:2b"
    }
    
    override suspend fun parseTextToRecords(text: String): Result<List<AIExpenseRecord>> {
        return try {
            // ===== 钱包检查：余额不足或被封禁则拒绝 =====
            val (canProceed, currentBalance, walletError) = walletRepository.canRecognize()
            if (!canProceed) {
                Log.w(TAG, "Wallet check failed: $walletError")
                return Result.failure(Exception(walletError))
            }
            Log.d(TAG, "Wallet check passed. Balance: ¥$currentBalance")

            Log.d(TAG, "Parsing text to records")
            
            val prompt = buildTextPrompt(text)
            val request = AIRecordRequest(
                model = MODEL_NAME,
                messages = listOf(
                    AIMessage(
                        role = "system",
                        content = "你是一个智能消费记录解析助手，能够从文本中提取消费信息并格式化输出。"
                    ),
                    AIMessage(
                        role = "user",
                        content = prompt
                    )
                ),
                stream = false,
                think = false
            )
            
            val response = aiRecordApi.parseRecord(request)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                val errorMessage = when (response.code()) {
                    400 -> "请求参数错误 (400): ${errorBody ?: "请检查输入内容"}"
                    401 -> "API密钥无效或已过期 (401): ${errorBody ?: "请检查API Key设置"}"
                    403 -> "请求被拒绝 (403): ${errorBody ?: "可能没有权限访问该模型"}"
                    404 -> "API端点不存在 (404): ${errorBody ?: "请检查API地址配置"}"
                    429 -> "请求过于频繁 (429): ${errorBody ?: "请稍后再试"}"
                    500 -> "服务器内部错误 (500): ${errorBody ?: "AI服务暂时不可用"}"
                    502 -> "网关错误 (502): ${errorBody ?: "服务器维护中"}"
                    503 -> "服务不可用 (503): ${errorBody ?: "服务器过载或维护中"}"
                    else -> "HTTP错误 (${response.code()}): ${errorBody ?: "未知错误"}"
                }
                Log.e(TAG, "API request failed: $errorMessage")
                throw Exception(errorMessage)
            }
            
            val content = response.body()?.message?.content
                ?: throw Exception("Empty response from AI")
            
            val records = parseAIResponse(content)
            Log.d(TAG, "Parsed ${records.size} records from text")

            // ===== 扣费：识别成功后扣除费用 =====
            val newBalance = walletRepository.deductRecognitionFee()
            Log.d(TAG, "Fee deducted. New balance: ¥$newBalance")

            Result.success(records)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse text", e)
            Result.failure(e)
        }
    }
    
    override suspend fun parseImagesToRecords(imageUris: List<Uri>): Result<List<AIExpenseRecord>> {
        return try {
            // ===== 钱包检查：余额不足或被封禁则拒绝 =====
            val (canProceed, currentBalance, walletError) = walletRepository.canRecognize()
            if (!canProceed) {
                Log.w(TAG, "Wallet check failed: $walletError")
                return Result.failure(Exception(walletError))
            }
            Log.d(TAG, "Wallet check passed. Balance: ¥$currentBalance")

            Log.d(TAG, "Parsing ${imageUris.size} images to records (ML Kit OCR + LLM)")

            // ===== 第一步：端上 OCR 提取文字（毫秒级，无需上传图片）=====
            val ocrText = ocrService.recognizeAll(imageUris)
            if (ocrText.isBlank()) {
                return Result.failure(Exception("未能从图片中识别出文字，请确保图片清晰且包含消费信息"))
            }
            Log.d(TAG, "OCR extracted ${ocrText.length} chars")

            // ===== 第二步：OCR 文本交给本地 LLM 做结构化解析 =====
            val prompt = buildOcrTextPrompt(ocrText)
            val request = AIRecordRequest(
                model = MODEL_NAME,
                messages = listOf(
                    AIMessage(
                        role = "system",
                        content = "你是一个智能消费记录解析助手，能够从票据OCR文本中提取消费信息并格式化输出。"
                    ),
                    AIMessage(
                        role = "user",
                        content = prompt
                    )
                ),
                stream = false,
                think = false
            )
            
            val response = aiRecordApi.parseRecord(request)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                val errorMessage = when (response.code()) {
                    400 -> "请求参数错误 (400): ${errorBody ?: "请检查输入内容"}"
                    401 -> "API密钥无效或已过期 (401): ${errorBody ?: "请检查API Key设置"}"
                    403 -> "请求被拒绝 (403): ${errorBody ?: "可能没有权限访问该模型"}"
                    404 -> "API端点不存在 (404): ${errorBody ?: "请检查API地址配置"}"
                    429 -> "请求过于频繁 (429): ${errorBody ?: "请稍后再试"}"
                    500 -> "服务器内部错误 (500): ${errorBody ?: "AI服务暂时不可用"}"
                    502 -> "网关错误 (502): ${errorBody ?: "服务器维护中"}"
                    503 -> "服务不可用 (503): ${errorBody ?: "服务器过载或维护中"}"
                    else -> "HTTP错误 (${response.code()}): ${errorBody ?: "未知错误"}"
                }
                Log.e(TAG, "API request failed: $errorMessage")
                throw Exception(errorMessage)
            }

            val content = response.body()?.message?.content
                ?: throw Exception("Empty response from AI")
            
            val records = parseAIResponse(content)
            Log.d(TAG, "Parsed ${records.size} records from images")

            // ===== 扣费：识别成功后扣除费用 =====
            val newBalance = walletRepository.deductRecognitionFee()
            Log.d(TAG, "Fee deducted. New balance: ¥$newBalance")

            Result.success(records)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse images", e)
            Result.failure(e)
        }
    }
    
    override suspend fun saveRecords(records: List<AIExpenseRecord>): Result<Unit> {
        return try {
            Log.d(TAG, "Saving ${records.size} AI records")
            
            val validRecords = records.filter { it.isValid }
            if (validRecords.isEmpty()) {
                Log.d(TAG, "No valid records to save")
                return Result.success(Unit)
            }
            
            val expenses = validRecords.map { aiRecord ->
                val uuid = java.util.UUID.randomUUID().toString()
                aiRecord.copy(id = uuid).toExpense()
            }
            
            val entities = expenses.map { ExpenseMapper.toEntity(it).copy(isSynced = false) }
            expenseDao.insertExpenses(entities)
            
            entities.forEach { entity ->
                addToSyncQueue("expense", entity.id, "CREATE", entity)
            }
            
            Log.d(TAG, "Successfully saved all ${validRecords.size} records")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save records", e)
            Result.failure(e)
        }
    }
    
    /**
     * 构建文本解析提示
     */
    private fun buildTextPrompt(text: String): String {
        val today = java.time.LocalDate.now()
        val dayOfWeek = today.dayOfWeek.getDisplayName(
            java.time.format.TextStyle.FULL,
            java.util.Locale.SIMPLIFIED_CHINESE
        )
        val dateStr = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        
        return """
今天是 $dateStr，星期$dayOfWeek。

请分析以下文本，提取其中的所有消费信息。如果有多个消费记录，请以JSON数组的形式输出。
每个记录应包含：
{
  "type": "消费类型", // 从预定义列表中选择：日常用品、奢侈品、通讯费用、食品、零食糖果、冷饮、方便食品、纺织品、饮品、调味品、交通出行、餐饮、医疗费用、水果、其他、水产品、乳制品、礼物人情、旅行度假、政务、水电煤气、美容美发、豆制品、个护美妆、电子产品、家用电器、五金、服装
  "amount": 金额, // 数字类型
  "date": "日期", // 日期格式 YYYY-MM-DD
  "remark": "备注" // 详细说明，注意：此处必须包含消费物品/服务的名称
}

请注意：
1. 如果文本中有多个消费记录，请返回JSON数组格式
2. 如果只有一个消费记录，请返回单个JSON对象或只有一个元素的数组
3. 如果文本中没有明确的消费类型，请根据内容选择最合适的预定义类型
4. 如果没有明确的日期，请使用今天日期（$dateStr）
5. 只返回JSON数据，不要添加其他无关内容，不要使用markdown代码块

文本内容：$text
        """.trimIndent()
    }
    
    /**
     * 构建 OCR 文本解析提示（图片经 ML Kit OCR 后的文本）
     */
    private fun buildOcrTextPrompt(ocrText: String): String {
        val today = java.time.LocalDate.now()
        val dayOfWeek = today.dayOfWeek.getDisplayName(
            java.time.format.TextStyle.FULL,
            java.util.Locale.SIMPLIFIED_CHINESE
        )
        val dateStr = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        
        return """
今天是 $dateStr，星期$dayOfWeek。

以下是从消费票据/账单截图中通过OCR识别出的文本（可能存在个别字符识别错误或换行混乱，请自行纠正理解）。
请从中提取所有消费记录。如果有多个消费记录，请以JSON数组的形式输出。
每个记录应包含：
{
  "type": "消费类型", // 从预定义列表中选择：日常用品、奢侈品、通讯费用、食品、零食糖果、冷饮、方便食品、纺织品、饮品、调味品、交通出行、餐饮、医疗费用、水果、其他、水产品、乳制品、礼物人情、旅行度假、政务、水电煤气、美容美发、豆制品、个护美妆、电子产品、家用电器、五金、服装
  "amount": 金额, // 数字类型
  "date": "日期", // 日期格式 YYYY-MM-DD
  "remark": "备注" // 详细说明，注意：此处必须包含消费物品/服务的名称
}

请注意：
1. 如果文本中有多个消费记录，请返回JSON数组格式
2. 如果只有一个消费记录，请返回单个JSON对象或只有一个元素的数组
3. 如果没有明确的消费类型，请根据内容选择最合适的预定义类型
4. 如果没有明确的日期，请使用今天日期（$dateStr）
5. 忽略优惠券、积分、广告等与实际支付无关的内容，金额优先取"实付/合计"
6. 只返回JSON数据，不要添加其他无关内容，不要使用markdown代码块

OCR文本内容：
$ocrText
        """.trimIndent()
    }
    
    /**
     * 解析 AI 响应
     */
    private fun parseAIResponse(content: String): List<AIExpenseRecord> {
        return try {
            // 清理响应内容：剥离可能内联的 <think>...</think> 推理段，再移除 markdown 代码块标记
            val cleanContent = content
                .replace(Regex("(?s)<think>.*?</think>"), "")
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            // 尝试解析为数组
            val listType = object : TypeToken<List<AIExpenseRecordDto>>() {}.type
            val dtoList: List<AIExpenseRecordDto> = try {
                gson.fromJson(cleanContent, listType)
            } catch (e: Exception) {
                // 如果解析数组失败，尝试解析单个对象
                val singleDto = gson.fromJson(cleanContent, AIExpenseRecordDto::class.java)
                listOf(singleDto)
            }
            
            dtoList.map { AIRecordMapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI response", e)
            emptyList()
        }
    }
    
    /**
     * 添加到同步队列
     */
    private suspend fun addToSyncQueue(
        entityType: String,
        entityId: String,
        operation: String,
        data: Any
    ) {
        val dto = when (data) {
            is ExpenseEntity -> ExpenseMapper.toDto(ExpenseMapper.toDomain(data))
            else -> data
        }
        
        val jsonData = gson.toJson(dto)
        val syncItem = SyncQueueEntity(
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            data = jsonData
        )
        syncQueueDao.insertSyncItem(syncItem)
    }
}
