package com.chronie.homemoneylite.ui.components

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chronie.homemoneylite.R
import android.app.AlertDialog
import java.time.LocalDate

/**
 * 单个日期选择弹窗（自定义滚轮）。用于新增/编辑支出的日期选择。
 */
fun showWheelDatePicker(
    context: Context,
    initial: LocalDate,
    minDate: LocalDate = LocalDate.of(2000, 1, 1),
    maxDate: LocalDate = LocalDate.now(),
    onConfirm: (LocalDate) -> Unit
) {
    val density = context.resources.displayMetrics.density
    val picker = WheelDatePicker(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setMinDate(minDate)
        setMaxDate(maxDate)
        setDate(initial)
    }
    AlertDialog.Builder(context)
        .setTitle(R.string.select_date)
        .setView(picker)
        .setPositiveButton(R.string.confirm) { _, _ -> onConfirm(picker.getDate()) }
        .setNegativeButton(R.string.cancel, null)
        .show()
}

/**
 * 时间范围选择弹窗（开始 + 结束 两个滚轮，单个对话框内完成）。
 * 用于图表自定义时间范围，避免连续弹出两个系统日期选择器。
 */
fun showWheelDateRangePicker(
    context: Context,
    initialStart: LocalDate,
    initialEnd: LocalDate,
    minDate: LocalDate = LocalDate.of(2000, 1, 1),
    maxDate: LocalDate = LocalDate.now(),
    onConfirm: (start: LocalDate, end: LocalDate) -> Unit
) {
    val density = context.resources.displayMetrics.density
    val pad = (16 * density).toInt()

    val startPicker = WheelDatePicker(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setMinDate(minDate)
        setMaxDate(maxDate)
        setDate(initialStart)
    }
    val endPicker = WheelDatePicker(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setMinDate(minDate)
        setMaxDate(maxDate)
        setDate(initialEnd)
    }

    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(pad, pad, pad, pad)

        addView(TextView(context).apply {
            text = context.getString(R.string.export_start_date)
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        })
        addView(startPicker)

        addView(TextView(context).apply {
            text = context.getString(R.string.export_end_date)
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(0, (16 * density).toInt(), 0, 0)
        })
        addView(endPicker)
    }

    AlertDialog.Builder(context)
        .setTitle(R.string.custom_range)
        .setView(container)
        .setPositiveButton(R.string.confirm) { _, _ ->
            var s = startPicker.getDate()
            var e = endPicker.getDate()
            if (e.isBefore(s)) e = s
            onConfirm(s, e)
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
}
