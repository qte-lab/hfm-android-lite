package com.chronie.homemoneylite.domain.repository

import com.chronie.homemoneylite.domain.model.AIExpenseRecord

/**
 * AI 记录仓库接口
 */
interface AIRecordRepository {
    
    /**
     * 解析文本为支出记录（OCR 已迁移到服务端，App 仅把识别出的文本交给 LLM）
     */
    suspend fun parseTextToRecords(text: String): Result<List<AIExpenseRecord>>
    
    /**
     * 批量保存 AI 识别的记录
     */
    suspend fun saveRecords(records: List<AIExpenseRecord>): Result<Unit>
}
