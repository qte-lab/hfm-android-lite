package com.chronie.homemoney.data.mapper

import com.chronie.homemoney.data.remote.dto.AIExpenseRecordDto
import com.chronie.homemoney.domain.model.AIExpenseRecord
import com.chronie.homemoney.domain.model.ExpenseType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * AI 记录数据映射器
 */
object AIRecordMapper {
    
    /**
     * DTO -> Domain Model
     */
    fun toDomain(dto: AIExpenseRecordDto): AIExpenseRecord {
        return AIExpenseRecord(
            type = parseExpenseType(dto.type),
            amount = dto.amount,
            date = dto.date, // 直接使用日期字符串
            remark = dto.remark,
            isEdited = false,
            isValid = validateRecord(dto)
        )
    }
    
    /**
     * 解析支出类型
     */
    private fun parseExpenseType(typeStr: String): ExpenseType {
        return when (typeStr) {
            "日常用品" -> ExpenseType.DAILY_GOODS
            "奢侈品" -> ExpenseType.LUXURY
            "通讯费用" -> ExpenseType.COMMUNICATION
            "食品" -> ExpenseType.FOOD
            "零食糖果" -> ExpenseType.SNACKS
            "冷饮" -> ExpenseType.COLD_DRINKS
            "方便食品" -> ExpenseType.CONVENIENCE_FOOD
            "纺织品" -> ExpenseType.TEXTILES
            "饮品" -> ExpenseType.BEVERAGES
            "调味品" -> ExpenseType.CONDIMENTS
            "交通出行" -> ExpenseType.TRANSPORTATION
            "餐饮" -> ExpenseType.DINING
            "医疗费用" -> ExpenseType.MEDICAL
            "水果" -> ExpenseType.FRUITS
            "水产品" -> ExpenseType.SEAFOOD
            "乳制品" -> ExpenseType.DAIRY
            "礼物人情" -> ExpenseType.GIFTS
            "旅行度假" -> ExpenseType.TRAVEL
            "政务" -> ExpenseType.GOVERNMENT
            "水电煤气" -> ExpenseType.UTILITIES
            else -> ExpenseType.OTHER
        }
    }
    
    /**
     * 解析日期时间
     */
    private fun parseDateTime(dateStr: String): LocalDateTime {
        return try {
            // 尝试解析 ISO 格式
            LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            try {
                // 尝试解析日期格式
                val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE)
                date.atStartOfDay()
            } catch (e2: Exception) {
                // 默认使用当前时间
                LocalDateTime.now()
            }
        }
    }
    
    /**
     * 验证记录有效性
     */
    private fun validateRecord(dto: AIExpenseRecordDto): Boolean {
        return dto.amount > 0 && dto.remark.isNotBlank()
    }
}
