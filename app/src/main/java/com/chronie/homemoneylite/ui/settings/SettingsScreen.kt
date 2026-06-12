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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.core.common.Language
import com.chronie.homemoneylite.ui.components.ExpressiveSwitch
import com.chronie.homemoneylite.ui.components.ExpressiveLoadingIndicator
import com.chronie.homemoneylite.ui.components.ColorPickerBottomSheet
import com.chronie.homemoneylite.ui.components.LanguageSelectorBottomSheet
import com.chronie.homemoneylite.ui.components.getColorGroups
import com.chronie.homemoneylite.ui.expense.formatDateByLocale
import com.chronie.homemoneylite.ui.theme.LocalThemeSettings
import com.chronie.homemoneylite.ui.theme.ThemeSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    context: Context,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToDatabaseTest: () -> Unit = {},
    onNavigateToLanSync: () -> Unit = {},
    onNavigateToOpenSourceLicenses: () -> Unit = {},
    onLogout: () -> Unit = {},
    onRequireLogin: () -> Unit = {}
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val scrollState = androidx.compose.foundation.rememberScrollState()
    var showLanguageBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部标题栏
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(R.string.settings),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        
        // 可滚动内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))
            
            // 语言选择
            Text(
                text = context.getString(R.string.language_settings),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showLanguageBottomSheet = true
                    },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = context.getString(R.string.select_language),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = currentLanguage.localName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = ">",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            
            if (showLanguageBottomSheet) {
                LanguageSelectorBottomSheet(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = { viewModel.setLanguage(it) },
                    onDismiss = { showLanguageBottomSheet = false },
                    context = context
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // AI 设置部分
            Divider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            AISettingsSection(viewModel = viewModel, context = context)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 预算管理部分
            Divider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            BudgetSettingsSection(context = context)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 数据同步部分
            Divider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SyncSection(
                viewModel = viewModel,
                context = context,
                onNavigateToLanSync = onNavigateToLanSync
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 数据导入导出部分
            Divider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DataImportExportSection(viewModel = viewModel, context = context)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 主题颜色设置部分
            Divider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = context.getString(R.string.theme_settings),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 动态颜色开关
            val useDynamicColor by viewModel.useDynamicColor.collectAsState()
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.dynamic_color),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = context.getString(R.string.dynamic_color_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 获取当前主题设置
                    val themeSettings = LocalThemeSettings.current
                    
                    ExpressiveSwitch(
                        checked = useDynamicColor,
                        onCheckedChange = { enabled ->
                            // 同时更新本地状态和ViewModel
                            themeSettings.value = ThemeSettings(
                                useDynamicColor = enabled,
                                primaryColor = themeSettings.value.primaryColor,
                                paletteStyle = themeSettings.value.paletteStyle
                            )
                            viewModel.toggleDynamicColor(enabled)
                        }
                    )
                }
            }
            
            // 手动颜色选择器（仅当动态颜色关闭时显示）
            if (!useDynamicColor) {
                Spacer(modifier = Modifier.height(16.dp))
                
                var showColorPicker by remember { mutableStateOf(false) }
                val themeSettings = LocalThemeSettings.current
                
                Text(
                    text = context.getString(R.string.manual_color_selection),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // 当前颜色显示和选择按钮
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showColorPicker = true },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 当前颜色圆
                            Surface(
                                shape = CircleShape,
                                color = Color(themeSettings.value.primaryColor),
                                modifier = Modifier.size(32.dp)
                            ) {}
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = context.getString(R.string.current_theme_color),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        
                        Text(
                            text = ">",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                
                // 颜色选择器BottomSheet
                if (showColorPicker) {
                    ColorPickerBottomSheet(
                        currentColor = themeSettings.value.primaryColor,
                        onColorSelected = { color ->
                            themeSettings.value = ThemeSettings(
                                useDynamicColor = false,
                                primaryColor = color,
                                paletteStyle = themeSettings.value.paletteStyle
                            )
                            viewModel.setPrimaryColor(color)
                        },
                        onDismiss = { showColorPicker = false },
                        context = context
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 开发者模式开关
            val isDeveloperMode by viewModel.isDeveloperMode.collectAsState(initial = false)
            
            Divider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = context.getString(R.string.developer_options),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.developer_mode),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = context.getString(R.string.developer_mode_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ExpressiveSwitch(
                        checked = isDeveloperMode,
                        onCheckedChange = { viewModel.toggleDeveloperMode() }
                    )
                }
            }
            
            // 开发者工具（仅在开发者模式下显示）
            if (isDeveloperMode) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = context.getString(R.string.developer_tools),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // 本地数据库入口按钮
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToDatabaseTest),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = context.getString(R.string.database_test),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = ">",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 意见反馈
            Divider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = context.getString(R.string.feedback_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://wj.qq.com/s2/24109109/3572/")
                            )
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(
                                context,
                                "Brower Open Failed: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.feedback_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = context.getString(R.string.feedback_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = ">",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            
            // 开源许可证信息
            Spacer(modifier = Modifier.height(32.dp))
            
            Divider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = context.getString(R.string.open_source_licenses),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToOpenSourceLicenses() },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.open_source_licenses),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = context.getString(R.string.open_source_licenses_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = ">",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 应用版本信息
            AppVersionInfo(context = context)
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
            .wrapContentWidth(Alignment.CenterHorizontally)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdownSelector(
    currentLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    context: Context
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentLanguage.localName,
            onValueChange = {},
            readOnly = true,
            label = { Text(context.getString(R.string.select_language)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Language.values().forEach { language ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = language.englishName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = language.localName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (language == currentLanguage) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    onClick = {
                        onLanguageSelected(language)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun LanguageItem(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = language.englishName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = language.localName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun AISettingsSection(
    viewModel: SettingsViewModel,
    context: Context
) {
    val apiKey by viewModel.aiApiKey.collectAsState()
    var showApiKeyDialog by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = context.getString(R.string.settings_ai_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showApiKeyDialog = true },
            color = MaterialTheme.colorScheme.surfaceVariant,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (apiKey.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = context.getString(R.string.api_key_set, apiKey.take(8)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
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
                        color = MaterialTheme.colorScheme.primary,
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
        Text(
            text = context.getString(R.string.budget_settings),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showBudgetDialog = true },
            color = MaterialTheme.colorScheme.surfaceVariant,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 显示当前预算状态
                        if (uiState.budget?.isEnabled == true) {
                            Text(
                                text = "${context.getString(R.string.budget_enable_feature)}: " + context.getString(R.string.currency_format, context.getString(R.string.currency_symbol), uiState.budget?.monthlyLimit ?: 0.0),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = context.getString(R.string.budget_enable_title),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

@OptIn(ExperimentalMaterial3Api::class)
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
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
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
            text = context.getString(R.string.data_import_export),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = context.getString(R.string.data_import_export_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.surfaceVariant,
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
                        color = MaterialTheme.colorScheme.surfaceVariant,
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
                        color = MaterialTheme.colorScheme.surfaceVariant,
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
                        color = MaterialTheme.colorScheme.surfaceVariant,
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
                    val datePickerState = androidx.compose.material3.rememberDatePickerState()
                    androidx.compose.material3.DatePickerDialog(
                        onDismissRequest = { showStartDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        startDate = java.time.Instant.ofEpochMilli(millis)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .toLocalDate()
                                    }
                                    showStartDatePicker = false
                                }
                            ) {
                                Text(context.getString(R.string.confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showStartDatePicker = false }) {
                                Text(context.getString(R.string.cancel))
                            }
                        }
                    ) {
                        androidx.compose.material3.DatePicker(state = datePickerState)
                    }
                }
                
                if (showEndDatePicker) {
                    val datePickerState = androidx.compose.material3.rememberDatePickerState()
                    androidx.compose.material3.DatePickerDialog(
                        onDismissRequest = { showEndDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        endDate = java.time.Instant.ofEpochMilli(millis)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .toLocalDate()
                                    }
                                    showEndDatePicker = false
                                }
                            ) {
                                Text(context.getString(R.string.confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEndDatePicker = false }) {
                                Text(context.getString(R.string.cancel))
                            }
                        }
                    ) {
                        androidx.compose.material3.DatePicker(state = datePickerState)
                    }
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
fun DeviceSyncDialog(
    context: Context,
    connectionType: String,
    onDismiss: () -> Unit,
    onDeviceSelected: (com.chronie.homemoneylite.domain.sync.DeviceInfo) -> Unit
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val syncMessage by viewModel.syncMessage.collectAsState()
    var discoveredDevices by remember { mutableStateOf<List<com.chronie.homemoneylite.domain.sync.DeviceInfo>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val isActive = remember { mutableStateOf(true) }
    
    // 开始搜索设备 (持续收集Flow直到弹窗关闭)
    LaunchedEffect(connectionType) {
        isActive.value = true
        discoveredDevices = emptyList()
        viewModel.searchDevices().collect {
            if (isActive.value) {
                if (!discoveredDevices.any { existing -> existing.deviceId == it.deviceId }) {
                    discoveredDevices = discoveredDevices + it
                }
            }
        }
    }
    
    // 关闭搜索
    DisposableEffect(Unit) {
        onDispose {
            isActive.value = false
        }
    }
    
    AlertDialog(
        onDismissRequest = {
            isActive.value = false
            onDismiss()
        },
        title = { Text(context.getString(R.string.device_sync_title, connectionType)) },
        text = {
            Column {
                Text(
                    text = syncMessage ?: context.getString(R.string.device_sync_searching, connectionType),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (discoveredDevices.isEmpty()) {
                    Text(context.getString(R.string.device_sync_searching_devices), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(context.getString(R.string.device_sync_found_devices, discoveredDevices.size), style = MaterialTheme.typography.bodySmall)
                    discoveredDevices.forEachIndexed { index, device ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable {
                                    isActive.value = false
                                    onDeviceSelected(device)
                                    onDismiss()
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = "${index + 1}. ${device.deviceName} (${device.address})",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                isActive.value = false
                onDismiss()
            }) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
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
    
    // 同步方式选择对话框
    var showSyncMethodDialog by remember { mutableStateOf(false) }
    
    // 显示同步消息
    syncMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            kotlinx.coroutines.delay(3000)
            viewModel.clearSyncMessage()
        }
    }
    
    Column {
        Text(
            text = context.getString(R.string.sync_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
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
                                MaterialTheme.colorScheme.primary
                            com.chronie.homemoneylite.domain.model.SyncStatus.FAILED,
                            com.chronie.homemoneylite.domain.model.SyncStatus.CONFLICT -> 
                                MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 手动同步按钮
                Button(
                    onClick = { showSyncMethodDialog = true },
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
    
    // 同步方式选择对话框
    if (showSyncMethodDialog) {
        AlertDialog(
            onDismissRequest = { showSyncMethodDialog = false },
            title = { Text(context.getString(R.string.sync_select_method)) },
            text = {
                Column {
                    // 从云端同步选项
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable {
                                showSyncMethodDialog = false
                                viewModel.manualSync()
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = context.getString(R.string.sync_cloud),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        text = context.getString(R.string.sync_other_devices),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // 局域网同步选项
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSyncMethodDialog = false
                                onNavigateToLanSync()
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = context.getString(R.string.sync_lan),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSyncMethodDialog = false }
                ) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
