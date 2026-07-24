package com.chronie.homemoneylite.data.mapper

import android.util.Base64
import com.chronie.homemoneylite.data.local.entity.ExpenseEntity
import com.chronie.homemoneylite.data.remote.dto.ExpenseDto
import com.chronie.homemoneylite.domain.model.Expense
import com.chronie.homemoneylite.domain.model.ExpenseType
import java.nio.charset.StandardCharsets
import java.util.UUID

object ExpenseMapper {
    
    fun toDomain(entity: ExpenseEntity): Expense {
        return Expense(
            id = entity.id,
            type = ExpenseType.fromString(entity.type),
            remark = decodeRemarkIfBase64(entity.remark),
            amount = entity.amount,
            date = entity.date,
            version = entity.version,
            updatedAt = entity.updatedAt,
            deletedAt = entity.deletedAt,
            isSynced = entity.isSynced
        )
    }
    
    fun toEntity(expense: Expense): ExpenseEntity {
        return ExpenseEntity(
            id = expense.id,
            type = getChineseTypeName(expense.type),
            remark = expense.remark,
            amount = expense.amount,
            date = expense.date,
            version = expense.version,
            updatedAt = expense.updatedAt,
            deletedAt = expense.deletedAt,
            isSynced = expense.isSynced
        )
    }
    
    fun toDomain(dto: ExpenseDto): Expense {
        val dateStr = try {
            if (dto.date.contains('T') || dto.date.contains(' ')) {
                val datePart = dto.date.substringBefore('T').substringBefore(' ')
                java.time.LocalDate.parse(datePart)
                datePart
            } else {
                java.time.LocalDate.parse(dto.date)
                dto.date
            }
        } catch (e: Exception) {
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }
        
        return Expense(
            id = dto.id ?: UUID.randomUUID().toString(),
            type = ExpenseType.fromString(dto.type),
            remark = dto.remark,
            amount = dto.amount,
            date = dateStr,
            version = dto.version,
            updatedAt = dto.updatedAt,
            deletedAt = dto.deletedAt,
            isSynced = true
        )
    }
    
    fun toDto(expense: Expense): ExpenseDto {
        return ExpenseDto(
            id = expense.id,
            type = getChineseTypeName(expense.type),
            remark = expense.remark,
            amount = expense.amount,
            date = expense.date,
            version = expense.version,
            updatedAt = expense.updatedAt,
            deletedAt = expense.deletedAt
        )
    }
    
    private fun getChineseTypeName(type: ExpenseType): String {
        return when (type) {
            ExpenseType.DAILY_GOODS -> "日常用品"
            ExpenseType.LUXURY -> "奢侈品"
            ExpenseType.COMMUNICATION -> "通讯费用"
            ExpenseType.FOOD -> "食品"
            ExpenseType.SNACKS -> "零食糖果"
            ExpenseType.COLD_DRINKS -> "冷饮"
            ExpenseType.CONVENIENCE_FOOD -> "方便食品"
            ExpenseType.TEXTILES -> "纺织品"
            ExpenseType.BEVERAGES -> "饮品"
            ExpenseType.CONDIMENTS -> "调味品"
            ExpenseType.TRANSPORTATION -> "交通出行"
            ExpenseType.DINING -> "餐饮"
            ExpenseType.MEDICAL -> "医疗费用"
            ExpenseType.FRUITS -> "水果"
            ExpenseType.OTHER -> "其他"
            ExpenseType.SEAFOOD -> "水产品"
            ExpenseType.DAIRY -> "乳制品"
            ExpenseType.GIFTS -> "礼物人情"
            ExpenseType.TRAVEL -> "旅行度假"
            ExpenseType.GOVERNMENT -> "政务"
            ExpenseType.UTILITIES -> "水电煤气"
            ExpenseType.BEAUTY -> "美容美发"
            ExpenseType.BEAN_PRODUCTS -> "豆制品"
            ExpenseType.COSMETICS -> "个护美妆"
            ExpenseType.ELECTRONICS -> "电子产品"
            ExpenseType.HOUSEHOLD_APPLIANCES -> "家用电器"
            ExpenseType.HARDWARE -> "五金"
            ExpenseType.CLOTHING -> "服装"
        }
    }

    /**
     * 备注在（迁移前的）历史数据中以 Base64 形式落库，旧 Compose 版在读取时解码；
     * 迁移到 XML 后这层“读取即解码”丢失，导致离线从数据库读出的备注显示为原始 Base64。
     *
     * 这里在唯一的本地读取关口还原：仅当字符串为合法 Base64 且能解码为有效 UTF-8 文本时才解码；
     * 否则（明文、或 Base64-of-binary 等已损坏数据）原样返回，绝不抛异常、不破坏数据。
     */
    private fun decodeRemarkIfBase64(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val candidate = raw.trim()
        if (!isBase64(candidate)) return raw
        return try {
            val decoded = Base64.decode(candidate, Base64.NO_WRAP)
            val text = String(decoded, StandardCharsets.UTF_8)
            // 校验确为可读文本：把解码结果重新编码回 UTF-8 应与原始字节一致（无替换字符）
            val roundTrip = text.toByteArray(StandardCharsets.UTF_8)
            if (roundTrip.contentEquals(decoded) && text.isNotBlank()) text else raw
        } catch (e: Exception) {
            raw
        }
    }

    private fun isBase64(s: String): Boolean {
        if (s.length % 4 != 0) return false
        return s.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }
    }
}
