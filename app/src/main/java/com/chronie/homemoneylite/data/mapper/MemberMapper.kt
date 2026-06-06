package com.chronie.homemoneylite.data.mapper

import com.chronie.homemoneylite.data.remote.dto.MemberDto
import com.chronie.homemoneylite.domain.model.Member
import java.text.SimpleDateFormat
import java.util.*

object MemberMapper {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun toDomain(dto: MemberDto): Member {
        return Member(
            id = dto.id,
            username = dto.username,
            createdAt = parseDate(dto.createdAt),
            updatedAt = parseDate(dto.updatedAt),
            avatar = dto.avatar
        )
    }

    private fun parseDate(dateString: String): Long {
        return try {
            dateFormat.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
