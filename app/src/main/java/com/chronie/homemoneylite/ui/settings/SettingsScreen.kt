package com.chronie.homemoneylite.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.*
import androidx.compose.runtime.*
import com.chronie.homemoneylite.ui.components.AppDatePickerDialog
import com.chronie.homemoneylite.ui.theme.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.ExperimentalAnimationApi
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.ui.components.ExpressiveLoadingIndicator
import com.chronie.homemoneylite.ui.expense.formatDateByLocale

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private val EaseInCubic = CubicBezierEasing(0.32f, 0f, 0.67f, 0f)

private enum class SettingsCategoryPage {
    FUNCTION,
    DATA_SYNC
}

private fun SettingsCategoryPage.title(context: Context): String = when (this) {
    SettingsCategoryPage.FUNCTION -> context.getString(R.string.settings_category_function)
    SettingsCategoryPage.DATA_SYNC -> context.getString(R.string.settings_category_data_sync)
}

private fun SettingsCategoryPage.description(context: Context): String = when (this) {
    SettingsCategoryPage.FUNCTION -> context.getString(R.string.settings_category_function_description)
    SettingsCategoryPage.DATA_SYNC -> context.getString(R.string.settings_category_data_sync_description)
}

private fun SettingsCategoryPage.icon(): ImageVector = when (this) {
    SettingsCategoryPage.FUNCTION -> Icons.Default.Settings
    SettingsCategoryPage.DATA_SYNC -> Icons.Default.Storage
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsScreen(
    context: Context,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToLanSync: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var selectedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCategory: SettingsCategoryPage? = selectedCategoryName?.let { SettingsCategoryPage.valueOf(it) }

    BackHandler(enabled = selectedCategory != null) {
        selectedCategoryName = null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = selectedCategory,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = fadeIn(animationSpec = tween(durationMillis = 200, easing = EaseOutCubic)),
                    initialContentExit = fadeOut(animationSpec = tween(durationMillis = 150, easing = EaseInCubic))
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { category ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colors.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (category != null) {
                        IconButton(onClick = { selectedCategoryName = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = category?.title(context) ?: context.getString(R.string.settings),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        AnimatedContent(
            targetState = selectedCategory,
            transitionSpec = {
                if (targetState != null && initialState == null) {
                    // 进入二级页面：从右侧滑入 + 淡入
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth / 4 },
                            animationSpec = tween(durationMillis = 300, easing = EaseOutCubic)
                        ) + fadeIn(animationSpec = tween(durationMillis = 200, easing = EaseOutCubic)),
                        initialContentExit = fadeOut(animationSpec = tween(durationMillis = 150, easing = EaseInCubic))
                    )
                } else if (targetState == null && initialState != null) {
                    // 返回主页面：从左侧滑入 + 淡入
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth / 4 },
                            animationSpec = tween(durationMillis = 300, easing = EaseOutCubic)
                        ) + fadeIn(animationSpec = tween(durationMillis = 200, easing = EaseOutCubic)),
                        initialContentExit = slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth / 4 },
                            animationSpec = tween(durationMillis = 300, easing = EaseInCubic)
                        ) + fadeOut(animationSpec = tween(durationMillis = 150, easing = EaseInCubic))
                    )
                } else {
                    // 二级页面之间切换
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth / 4 },
                            animationSpec = tween(durationMillis = 300, easing = EaseOutCubic)
                        ) + fadeIn(animationSpec = tween(durationMillis = 200, easing = EaseOutCubic)),
                        initialContentExit = fadeOut(animationSpec = tween(durationMillis = 150, easing = EaseInCubic))
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { category ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (category == null) {
                    Text(
                        text = context.getString(R.string.settings_choose_category),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )

                    listOf(
                        SettingsCategoryPage.FUNCTION,
                        SettingsCategoryPage.DATA_SYNC
                    ).forEach { categoryItem ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategoryName = categoryItem.name },
                            color = MaterialTheme.colors.surfaceContainerLow,
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colors.outlineVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colors.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = categoryItem.icon(),
                                        contentDescription = null,
                                        modifier = Modifier.padding(10.dp),
                                        tint = MaterialTheme.colors.onPrimaryContainer
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = categoryItem.title(context),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = categoryItem.description(context),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colors.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = ">",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 开源许可（显示在版本信息上方，可点击）
                    OpenSourceLicensesInline(context = context)

                    Spacer(modifier = Modifier.height(8.dp))
                    AppVersionInfo(context = context)
                } else {
                    when (category) {
                        SettingsCategoryPage.FUNCTION -> FunctionSettingsContent(
                            viewModel = viewModel,
                            context = context
                        )
                        SettingsCategoryPage.DATA_SYNC -> DataSyncSettingsContent(
                            viewModel = viewModel,
                            context = context,
                            onNavigateToLanSync = onNavigateToLanSync
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun FunctionSettingsContent(
    viewModel: SettingsViewModel,
    context: Context
) {
    SettingsCategorySection(title = context.getString(R.string.budget_settings)) {
        BudgetSettingsSection(context = context)
    }

    SettingsCategorySection(title = context.getString(R.string.settings_ai_title)) {
        AISettingsSection(viewModel = viewModel, context = context)
    }
}

@Composable
private fun DataSyncSettingsContent(
    viewModel: SettingsViewModel,
    context: Context,
    onNavigateToLanSync: () -> Unit
) {
    SettingsCategorySection(title = context.getString(R.string.sync_title)) {
        SyncSection(
            viewModel = viewModel,
            context = context,
            onNavigateToLanSync = onNavigateToLanSync
        )
    }

    SettingsCategorySection(title = context.getString(R.string.data_import_export)) {
        DataImportExportSection(viewModel = viewModel, context = context)
    }
}

@Composable
private fun SettingsCategorySection(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        backgroundColor = MaterialTheme.colors.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colors.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colors.onSurface
                )
                description?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

@Composable
fun AppVersionInfo(context: Context) {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName
    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
    
    Text(
        text = "Version $versionName ($versionCode)",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colors.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
            .wrapContentWidth(Alignment.CenterHorizontally)
    )
}

@Composable
fun AISettingsSection(
    viewModel: SettingsViewModel,
    context: Context
) {
    val apiKey by viewModel.aiApiKey.collectAsState()
    var showApiKeyDialog by remember { mutableStateOf(false) }
    
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showApiKeyDialog = true },
            color = MaterialTheme.colors.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = context.getString(R.string.settings_ai_api_key),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = context.getString(R.string.settings_ai_api_key_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                        if (apiKey.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = context.getString(R.string.api_key_set, apiKey.take(8)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colors.primary
                            )
                        }
                    }
                    Text(
                        text = ">",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
    
    // API Key 输入对话框
    if (showApiKeyDialog) {
        var inputApiKey by remember { mutableStateOf(apiKey) }
        
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text(context.getString(R.string.settings_ai_api_key)) },
            text = {
                Column {
                    Text(
                        text = context.getString(R.string.settings_ai_api_key_description),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = inputApiKey,
                        onValueChange = { inputApiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(context.getString(R.string.settings_ai_api_key_hint)) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = context.getString(R.string.settings_ai_get_api_key),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier
                            .clickable {
                                try {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://cloud.siliconflow.cn/me/account/ak")
                                    )
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Browser Open Failed: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setAIApiKey(inputApiKey)
                        showApiKeyDialog = false
                    }
                ) {
                    Text(context.getString(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun BudgetSettingsSection(
    context: Context,
    budgetViewModel: com.chronie.homemoneylite.ui.budget.BudgetViewModel = hiltViewModel()
) {
    val uiState by budgetViewModel.uiState.collectAsState()
    var showBudgetDialog by remember { mutableStateOf(false) }
    
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showBudgetDialog = true },
            color = MaterialTheme.colors.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = context.getString(R.string.budget_monthly_limit),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = context.getString(R.string.budget_enable_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 显示当前预算状态
                        if (uiState.budget?.isEnabled == true) {
                            Text(
                                text = "${context.getString(R.string.budget_enable_feature)}: " + context.getString(R.string.currency_format, context.getString(R.string.currency_symbol), uiState.budget?.monthlyLimit ?: 0.0),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colors.primary
                            )
                        } else {
                            Text(
                                text = context.getString(R.string.budget_enable_title),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = ">",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
    
    // 预算设置对话框
    if (showBudgetDialog) {
        com.chronie.homemoneylite.ui.budget.BudgetSettingsDialog(
            context = context,
            currentBudget = uiState.budget,
            onDismiss = { showBudgetDialog = false },
            onSave = { limit, threshold, enabled ->
                budgetViewModel.saveBudget(limit, threshold, enabled)
                showBudgetDialog = false
            }
        )
    }
}

@Composable
fun DataImportExportSection(
    viewModel: SettingsViewModel,
    context: Context
) {
    val exportInProgress by viewModel.exportInProgress.collectAsState()
    val importInProgress by viewModel.importInProgress.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    var showDateRangeDialog by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<java.time.LocalDate?>(null) }
    var endDate by remember { mutableStateOf<java.time.LocalDate?>(null) }
    
    // 权限请求
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.permission_storage_required),
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // 检查并请求权限
    fun checkAndRequestPermissions(onGranted: () -> Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf("android.permission.READ_MEDIA_IMAGES")
        } else {
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        
        val allGranted = permissions.all { permission ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        
        if (allGranted) {
            onGranted()
        } else {
            permissionLauncher.launch(permissions)
        }
    }
    
    // 文件选择器
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { viewModel.importExpenses(it) }
    }
    
    Column {
        Text(
            text = context.getString(R.string.data_import_export_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 导出按钮
        Button(
            onClick = { 
                checkAndRequestPermissions {
                    showExportDialog = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !exportInProgress && !importInProgress
        ) {
            if (exportInProgress) {
                ExpressiveLoadingIndicator(size = 20.dp, containerVisible = false)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (exportInProgress) {
                    context.getString(R.string.export_in_progress)
                } else {
                    context.getString(R.string.export_data)
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 导入按钮
        Button(
            onClick = { 
                checkAndRequestPermissions {
                    filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !exportInProgress && !importInProgress
        ) {
            if (importInProgress) {
                ExpressiveLoadingIndicator(size = 20.dp, containerVisible = false)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (importInProgress) {
                    context.getString(R.string.import_in_progress)
                } else {
                    context.getString(R.string.import_data)
                }
            )
        }
    }
    
    // 导出选项对话框
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(context.getString(R.string.export_data)) },
            text = {
                Column {
                    Text(
                        text = context.getString(R.string.export_select_range),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // 导出全部数据按钮
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportDialog = false
                                viewModel.exportExpenses(null, null)
                            },
                        color = MaterialTheme.colors.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = context.getString(R.string.export_all_data),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 导出日期范围按钮
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportDialog = false
                                showDateRangeDialog = true
                            },
                        color = MaterialTheme.colors.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = context.getString(R.string.export_date_range),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }
    
    // 日期范围选择对话框
    if (showDateRangeDialog) {
        var showStartDatePicker by remember { mutableStateOf(false) }
        var showEndDatePicker by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showDateRangeDialog = false },
            title = { Text(context.getString(R.string.export_select_range)) },
            text = {
                Column {
                    // 开始日期
                    Text(
                        text = context.getString(R.string.export_start_date),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartDatePicker = true },
                        color = MaterialTheme.colors.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = startDate?.let { formatDateByLocale(it.toString(), context.resources.configuration.locale.toLanguageTag()) } ?: context.getString(R.string.export_start_date),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 结束日期
                    Text(
                        text = context.getString(R.string.export_end_date),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEndDatePicker = true },
                        color = MaterialTheme.colors.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = endDate?.let { formatDateByLocale(it.toString(), context.resources.configuration.locale.toLanguageTag()) } ?: context.getString(R.string.export_end_date),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                
                // 日期选择器
                if (showStartDatePicker) {
                    val initialStartMillis = startDate
                        ?.atStartOfDay(java.time.ZoneId.systemDefault())
                        ?.toInstant()
                        ?.toEpochMilli()
                        ?: java.time.LocalDate.now()
                            .atStartOfDay(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    AppDatePickerDialog(
                        initialDateMillis = initialStartMillis,
                        onDateSelected = { millis ->
                            startDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            showStartDatePicker = false
                        },
                        onDismiss = { showStartDatePicker = false }
                    )
                }
                
                if (showEndDatePicker) {
                    val initialEndMillis = endDate
                        ?.atStartOfDay(java.time.ZoneId.systemDefault())
                        ?.toInstant()
                        ?.toEpochMilli()
                        ?: java.time.LocalDate.now()
                            .atStartOfDay(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    AppDatePickerDialog(
                        initialDateMillis = initialEndMillis,
                        onDateSelected = { millis ->
                            endDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            showEndDatePicker = false
                        },
                        onDismiss = { showEndDatePicker = false }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDateRangeDialog = false
                        viewModel.exportExpenses(startDate, endDate)
                    },
                    enabled = startDate != null && endDate != null
                ) {
                    Text(context.getString(R.string.export_data))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangeDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun SyncSection(
    viewModel: SettingsViewModel,
    context: Context,
    onNavigateToLanSync: () -> Unit = {}
) {
    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    
    // 显示同步消息
    syncMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            kotlinx.coroutines.delay(3000)
            viewModel.clearSyncMessage()
        }
    }
    
    Column {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 同步状态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context.getString(R.string.sync_status),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = when (syncStatus) {
                            com.chronie.homemoneylite.domain.model.SyncStatus.IDLE -> 
                                context.getString(R.string.sync_status_idle)
                            com.chronie.homemoneylite.domain.model.SyncStatus.SYNCING -> 
                                context.getString(R.string.sync_status_syncing)
                            com.chronie.homemoneylite.domain.model.SyncStatus.SUCCESS -> 
                                context.getString(R.string.sync_status_success)
                            com.chronie.homemoneylite.domain.model.SyncStatus.FAILED -> 
                                context.getString(R.string.sync_status_failed)
                            com.chronie.homemoneylite.domain.model.SyncStatus.CONFLICT -> 
                                context.getString(R.string.sync_status_conflict)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (syncStatus) {
                            com.chronie.homemoneylite.domain.model.SyncStatus.SUCCESS -> 
                                MaterialTheme.colors.primary
                            com.chronie.homemoneylite.domain.model.SyncStatus.FAILED,
                            com.chronie.homemoneylite.domain.model.SyncStatus.CONFLICT -> 
                                MaterialTheme.colors.error
                            else -> MaterialTheme.colors.onSurfaceVariant
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 最后同步时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context.getString(R.string.sync_last_time),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = lastSyncTime?.let { 
                            try {
                                // 尝试处理包含时间的格式（如 "2026-02-05 22:10:07"）
                                val dateTimeString = it
                                val parts = dateTimeString.split(' ')
                                val datePart = parts[0] // 提取日期部分
                                val timePart = if (parts.size > 1) parts[1] else "" // 提取时间部分
                                val formattedDate = formatDateByLocale(datePart, context.resources.configuration.locale.toLanguageTag())
                                if (timePart.isNotEmpty()) {
                                    "$formattedDate $timePart"
                                } else {
                                    formattedDate
                                }
                            } catch (e: Exception) {
                                // 如果解析失败，返回原始字符串
                                it
                            }
                        } ?: context.getString(R.string.sync_never),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 待同步项数量
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context.getString(R.string.sync_pending_count),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = pendingSyncCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (pendingSyncCount > 0) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.onSurfaceVariant
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 手动同步按钮
                Button(
                    onClick = { viewModel.manualSync() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = syncStatus != com.chronie.homemoneylite.domain.model.SyncStatus.SYNCING
                ) {
                    if (syncStatus == com.chronie.homemoneylite.domain.model.SyncStatus.SYNCING) {
                        ExpressiveLoadingIndicator(size = 20.dp, containerVisible = false)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (syncStatus == com.chronie.homemoneylite.domain.model.SyncStatus.SYNCING) {
                            context.getString(R.string.sync_syncing)
                        } else {
                            context.getString(R.string.sync_manual_trigger)
                        }
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

/**
 * 开源库信息（迁移自独立的开源许可页面，现直接在设置主界面展示）。
 */
private data class LibraryInfo(
    val name: String,
    val version: String,
    val license: String,
    val licenseUrl: String,
    val projectUrl: String
)

private val libraries = listOf(
    LibraryInfo(
        name = "Kotlin Coroutines Android",
        version = "1.11.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/Kotlin/kotlinx.coroutines"
    ),
    LibraryInfo(
        name = "AndroidX Core KTX",
        version = "1.19.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/core"
    ),
    LibraryInfo(
        name = "AndroidX AppCompat",
        version = "1.7.1",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/appcompat"
    ),
    LibraryInfo(
        name = "AndroidX CoordinatorLayout",
        version = "1.3.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/coordinatorlayout"
    ),
    LibraryInfo(
        name = "AndroidX Core Splashscreen",
        version = "1.2.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/core"
    ),
    LibraryInfo(
        name = "AndroidX Activity Compose",
        version = "1.13.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/activity"
    ),
    LibraryInfo(
        name = "JUnit",
        version = "4.13.2",
        license = "Eclipse Public License 1.0",
        licenseUrl = "https://www.eclipse.org/legal/epl-v10.html",
        projectUrl = "https://junit.org/junit4/"
    ),
    LibraryInfo(
        name = "AndroidX Test JUnit",
        version = "1.3.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/test"
    ),
    LibraryInfo(
        name = "AndroidX Test Espresso",
        version = "3.7.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/test"
    ),
    LibraryInfo(
        name = "Jetpack Compose BOM",
        version = "2026.06.00",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/compose-bom"
    ),
    LibraryInfo(
        name = "M3Color",
        version = "2026.1",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/Kyant0/M3Color"
    ),
    LibraryInfo(
        name = "AndroidX Material3",
        version = "1.4.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/compose-material3"
    ),
    LibraryInfo(
        name = "AndroidX Lifecycle Runtime Compose",
        version = "2.10.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/lifecycle"
    ),
    LibraryInfo(
        name = "AndroidX Lifecycle ViewModel Compose",
        version = "2.10.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/lifecycle"
    ),
    LibraryInfo(
        name = "AndroidX Navigation Compose",
        version = "2.9.8",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/navigation"
    ),
    LibraryInfo(
        name = "Dagger Hilt Android",
        version = "2.60",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://dagger.dev/hilt/"
    ),
    LibraryInfo(
        name = "AndroidX Hilt Navigation Compose",
        version = "1.3.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/hilt"
    ),
    LibraryInfo(
        name = "AndroidX Room Runtime",
        version = "2.8.4",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/room"
    ),
    LibraryInfo(
        name = "AndroidX Room KTX",
        version = "2.8.4",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/room"
    ),
    LibraryInfo(
        name = "Retrofit",
        version = "3.0.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/square/retrofit"
    ),
    LibraryInfo(
        name = "Retrofit Gson Converter",
        version = "3.0.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/square/retrofit/tree/master/retrofit-converters/gson"
    ),
    LibraryInfo(
        name = "OkHttp Logging Interceptor",
        version = "5.4.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/square/okhttp"
    ),
    LibraryInfo(
        name = "AndroidX Paging Runtime KTX",
        version = "3.5.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/paging"
    ),
    LibraryInfo(
        name = "AndroidX Paging Compose",
        version = "3.5.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/paging"
    ),
    LibraryInfo(
        name = "Coil Compose",
        version = "2.7.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/coil-kt/coil"
    ),
    LibraryInfo(
        name = "AndroidX Security Crypto",
        version = "1.1.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/security"
    ),
    LibraryInfo(
        name = "SQLCipher Android",
        version = "4.16.0",
        license = "BSD 3-Clause License",
        licenseUrl = "https://opensource.org/licenses/BSD-3-Clause",
        projectUrl = "https://www.zetetic.net/sqlcipher/"
    ),
    LibraryInfo(
        name = "AndroidX SQLite",
        version = "2.6.2",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/sqlite"
    ),
    LibraryInfo(
        name = "AndroidX Work Runtime KTX",
        version = "2.11.2",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/work"
    ),
    LibraryInfo(
        name = "AndroidX Hilt Work",
        version = "1.3.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/hilt"
    ),
    LibraryInfo(
        name = "FastExcel",
        version = "0.20.2",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/dhatim/fastexcel"
    ),
    LibraryInfo(
        name = "FastExcel Reader",
        version = "0.20.2",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/dhatim/fastexcel"
    ),
    LibraryInfo(
        name = "Aalto XML",
        version = "1.4.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/FasterXML/aalto-xml"
    ),
    LibraryInfo(
        name = "XZ",
        version = "1.12",
        license = "Public Domain",
        licenseUrl = "https://tukaani.org/xz/legal.html",
        projectUrl = "https://tukaani.org/xz/"
    ),
    LibraryInfo(
        name = "UCrop",
        version = "2.2.11",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/Yalantis/uCrop"
    ),
    LibraryInfo(
        name = "MockK",
        version = "1.14.9",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://mockk.io/"
    ),
    LibraryInfo(
        name = "Kotlin Coroutines Test",
        version = "1.11.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/Kotlin/kotlinx.coroutines"
    )
)

/**
 * 在设置主界面（版本信息上方）直接展示所有开源库的许可证，按许可证分组，
 * 每一组可点击跳转到对应许可证页面。格式示例：
 * Apache License 2.0: M3Color、AndroidX Material3、...
 */
@Composable
private fun OpenSourceLicensesInline(context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        backgroundColor = MaterialTheme.colors.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colors.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = context.getString(R.string.open_source_licenses),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val grouped = libraries.groupBy { it.license }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                grouped.forEach { (license, libs) ->
                    val licenseUrl = libs.first().licenseUrl
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(licenseUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                } catch (_: Exception) {
                                    // 忽略无法打开链接的异常
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colors.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append("$license: ")
                                }
                                withStyle(
                                    SpanStyle(color = MaterialTheme.colors.onSurfaceVariant)
                                ) {
                                    append(libs.joinToString(separator = "、") { it.name })
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
