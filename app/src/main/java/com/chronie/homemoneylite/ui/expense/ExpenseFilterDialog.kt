package com.chronie.homemoneylite.ui.expense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.*
import com.chronie.homemoneylite.ui.components.AppDatePickerDialog
import com.chronie.homemoneylite.ui.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.domain.model.ExpenseFilters
import com.chronie.homemoneylite.domain.model.ExpenseType
import com.chronie.homemoneylite.domain.model.SortOption
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 支出筛选对话框
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ExpenseFilterDialog(
    context: android.content.Context,
    currentFilters: ExpenseFilters,
    onDismiss: () -> Unit,
    onApplyFilters: (ExpenseFilters) -> Unit
) {
    var keyword by remember { mutableStateOf(currentFilters.keyword ?: "") }
    var selectedTypes by remember { mutableStateOf(currentFilters.type?.let { setOf(it) } ?: emptySet()) }
    var minAmount by remember { mutableStateOf(currentFilters.minAmount?.toString() ?: "") }
    var maxAmount by remember { mutableStateOf(currentFilters.maxAmount?.toString() ?: "") }
    var startDate by remember { mutableStateOf(currentFilters.startDate) }
    var endDate by remember { mutableStateOf(currentFilters.endDate) }
    var sortOption by remember { mutableStateOf(currentFilters.sortBy) }
    var showTypeSelector by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colors.surface,
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context.getString(R.string.expense_list_filter_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = context.getString(R.string.cancel))
                    }
                }
                
                Divider()
                
                // 筛选内容
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 搜索关键词
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text(context.getString(R.string.common_search)) },
                        placeholder = { Text(context.getString(R.string.expense_list_search_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // 支出类型选择
                    OutlinedButton(
                        onClick = { showTypeSelector = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (selectedTypes.isEmpty()) {
                                context.getString(R.string.expense_list_filter_all_types)
                            } else {
                                context.getString(R.string.expense_list_filter_select_types) + " (${selectedTypes.size})"
                            }
                        )
                    }
                    
                    // 日期范围
                    Text(
                        text = context.getString(R.string.expense_list_filter_date_range),
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = startDate?.format(dateFormatter) 
                                    ?: context.getString(R.string.expense_list_filter_start_date)
                            )
                        }
                        
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = endDate?.format(dateFormatter) 
                                    ?: context.getString(R.string.expense_list_filter_end_date)
                            )
                        }
                    }
                    
                    // 金额范围
                    Text(
                        text = context.getString(R.string.expense_list_filter_amount_range),
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minAmount,
                            onValueChange = { minAmount = it },
                            label = { Text(context.getString(R.string.expense_list_filter_min_amount)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = maxAmount,
                            onValueChange = { maxAmount = it },
                            label = { Text(context.getString(R.string.expense_list_filter_max_amount)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    
                    // 排序选项
                    Text(
                        text = context.getString(R.string.expense_list_sort),
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SortOption.values().forEach { option ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = sortOption == option,
                                    onClick = { sortOption = option }
                                )
                                Text(
                                    text = getSortOptionText(context, option),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
                
                Divider()
                
                // 底部按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            keyword = ""
                            selectedTypes = emptySet()
                            minAmount = ""
                            maxAmount = ""
                            startDate = null
                            endDate = null
                            sortOption = SortOption.DATE_DESC
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(context.getString(R.string.expense_list_clear_filters))
                    }
                    
                    Button(
                        onClick = {
                            val filters = ExpenseFilters(
                                keyword = keyword.ifBlank { null },
                                type = selectedTypes.firstOrNull(),
                                minAmount = minAmount.toDoubleOrNull(),
                                maxAmount = maxAmount.toDoubleOrNull(),
                                startDate = startDate,
                                endDate = endDate,
                                sortBy = sortOption
                            )
                            onApplyFilters(filters)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(context.getString(R.string.expense_list_apply_filters))
                    }
                }
            }
        }
    }
    
    // 类型选择对话框
    if (showTypeSelector) {
        ExpenseTypeSelector(
            context = context,
            selectedTypes = selectedTypes,
            onDismiss = { showTypeSelector = false },
            onConfirm = { types ->
                selectedTypes = types
                showTypeSelector = false
            }
        )
    }
    
    // 开始日期选择器
    if (showStartDatePicker) {
        val initialStartMillis = startDate?.toEpochDay()?.times(86400000L)
            ?: java.time.LocalDate.now().toEpochDay() * 86400000L
        AppDatePickerDialog(
            initialDateMillis = initialStartMillis,
            onDateSelected = { millis ->
                startDate = LocalDate.ofEpochDay(millis / 86400000L)
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }
    
    // 结束日期选择器
    if (showEndDatePicker) {
        val initialEndMillis = endDate?.toEpochDay()?.times(86400000L)
            ?: java.time.LocalDate.now().toEpochDay() * 86400000L
        AppDatePickerDialog(
            initialDateMillis = initialEndMillis,
            onDateSelected = { millis ->
                endDate = LocalDate.ofEpochDay(millis / 86400000L)
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

/**
 * 支出类型选择器 - 支持搜索功能
 * 使用自定义 Dialog + LazyColumn，避免 AlertDialog 内部嵌套 verticalScroll 在滚动时
 * 出现测量异常（内容上移/崩坏/空白）的问题。
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ExpenseTypeSelector(
    context: android.content.Context,
    selectedTypes: Set<ExpenseType>,
    onDismiss: () -> Unit,
    onConfirm: (Set<ExpenseType>) -> Unit
) {
    var tempSelectedTypes by remember { mutableStateOf(selectedTypes) }
    var searchQuery by remember { mutableStateOf("") }

    // Filter types based on search query
    val filteredTypes = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            ExpenseType.values().toList()
        } else {
            ExpenseType.values().filter { type ->
                val displayName = ExpenseTypeLocalizer.getLocalizedName(context, type)
                displayName.contains(searchQuery, ignoreCase = true) ||
                    type.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colors.surface,
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = context.getString(R.string.expense_list_filter_select_types),
                    style = MaterialTheme.typography.titleLarge
                )
                if (filteredTypes.size != ExpenseType.values().size) {
                    Text(
                        text = context.getString(R.string.search_results_count, filteredTypes.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(context.getString(R.string.search_category)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = context.getString(R.string.clear))
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredTypes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = context.getString(R.string.no_results_found),
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredTypes) { type ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = tempSelectedTypes.contains(type),
                                    onCheckedChange = { checked ->
                                        tempSelectedTypes = if (checked) {
                                            tempSelectedTypes + type
                                        } else {
                                            tempSelectedTypes - type
                                        }
                                    }
                                )
                                Text(
                                    text = ExpenseTypeLocalizer.getLocalizedName(context, type),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(context.getString(R.string.cancel))
                    }
                    Button(
                        onClick = { onConfirm(tempSelectedTypes) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(context.getString(R.string.confirm))
                    }
                }
            }
        }
    }
}

/**
 * 获取排序选项的本地化文本
 */
private fun getSortOptionText(context: android.content.Context, option: SortOption): String {
    return when (option) {
        SortOption.DATE_DESC -> context.getString(R.string.expense_list_sort_date_desc)
        SortOption.DATE_ASC -> context.getString(R.string.expense_list_sort_date_asc)
        SortOption.AMOUNT_DESC -> context.getString(R.string.expense_list_sort_amount_desc)
        SortOption.AMOUNT_ASC -> context.getString(R.string.expense_list_sort_amount_asc)
    }
}
