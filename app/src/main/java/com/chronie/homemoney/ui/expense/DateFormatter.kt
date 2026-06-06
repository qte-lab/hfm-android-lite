package com.chronie.homemoney.ui.expense

import android.content.Context
import com.chronie.homemoney.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun formatDateShort(dateString: String, locale: String): String {
    try {
        val date = LocalDate.parse(dateString)
        return when (locale) {
            "zh", "zh-CN" -> {
                "${date.year}/${String.format("%02d", date.monthValue)}/${String.format("%02d", date.dayOfMonth)}"
            }
            "zh-HK", "zh-MO", "zh-SG", "zh-TW" -> {
                "${String.format("%02d", date.dayOfMonth)}/${String.format("%02d", date.monthValue)}/${date.year}"
            }
            "ja-JP" -> {
                "${date.year}/${String.format("%02d", date.monthValue)}/${String.format("%02d", date.dayOfMonth)}"
            }
            "ko-KR" -> {
                "${date.year}.${String.format("%02d", date.monthValue)}.${String.format("%02d", date.dayOfMonth)}"
            }
            "vi-VN" -> {
                "${String.format("%02d", date.dayOfMonth)}/${String.format("%02d", date.monthValue)}/${date.year}"
            }
            "th-TH" -> {
                "${date.dayOfMonth}-${String.format("%02d", date.monthValue)}-${(date.year + 543)}"
            }
            "id-ID" -> {
                "${date.dayOfMonth}/${String.format("%02d", date.monthValue)}/${date.year}"
            }
            "ms-MY" -> {
                "${date.dayOfMonth}/${String.format("%02d", date.monthValue)}/${date.year}"
            }
            "en-US" -> {
                "${String.format("%02d", date.monthValue)}/${String.format("%02d", date.dayOfMonth)}/${date.year}"
            }
            else -> {
                "${date.year}-${String.format("%02d", date.monthValue)}-${String.format("%02d", date.dayOfMonth)}"
            }
        }
    } catch (e: Exception) {
        return dateString
    }
}

fun formatRelativeDate(dateString: String, context: Context, locale: String? = null): String {
    try {
        val date = LocalDate.parse(dateString)
        val today = LocalDate.now()
        val daysBetween = ChronoUnit.DAYS.between(date, today)

        val dayOfWeek = date.dayOfWeek
        val weekdayString = when (dayOfWeek) {
            DayOfWeek.MONDAY -> context.getString(R.string.monday)
            DayOfWeek.TUESDAY -> context.getString(R.string.tuesday)
            DayOfWeek.WEDNESDAY -> context.getString(R.string.wednesday)
            DayOfWeek.THURSDAY -> context.getString(R.string.thursday)
            DayOfWeek.FRIDAY -> context.getString(R.string.friday)
            DayOfWeek.SATURDAY -> context.getString(R.string.saturday)
            DayOfWeek.SUNDAY -> context.getString(R.string.sunday)
        }

        val formattedDate = if (locale != null) formatDateShort(dateString, locale) else dateString

        return when {
            daysBetween == 0L -> "${context.getString(R.string.date_today)}（$weekdayString）"
            daysBetween == 1L -> "${context.getString(R.string.date_yesterday)}（$weekdayString）"
            daysBetween in 2..6 -> "${context.getString(R.string.date_days_ago, daysBetween)}（$weekdayString）"
            else -> "$formattedDate（$weekdayString）"
        }
    } catch (e: Exception) {
        return dateString
    }
}

fun formatDateByLocale(dateString: String, locale: String): String {
    try {
        val date = LocalDate.parse(dateString)

        return when (locale) {
            "en-US" -> {
                val monthNames = arrayOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
                )
                "${monthNames[date.monthValue - 1]} ${date.dayOfMonth}, ${date.year}"
            }
            "zh", "zh-CN", "zh-HK", "zh-MO", "zh-SG", "zh-TW" -> {
                "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
            }
            "ja-JP" -> {
                "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
            }
            "ko-KR" -> {
                "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일"
            }
            "vi-VN" -> {
                val monthNames = arrayOf(
                    "tháng 1", "tháng 2", "tháng 3", "tháng 4", "tháng 5", "tháng 6",
                    "tháng 7", "tháng 8", "tháng 9", "tháng 10", "tháng 11", "tháng 12"
                )
                "${date.dayOfMonth} ${monthNames[date.monthValue - 1]} năm ${date.year}"
            }
            "th-TH" -> {
                val monthNames = arrayOf(
                    "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
                    "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"
                )
                "${date.dayOfMonth} ${monthNames[date.monthValue - 1]} ${date.year + 543}"
            }
            "id-ID" -> {
                val monthNames = arrayOf(
                    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                )
                "${date.dayOfMonth} ${monthNames[date.monthValue - 1]} ${date.year}"
            }
            "ms-MY" -> {
                val monthNames = arrayOf(
                    "Januari", "Februari", "Mac", "April", "Mei", "Jun",
                    "Julai", "Ogos", "September", "Oktober", "November", "Disember"
                )
                "${date.dayOfMonth} ${monthNames[date.monthValue - 1]} ${date.year}"
            }
            else -> {
                "${date.year}-${String.format("%02d", date.monthValue)}-${String.format("%02d", date.dayOfMonth)}"
            }
        }
    } catch (e: Exception) {
        return dateString
    }
}

fun formatMonthLabelByLocale(dateString: String, locale: String): String {
    try {
        val date = LocalDate.parse(dateString)

        return when (locale) {
            "zh", "zh-CN", "zh-HK", "zh-MO", "zh-SG", "zh-TW" -> {
                "${date.year}年${date.monthValue}月"
            }
            "ja-JP" -> {
                "${date.year}年${date.monthValue}月"
            }
            "ko-KR" -> {
                "${date.year}년 ${date.monthValue}월"
            }
            "vi-VN" -> {
                val monthNames = arrayOf(
                    "tháng 1", "tháng 2", "tháng 3", "tháng 4", "tháng 5", "tháng 6",
                    "tháng 7", "tháng 8", "tháng 9", "tháng 10", "tháng 11", "tháng 12"
                )
                "${monthNames[date.monthValue - 1]} năm ${date.year}"
            }
            "th-TH" -> {
                val monthNames = arrayOf(
                    "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
                    "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"
                )
                "${monthNames[date.monthValue - 1]} ${date.year + 543}"
            }
            "id-ID" -> {
                val monthNames = arrayOf(
                    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                )
                "${monthNames[date.monthValue - 1]} ${date.year}"
            }
            "ms-MY" -> {
                val monthNames = arrayOf(
                    "Januari", "Februari", "Mac", "April", "Mei", "Jun",
                    "Julai", "Ogos", "September", "Oktober", "November", "Disember"
                )
                "${monthNames[date.monthValue - 1]} ${date.year}"
            }
            "en", "en-US" -> {
                val monthNames = arrayOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
                )
                "${monthNames[date.monthValue - 1]} ${date.year}"
            }
            else -> {
                "${date.year}-${String.format("%02d", date.monthValue)}"
            }
        }
    } catch (e: Exception) {
        return dateString
    }
}
