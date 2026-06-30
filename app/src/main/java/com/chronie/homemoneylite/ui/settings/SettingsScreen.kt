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
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseInCubic
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.chronie.homemoneylite.ui.components.getColorGroups
import com.chronie.homemoneylite.ui.expense.formatDateByLocale
import com.chronie.homemoneylite.ui.theme.LocalThemeSettings
import com.chronie.homemoneylite.ui.theme.ThemeSettings

private enum class SettingsCategoryPage {
    THEME,
    FUNCTION,
    DATA_SYNC,
    MORE
}

private fun SettingsCategoryPage.title(context: Context): String = when (this) {
    SettingsCategoryPage.THEME -> context.getString(R.string.settings_category_theme)
    SettingsCategoryPage.FUNCTION -> context.getString(R.string.settings_category_function)
    SettingsCategoryPage.DATA_SYNC -> context.getString(R.string.settings_category_data_sync)
    SettingsCategoryPage.MORE -> context.getString(R.string.settings_category_more)
}

private fun SettingsCategoryPage.description(context: Context): String = when (this) {
    SettingsCategoryPage.THEME -> context.getString(R.string.settings_category_theme_description)
    SettingsCategoryPage.FUNCTION -> context.getString(R.string.settings_category_function_description)
    SettingsCategoryPage.DATA_SYNC -> context.getString(R.string.settings_category_data_sync_description)
    SettingsCategoryPage.MORE -> context.getString(R.string.settings_category_more_description)
}

private fun SettingsCategoryPage.icon(): ImageVector = when (this) {
    SettingsCategoryPage.THEME -> Icons.Default.Settings
    SettingsCategoryPage.FUNCTION -> Icons.Default.Settings
    SettingsCategoryPage.DATA_SYNC -> Icons.Default.Storage
    SettingsCategoryPage.MORE -> Icons.Default.Build
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    context: Context,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToDatabaseTest: () -> Unit = {},
    onNavigateToLanSync: () -> Unit = {},
    onNavigateToOpenSourceLicenses: () -> Unit = {}
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
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
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    listOf(
                        SettingsCategoryPage.THEME,
                        SettingsCategoryPage.FUNCTION,
                        SettingsCategoryPage.DATA_SYNC,
                        SettingsCategoryPage.MORE
                    ).forEach { categoryItem ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategoryName = categoryItem.name },
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
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
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = categoryItem.icon(),
                                        contentDescription = null,
                                        modifier = Modifier.padding(10.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    AppVersionInfo(context = context)
                } else {
                    when (category) {
                        SettingsCategoryPage.THEME -> ThemeSettingsContent(
                            viewModel = viewModel,
                            context = context,
                            currentLanguage = currentLanguage,
                            onNavigateToOpenSourceLicenses = onNavigateToOpenSourceLicenses
                        )
                        SettingsCategoryPage.FUNCTION -> FunctionSettingsContent(
                            viewModel = viewModel,
                            context = context
                        )
                        SettingsCategoryPage.DATA_SYNC -> DataSyncSettingsContent(
                            viewModel = viewModel,
                            context = context,
                            onNavigateToLanSync = onNavigateToLanSync
                        )
                        SettingsCategoryPage.MORE -> MoreSettingsContent(
                            context = context,
                            onNavigateToOpenSourceLicenses = onNavigateToOpenSourceLicenses,
                            onNavigateToDatabaseTest = onNavigateToDatabaseTest
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSettingsContent(
    viewModel: SettingsViewModel,
    context: Context,
    currentLanguage: Language,
    onNavigateToOpenSourceLicenses: () -> Unit
) {
    val useDynamicColor by viewModel.useDynamicColor.collectAsState()
    val themeSettings = LocalThemeSettings.current

    SettingsCategorySection(title = context.getString(R.string.language_settings)) {
        LanguageDropdownSelector(
            currentLanguage = currentLanguage,
            onLanguageSelected = { viewModel.setLanguage(it) },
            context = context
        )
    }

    SettingsCategorySection(title = context.getString(R.string.theme_settings)) {
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
                ExpressiveSwitch(
                    checked = useDynamicColor,
                    onCheckedChange = { enabled ->
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

        if (!useDynamicColor) {
            var showColorPicker by remember { mutableStateOf(false) }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = context.getString(R.string.manual_color_selection),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Text(text = ">", style = MaterialTheme.typography.titleLarge)
                }
            }

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
private fun MoreSettingsContent(
    context: Context,
    onNavigateToOpenSourceLicenses: () -> Unit,
    onNavigateToDatabaseTest: () -> Unit
) {
    SettingsCategorySection(title = context.getString(R.string.feedback_title)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wj.qq.com/s2/24109109/3572/"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Browser Open Failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                Text(text = ">", style = MaterialTheme.typography.titleLarge)
            }
        }
    }

    SettingsCategorySection(title = context.getString(R.string.open_source_licenses)) {
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
                Text(text = ">", style = MaterialTheme.typography.titleLarge)
            }
        }
    }

    SettingsCategorySection(title = context.getString(R.string.database_test)) {
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
                Column {
                    Text(
                        text = context.getString(R.string.database_test),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = context.getString(R.string.database_test),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(text = ">", style = MaterialTheme.typography.titleLarge)
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    AppVersionInfo(context = context)
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                description?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
