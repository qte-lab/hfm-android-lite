package com.chronie.homemoneylite.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chronie.homemoneylite.ui.theme.onSurfaceVariant
import com.chronie.homemoneylite.ui.theme.titleMedium
import com.chronie.homemoneylite.ui.theme.bodyMedium
import java.time.LocalDate

/**
 * 通用滚轮选择器（兼容 Compose 1.1.0，手动实现停止吸附）。
 *
 * @param items 选项文本列表
 * @param selectedIndex 当前选中项下标（受控）
 * @param onSelectedIndexChange 滚动停止吸附后回调新的下标
 */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
    visibleCount: Int = 5
) {
    val safeSelected = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeSelected)
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val paddingCount = visibleCount / 2

    // 当前处于中心位置的下标（滚动中实时变化）
    val centerIndex by remember(items.size) {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            (listState.firstVisibleItemIndex + if (offset > itemHeightPx / 2f) 1 else 0)
                .coerceIn(0, (items.size - 1).coerceAtLeast(0))
        }
    }

    // 外部选中项变化（或选项数变化）时同步滚动位置
    LaunchedEffect(safeSelected, items.size) {
        if (!listState.isScrollInProgress && centerIndex != safeSelected) {
            listState.scrollToItem(safeSelected)
        }
    }

    // 滚动停止后吸附到最近一行并回调
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && items.isNotEmpty()) {
            val target = centerIndex
            if (listState.firstVisibleItemScrollOffset != 0) {
                listState.animateScrollToItem(target)
            }
            if (target != safeSelected) {
                onSelectedIndexChange(target)
            }
        }
    }

    Box(modifier = modifier.height(itemHeight * visibleCount)) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = itemHeight * paddingCount),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, label ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    val isCenter = index == centerIndex
                    Text(
                        text = label,
                        style = if (isCenter) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCenter) MaterialTheme.colors.primary
                        else MaterialTheme.colors.onSurfaceVariant
                    )
                }
            }
        }

        // 中心行指示线
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
        ) {
            Divider(color = MaterialTheme.colors.primary.copy(alpha = 0.35f))
            Box(modifier = Modifier.weight(1f))
            Divider(color = MaterialTheme.colors.primary.copy(alpha = 0.35f))
        }
    }
}

/**
 * 年月日滚轮日期选择器（xxxx年 xx月 xx日 一行三列）。
 *
 * @param date 当前选中日期（受控）
 * @param onDateChange 日期变化回调（自动处理月末天数钳制）
 */
@Composable
fun WheelDatePicker(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    minYear: Int = 1970,
    maxYear: Int = 2100
) {
    val years = remember(minYear, maxYear) { (minYear..maxYear).map { "${it}年" } }
    val months = remember { (1..12).map { "${it}月" } }
    val dayCount = date.lengthOfMonth()
    val days = remember(dayCount) { (1..dayCount).map { "${it}日" } }

    Row(modifier = modifier.fillMaxWidth()) {
        WheelPicker(
            items = years,
            selectedIndex = date.year - minYear,
            onSelectedIndexChange = { index ->
                val newYear = minYear + index
                val maxDay = LocalDate.of(newYear, date.monthValue, 1).lengthOfMonth()
                onDateChange(LocalDate.of(newYear, date.monthValue, date.dayOfMonth.coerceAtMost(maxDay)))
            },
            modifier = Modifier.weight(1.2f)
        )
        WheelPicker(
            items = months,
            selectedIndex = date.monthValue - 1,
            onSelectedIndexChange = { index ->
                val newMonth = index + 1
                val maxDay = LocalDate.of(date.year, newMonth, 1).lengthOfMonth()
                onDateChange(LocalDate.of(date.year, newMonth, date.dayOfMonth.coerceAtMost(maxDay)))
            },
            modifier = Modifier.weight(1f)
        )
        WheelPicker(
            items = days,
            selectedIndex = (date.dayOfMonth - 1).coerceAtMost(dayCount - 1),
            onSelectedIndexChange = { index ->
                onDateChange(LocalDate.of(date.year, date.monthValue, index + 1))
            },
            modifier = Modifier.weight(1f)
        )
    }
}
