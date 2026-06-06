package com.chronie.homemoney.domain.repository

import android.net.Uri
import com.chronie.homemoney.domain.model.AIExpenseRecord

/**
 * AI 记录仓库接口
 */
interface AIRecordRepository {
    
    /**
     * 解析文本为支出记录
     */
    suspend fun parseTextToRecords(text: String): Result<List<AIExpenseRecord>>
    
    /**
     * 解析图片为支出记录
     */
    suspend fun parseImagesToRecords(imageUris: List<Uri>): Result<List<AIExpenseRecord>>
    
    /**
     * 批量保存 AI 识别的记录
     */
    suspend fun saveRecords(records: List<AIExpenseRecord>): Result<Unit>
}
