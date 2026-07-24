package com.chronie.homemoneylite.ui.expense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.*
import com.chronie.homemoneylite.ui.components.AppDatePickerDialog
import com.chronie.homemoneylite.ui.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.domain.model.ExpenseType
import com.chronie.homemoneylite.ui.components.ExpressiveLoadingIndicator
import com.chronie.homemoneylite.ui.components.CircularIconButton
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 添加支出界面
 */
@Composable
fun AddExpenseScreen(
    context: android.content.Context,
    expenseId: String? = null,
    viewModel: AddExpenseViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 编辑模式初始化（不再强制要求登录）
    LaunchedEffect(expenseId) {
        if (expenseId != null) {
            // 如果有expenseId，加载支出记录进行编辑
            viewModel.loadExpenseForEdit(expenseId)
        }
    }

    // 显示日期选择器的状态
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (expenseId != null) 
                            context.getString(R.string.edit_expense_title) 
                        else 
                            context.getString(R.string.add_expense_title) 
                    ) 
                },
                navigationIcon = {
                    CircularIconButton(onClick = onNavigateBack, modifier = Modifier.padding(start = 8.dp, end = 4.dp)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = context.getString(R.string.back))
                    }
                },
                actions = {
                    CircularIconButton(
                        onClick = {
                            viewModel.saveExpense(
                                onSuccess = {
                                    onNavigateBack()
                                },
                                onError = {
                                    // 错误会通过 snackbar 显示
                                }
                            )
                        },
                        enabled = !uiState.isSaving,
                        modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                    ) {
                        if (uiState.isSaving) {
                            ExpressiveLoadingIndicator(size = 24.dp, containerVisible = false)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = context.getString(R.string.save))
                        }
                    }
                },
                backgroundColor = MaterialTheme.colors.background
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 类型选择
            ExpenseTypeDropdown(
                selectedType = uiState.selectedType,
                error = uiState.typeError,
                context = context,
                onTypeSelected = { viewModel.setType(it) }
            )

            // 金额输入
            ExpenseAmountField(
                amount = uiState.amount,
                error = uiState.amountError,
                context = context,
                onAmountChange = { viewModel.setAmount(it) }
            )

            // 日期选择
            ExpenseDateField(
                selectedDate = uiState.selectedDate,
                error = uiState.dateError,
                context = context,
                onClick = { showDatePicker = true }
            )

            // 备注输入
            ExpenseRemarkField(
                remark = uiState.remark,
                context = context,
                onRemarkChange = { viewModel.setRemark(it) }
            )
        }
    }

    // 日期选择器
    if (showDatePicker) {
        ExpenseDatePickerDialog(
            context = context,
            initialDate = uiState.selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                viewModel.setDate(date)
                showDatePicker = false
            }
        )
    }

    // 显示保存错误
    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let { error ->
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.add_expense_save_failed, error),
                duration = SnackbarDuration.Short
            )
        }
    }
}

/**
 * 类型选择下拉菜单 - 支持搜索功能
 */
@Composable
fun ExpenseTypeDropdown(
    selectedType: ExpenseType?,
    error: String?,
    context: android.content.Context,
    onTypeSelected: (ExpenseType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
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
    
    // Reset search when dropdown closes
    LaunchedEffect(expanded) {
        if (!expanded) {
            searchQuery = ""
        }
    }

    Column {
        Text(
            text = context.getString(R.string.add_expense_type_label),
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Box {
            OutlinedTextField(
                value = if (selectedType != null && !expanded) {
                    ExpenseTypeLocalizer.getLocalizedName(context, selectedType)
                } else if (expanded && searchQuery.isNotEmpty()) {
                    searchQuery
                } else if (selectedType != null) {
                    ExpenseTypeLocalizer.getLocalizedName(context, selectedType)
                } else {
                    ""
                },
                onValueChange = {
                    searchQuery = it
                    if (!expanded) expanded = true
                },
                modifier = Modifier
                    .fillMaxWidth(),
                placeholder = { Text(context.getString(R.string.add_expense_type_hint)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                },
                isError = error != null,
                singleLine = true
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 280.dp)
            ) {
                if (filteredTypes.isEmpty()) {
                    DropdownMenuItem(
                        onClick = { expanded = false },
                        enabled = false
                    ) {
                        Text(
                            context.getString(R.string.no_results_found),
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                    }
                } else {
                    filteredTypes.forEach { type ->
                        DropdownMenuItem(
                            onClick = {
                                onTypeSelected(type)
                                searchQuery = ""
                                expanded = false
                            }
                        ) {
                            Text(
                                ExpenseTypeLocalizer.getLocalizedName(context, type),
                                fontWeight = if (type == selectedType) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        if (error != null) {
            Text(
                text = when (error) {
                    "TYPE_REQUIRED" -> context.getString(R.string.add_expense_validation_type_required)
                    else -> error
                },
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * 金额输入字段
 */
@Composable
fun ExpenseAmountField(
    amount: String,
    error: String?,
    context: android.content.Context,
    onAmountChange: (String) -> Unit
) {
    Column {
        Text(
            text = context.getString(R.string.add_expense_amount_label),
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(context.getString(R.string.add_expense_amount_hint)) },
            leadingIcon = { Text(context.getString(R.string.currency_symbol) + " ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = error != null
        )
        if (error != null) {
            Text(
                text = when (error) {
                    "AMOUNT_REQUIRED" -> context.getString(R.string.add_expense_validation_amount_required)
                    "AMOUNT_INVALID" -> context.getString(R.string.add_expense_validation_amount_invalid)
                    else -> error
                },
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * 日期选择字段
 */
@Composable
fun ExpenseDateField(
    selectedDate: LocalDate,
    error: String?,
    context: android.content.Context,
    onClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    Column {
        Text(
            text = context.getString(R.string.add_expense_date_label),
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = selectedDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        if (error != null) {
            Text(
                text = when (error) {
                    "DATE_REQUIRED" -> context.getString(R.string.add_expense_validation_date_required)
                    else -> error
                },
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * 备注输入字段
 */
@Composable
fun ExpenseRemarkField(
    remark: String,
    context: android.content.Context,
    onRemarkChange: (String) -> Unit
) {
    Column {
        Text(
            text = context.getString(R.string.add_expense_remark_label),
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = remark,
            onValueChange = onRemarkChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text(context.getString(R.string.add_expense_remark_hint)) },
            maxLines = 4
        )
    }
}

/**
 * 日期选择器对话框
 */
@Composable
fun ExpenseDatePickerDialog(
    context: android.content.Context,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val initialMillis = initialDate.toEpochDay() * 86400000L

    AppDatePickerDialog(
        initialDateMillis = initialMillis,
        onDateSelected = { millis ->
            val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
            onDateSelected(date)
        },
        onDismiss = onDismiss
    )
}