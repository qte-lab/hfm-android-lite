package com.chronie.homemoneylite.ui.expense

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.domain.model.Expense
import com.chronie.homemoneylite.ui.budget.BudgetCard
import com.chronie.homemoneylite.ui.components.ExpressiveLoadingIndicator
import com.chronie.homemoneylite.ui.components.PullToRefreshBox
import com.chronie.homemoneylite.ui.theme.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 支出列表界面
 */
@Composable
fun ExpenseListScreen(
    context: android.content.Context,
    viewModel: ExpenseListViewModel = hiltViewModel(),
    shouldRefresh: Boolean = false,
    onRefreshHandled: () -> Unit = {},
    onNavigateToMoreFunctions: () -> Unit = {},
    onNavigateToAddExpense: () -> Unit = {},
    onNavigateToEditExpense: (expenseId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var budgetRefreshTrigger by remember { mutableStateOf(0) }
    
    // 处理刷新请求
    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            viewModel.refresh()
            budgetRefreshTrigger++
            onRefreshHandled()
        }
    }
    
    // 刷新函数
    val onRefresh: () -> Unit = {
        isRefreshing = true
        viewModel.refresh()
        budgetRefreshTrigger++
    }
    
    // 处理刷新状态重置
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(1000)
            isRefreshing = false
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        // 顶部工具栏 - 固定在页面顶部，不随内容滚动
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.surface,
            elevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(R.string.expense_list_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = context.getString(R.string.common_more_functions)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                showMoreMenu = false
                                showFilterDialog = true
                            }
                        ) {
                            Text(context.getString(R.string.common_filter))
                        }
                        DropdownMenuItem(
                            onClick = {
                                showMoreMenu = false
                                viewModel.resetFilters()
                            }
                        ) {
                            Text(context.getString(R.string.expense_list_clear_filters))
                        }
                    }
                }
            }
        }
        
        // 内容区域 - 放在工具栏下方，可以滚动
        Column(modifier = Modifier.fillMaxSize()) {
            // 给工具栏留出空间
            Spacer(modifier = Modifier.height(64.dp)) // 工具栏的大致高度
            
            when {
                uiState.isLoading && uiState.expenses.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ExpressiveLoadingIndicator(containerVisible = true)
                    }
                }
                uiState.error != null && uiState.expenses.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = uiState.error ?: context.getString(R.string.common_error),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Button(onClick = { viewModel.refresh() }) {
                                Text(context.getString(R.string.common_retry))
                            }
                        }
                    }
                }
                uiState.expenses.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = context.getString(R.string.expense_list_empty),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = context.getString(R.string.expense_list_empty_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    val listState = rememberLazyListState()
                    val groupedExpenses = uiState.groupedExpenses
                    var lastLoadTime by remember { mutableStateOf(0L) }
                    
                    // 改进的滚动检测：仅当真正接近底部时才加载更多
                    LaunchedEffect(listState, uiState.hasMore, uiState.isLoading) {
                        snapshotFlow { 
                            val layoutInfo = listState.layoutInfo
                            val totalItemsCount = layoutInfo.totalItemsCount
                            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                            Triple(totalItemsCount, lastVisibleIndex, layoutInfo.visibleItemsInfo.firstOrNull()?.index)
                        }.collect { (totalItemsCount, lastVisibleIndex, firstVisibleIndex) ->
                            val currentTime = System.currentTimeMillis()
                            if (totalItemsCount > 0 && 
                                lastVisibleIndex != null && 
                                firstVisibleIndex != null &&
                                lastVisibleIndex >= totalItemsCount - 1 && // 只在最后一项可见时才触发
                                uiState.hasMore && 
                                !uiState.isLoading &&
                                currentTime - lastLoadTime > 1000) { // 防抖，至少间隔1秒
                                lastLoadTime = currentTime
                                viewModel.loadMore()
                            }
                        }
                    }
                    
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp), // 为浮动按钮留出空间
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                        // 预算管理卡片
                        item(key = "budget_card") {
                            BudgetCard(
                                context = context,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                refreshTrigger = budgetRefreshTrigger
                            )
                        }
                        
                        // 统计信息卡片
                        item(key = "stats_card") {
                            ExpenseStatisticsCard(
                                statistics = uiState.statistics,
                                context = context,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                        
                        // 支出列表项
                        groupedExpenses.forEach { (date, expenses) ->
                            // 日期标题
                            item(key = "header_$date") {
                                ExpenseDateHeader(
                                    date = date,
                                    count = expenses.size,
                                    totalAmount = expenses.sumOf { it.amount },
                                    context = context,
                                    locale = context.resources.configuration.locale.toLanguageTag(),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                            
                            // 该日期下的支出项
                            items(
                                items = expenses,
                                key = { expense -> "expense_${expense.id}" }
                            ) { expense ->
                                LongPressExpenseItem(
                                    expense = expense,
                                    context = context,
                                    onEdit = { onNavigateToEditExpense(expense.id) },
                                    onDelete = { viewModel.deleteExpense(expense) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                        
                        // 加载更多指示器
                        if (uiState.hasMore) {
                            item(key = "load_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoading) {
                                        ExpressiveLoadingIndicator(containerVisible = true)
                                    } else {
                                        Button(onClick = { viewModel.loadMore() }) {
                                            Text(context.getString(R.string.common_loading))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
        
        // 浮动按钮
        FloatingActionButton(
            onClick = onNavigateToAddExpense,
            backgroundColor = MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 80.dp) // 抬高按钮，避免被底部导航栏遮挡
        ) {
            Icon(Icons.Default.Add, contentDescription = context.getString(R.string.add_expense_title))
        }
        
        // 筛选对话框
        if (showFilterDialog) {
            ExpenseFilterDialog(
                context = context,
                currentFilters = uiState.filters,
                onDismiss = { showFilterDialog = false },
                onApplyFilters = { filters ->
                    viewModel.updateFilters(filters)
                }
            )
        }
    }
}

/**
 * 日期标题
 */
@Composable
fun ExpenseDateHeader(
    date: String,
    count: Int,
    totalAmount: Double,
    context: android.content.Context,
    locale: String? = null,
    modifier: Modifier = Modifier
) {
    val displayDate = formatRelativeDate(date, context, locale)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = displayDate,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface
            )
            Text(
                text = context.getString(R.string.expense_stats_count) + ": $count",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colors.onSurfaceVariant
            )
        }
        Text(
            text = "-" + context.getString(R.string.currency_format, context.getString(R.string.currency_symbol), totalAmount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.error
        )
    }
}

/**
 * 统计信息卡片
 */
@Composable
fun ExpenseStatisticsCard(
    statistics: com.chronie.homemoneylite.domain.model.ExpenseStatistics,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    label = context.getString(R.string.expense_stats_count),
                    value = statistics.count.toString()
                )
                StatisticItem(
                    label = context.getString(R.string.expense_stats_total),
                    value = context.getString(R.string.currency_format, context.getString(R.string.currency_symbol), statistics.totalAmount)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    label = context.getString(R.string.expense_stats_average),
                    value = context.getString(R.string.currency_format, context.getString(R.string.currency_symbol), statistics.averageAmount)
                )
                StatisticItem(
                    label = context.getString(R.string.expense_stats_median),
                    value = context.getString(R.string.currency_format, context.getString(R.string.currency_symbol), statistics.medianAmount)
                )
            }
        }
    }
}

/**
 * 统计项
 */
@Composable
fun StatisticItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colors.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onPrimaryContainer
        )
    }
}

/**
 * 长按触发的支出列表项
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LongPressExpenseItem(
    expense: Expense,
    context: android.content.Context,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 底部托盘菜单显示状态
    val showBottomSheetMenu = remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    
    // 弹窗状态 - 第一次确认
    val showFirstConfirmDialog = remember { mutableStateOf(false) }
    // 弹窗状态 - 第二次确认
    val showSecondConfirmDialog = remember { mutableStateOf(false) }
    
    // 同步显隐到 MD2 底部抽屉状态
    LaunchedEffect(showBottomSheetMenu.value) {
        if (showBottomSheetMenu.value) bottomSheetState.show() else bottomSheetState.hide()
    }
    LaunchedEffect(bottomSheetState.currentValue) {
        if (bottomSheetState.currentValue == ModalBottomSheetValue.Hidden) showBottomSheetMenu.value = false
    }
    
    // 处理第一次确认
    fun handleFirstConfirm() {
        showFirstConfirmDialog.value = false
        showSecondConfirmDialog.value = true
    }
    
    // 处理第二次确认
    fun handleSecondConfirm() {
        showSecondConfirmDialog.value = false
        showBottomSheetMenu.value = false
        onDelete()
    }
    
    // 取消删除
    fun cancelDelete() {
        showFirstConfirmDialog.value = false
        showSecondConfirmDialog.value = false
    }
    
    // 显示删除确认
    fun showDeleteConfirm() {
        showBottomSheetMenu.value = false
        showFirstConfirmDialog.value = true
    }
    
    // 使用传递的context来获取本地化字符串
    val typeDisplayName = ExpenseTypeLocalizer.getLocalizedName(context, expense.type)
    
    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetShape = MaterialTheme.shapes.large,
        sheetBackgroundColor = MaterialTheme.colors.surface,
        sheetContentColor = MaterialTheme.colors.onSurface,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 选中记录的详细信息
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = typeDisplayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!expense.remark.isNullOrBlank()) {
                        Text(
                            text = expense.remark,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDateByLocale(expense.date, context.resources.configuration.locale.toLanguageTag()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                        Text(
                            text = "-" + context.getString(R.string.currency_format, context.getString(R.string.currency_symbol), expense.amount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.error
                        )
                    }
                }
                
                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 编辑按钮
                    Button(
                        onClick = {
                            showBottomSheetMenu.value = false
                            onEdit()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primaryContainer,
                            contentColor = MaterialTheme.colors.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = context.getString(R.string.edit))
                    }
                    
                    // 删除按钮
                    Button(
                        onClick = {
                            showDeleteConfirm()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.errorContainer,
                            contentColor = MaterialTheme.colors.onErrorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = context.getString(R.string.delete))
                    }
                }
            }
        }
    ) {
        // 原屏幕主体内容
        Box(modifier = modifier.fillMaxWidth()) {
            // 支出列表项 - 添加长按检测
            ExpenseListItem(
                expense = expense,
                context = context,
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                showBottomSheetMenu.value = true
                            }
                        )
                    }
            )
            
            // 第一次确认弹窗
            if (showFirstConfirmDialog.value) {
                AlertDialog(
                    onDismissRequest = { cancelDelete() },
                    title = { Text(text = context.getString(R.string.delete_confirm_title)) },
                    text = { Text(text = context.getString(R.string.delete_confirm_message)) },
                    confirmButton = {
                        Button(onClick = { handleFirstConfirm() }) {
                            Text(text = context.getString(R.string.confirm))
                        }
                    },
                    dismissButton = {
                        Button(onClick = { cancelDelete() }) {
                            Text(text = context.getString(R.string.cancel))
                        }
                    }
                )
            }
            
            // 第二次确认弹窗
            if (showSecondConfirmDialog.value) {
                AlertDialog(
                    onDismissRequest = { cancelDelete() },
                    title = { Text(text = context.getString(R.string.delete_second_confirm_title)) },
                    text = { Text(text = context.getString(R.string.delete_second_confirm_message)) },
                    confirmButton = {
                        Button(
                            onClick = { handleSecondConfirm() },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.errorContainer,
                                contentColor = MaterialTheme.colors.onErrorContainer
                            )
                        ) {
                            Text(text = context.getString(R.string.delete))
                        }
                    },
                    dismissButton = {
                        Button(onClick = { cancelDelete() }) {
                            Text(text = context.getString(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

/**
 * 支出列表项
 */
@Composable
fun ExpenseListItem(
    expense: Expense,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    // 使用传递的context来获取本地化字符串
    val typeDisplayName = ExpenseTypeLocalizer.getLocalizedName(context, expense.type)

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = typeDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (!expense.remark.isNullOrBlank()) {
                    Text(
                        text = expense.remark,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }
                Text(
                    text = formatDateByLocale(expense.date, context.resources.configuration.locale.toLanguageTag()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            }
            Text(
                text = "-" + context.getString(R.string.currency_format, context.getString(R.string.currency_symbol), expense.amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.error
        )
    }
}
}
