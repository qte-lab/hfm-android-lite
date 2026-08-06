package com.chronie.homemoneylite.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.NumberPicker
import java.time.LocalDate
import java.time.YearMonth

/**
 * 自定义滚轮日期选择器（年 / 月 / 日 三个 NumberPicker 组合）。
 * 不依赖系统 DatePickerDialog，统一为滚轮交互；自动按所选年月校正当月天数，
 * 并把选中的日期约束在 [minDate, maxDate] 区间内。
 *
 * 注意：NumberPicker 设置 minValue 时要求当前 value >= minValue（默认 value=0 会抛异常），
 * 因此统一采用「先设 maxValue → 再设（受约束的）value → 最后设 minValue」的顺序。
 */
class WheelDatePicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val yearPicker: NumberPicker
    private val monthPicker: NumberPicker
    private val dayPicker: NumberPicker

    private var minDate: LocalDate = LocalDate.of(2000, 1, 1)
    private var maxDate: LocalDate = LocalDate.now()

    private var onDateChanged: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        val pad = (8 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)

        val lp = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        yearPicker = NumberPicker(context).apply {
            descendantFocusability = FOCUS_BLOCK_DESCENDANTS
            layoutParams = lp
        }
        monthPicker = NumberPicker(context).apply {
            descendantFocusability = FOCUS_BLOCK_DESCENDANTS
            layoutParams = lp
        }
        dayPicker = NumberPicker(context).apply {
            descendantFocusability = FOCUS_BLOCK_DESCENDANTS
            layoutParams = lp
        }

        yearPicker.setOnValueChangedListener { _, _, _ ->
            updateDayRange()
            clampToBounds()
            onDateChanged?.invoke()
        }
        monthPicker.setOnValueChangedListener { _, _, _ ->
            updateDayRange()
            clampToBounds()
            onDateChanged?.invoke()
        }
        dayPicker.setOnValueChangedListener { _, _, _ ->
            clampToBounds()
            onDateChanged?.invoke()
        }

        addView(yearPicker)
        addView(monthPicker)
        addView(dayPicker)

        applyRange()
    }

    fun setMinDate(date: LocalDate) {
        minDate = date
        applyRange()
    }

    fun setMaxDate(date: LocalDate) {
        maxDate = date
        applyRange()
    }

    fun setOnDateChangedListener(listener: (() -> Unit)?) {
        onDateChanged = listener
    }

    fun setDate(date: LocalDate) {
        var d = date
        if (d.isBefore(minDate)) d = minDate
        if (d.isAfter(maxDate)) d = maxDate
        yearPicker.maxValue = maxDate.year
        yearPicker.value = d.year.coerceIn(minDate.year, maxDate.year)
        yearPicker.minValue = minDate.year

        monthPicker.maxValue = 12
        monthPicker.value = d.monthValue.coerceIn(1, 12)
        monthPicker.minValue = 1

        updateDayRange()
        dayPicker.value = d.dayOfMonth.coerceIn(dayPicker.minValue, dayPicker.maxValue)
    }

    fun getDate(): LocalDate = runCatching {
        LocalDate.of(yearPicker.value, monthPicker.value, dayPicker.value)
    }.getOrDefault(minDate)

    private fun applyRange() {
        yearPicker.maxValue = maxDate.year
        yearPicker.value = yearPicker.value.coerceIn(minDate.year, maxDate.year)
        yearPicker.minValue = minDate.year

        monthPicker.maxValue = 12
        monthPicker.value = monthPicker.value.coerceIn(1, 12)
        monthPicker.minValue = 1
        monthPicker.displayedValues = (1..12).map { "${it}月" }.toTypedArray()

        updateDayRange()

        val cur = getDate()
        if (cur.isBefore(minDate)) setDate(minDate)
        else if (cur.isAfter(maxDate)) setDate(maxDate)
    }

    private fun updateDayRange() {
        val len = runCatching {
            YearMonth.of(yearPicker.value, monthPicker.value).lengthOfMonth()
        }.getOrDefault(28)
        dayPicker.maxValue = len
        dayPicker.value = dayPicker.value.coerceIn(1, len)
        dayPicker.minValue = 1
    }

    private fun clampToBounds() {
        val cur = getDate()
        if (cur.isAfter(maxDate)) {
            yearPicker.maxValue = maxDate.year
            yearPicker.value = maxDate.year
            yearPicker.minValue = minDate.year
            monthPicker.maxValue = 12
            monthPicker.value = maxDate.monthValue
            monthPicker.minValue = 1
            updateDayRange()
            dayPicker.value = maxDate.dayOfMonth
        } else if (cur.isBefore(minDate)) {
            yearPicker.maxValue = maxDate.year
            yearPicker.value = minDate.year
            yearPicker.minValue = minDate.year
            monthPicker.maxValue = 12
            monthPicker.value = minDate.monthValue
            monthPicker.minValue = 1
            updateDayRange()
            dayPicker.value = minDate.dayOfMonth
        }
    }
}
