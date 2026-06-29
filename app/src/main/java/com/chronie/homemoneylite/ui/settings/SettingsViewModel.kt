package com.chronie.homemoneylite.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.core.common.Language
import com.chronie.homemoneylite.core.common.LanguageManager
import com.chronie.homemoneylite.data.sync.SyncScheduler
import com.chronie.homemoneylite.domain.model.SyncStatus
import com.chronie.homemoneylite.domain.sync.DeviceInfo
import com.chronie.homemoneylite.domain.sync.SyncManager
import com.chronie.homemoneylite.domain.usecase.ExportExpensesUseCase
import com.chronie.homemoneylite.domain.usecase.ImportExpensesUseCase
import com.chronie.homemoneylite.ui.theme.PaletteStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val languageManager: LanguageManager,
    private val syncManager: SyncManager,
    private val syncScheduler: SyncScheduler,
    private val exportExpensesUseCase: ExportExpensesUseCase,
    private val importExpensesUseCase: ImportExpensesUseCase,
    private val preferencesManager: com.chronie.homemoneylite.data.local.PreferencesManager,
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel(), com.chronie.homemoneylite.domain.sync.SyncRequestCallback {

    // 动态颜色开关状态
    private val _useDynamicColor = MutableStateFlow(true)
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor.asStateFlow()

    // 手动选择的主色调
    private val _primaryColor = MutableStateFlow(0xFF6750A4.toInt()) // 默认紫色
    val primaryColor: StateFlow<Int> = _primaryColor.asStateFlow()

    // 调色板样式
    private val _paletteStyle = MutableStateFlow(PaletteStyle.Expressive)
    val paletteStyle: StateFlow<PaletteStyle> = _paletteStyle.asStateFlow()

    val currentLanguage: StateFlow<Language> = languageManager.currentLanguage

    private val _aiApiKey = MutableStateFlow("")
    val aiApiKey: StateFlow<String> = _aiApiKey.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> = syncManager.observeSyncStatus()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SyncStatus.IDLE
        )

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _exportInProgress = MutableStateFlow(false)
    val exportInProgress: StateFlow<Boolean> = _exportInProgress.asStateFlow()

    private val _importInProgress = MutableStateFlow(false)
    val importInProgress: StateFlow<Boolean> = _importInProgress.asStateFlow()

    init {
        loadSyncInfo()
        loadAIApiKey()
        loadDynamicColorSettings()
    }

    fun setAIApiKey(apiKey: String) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("ai_settings", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("siliconflow_api_key", apiKey).apply()
            _aiApiKey.value = apiKey
            _syncMessage.value = context.getString(R.string.settings_ai_api_key_saved)
        }
    }

    private fun loadAIApiKey() {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("ai_settings", android.content.Context.MODE_PRIVATE)
            _aiApiKey.value = prefs.getString("siliconflow_api_key", "") ?: ""
        }
    }

    private fun loadSyncInfo() {
        viewModelScope.launch {
            // 加载最后同步时间
            val lastSync = syncManager.getLastSyncTime()
            _lastSyncTime.value = if (lastSync != null) {
                formatTimestamp(lastSync)
            } else {
                null
            }

            // 加载待同步项数量
            _pendingSyncCount.value = syncManager.getPendingSyncCount()
        }
    }

    // 加载动态颜色设置
    private fun loadDynamicColorSettings() {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("theme_settings", android.content.Context.MODE_PRIVATE)
            _useDynamicColor.value = prefs.getBoolean("use_dynamic_color", true)
            _primaryColor.value = prefs.getInt("primary_color", 0xFF6750A4.toInt())
            val paletteStyleValue = prefs.getInt("palette_style", PaletteStyle.Expressive.ordinal)
            val paletteStyle = PaletteStyle.values().getOrElse(paletteStyleValue) { PaletteStyle.Expressive }
            _paletteStyle.value = paletteStyle
        }
    }

    // 切换动态颜色开关
    fun toggleDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("theme_settings", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("use_dynamic_color", enabled).apply()
            _useDynamicColor.value = enabled
            _syncMessage.value = context.getString(if (enabled) R.string.dynamic_color_enabled else R.string.dynamic_color_disabled)
        }
    }

    // 设置手动颜色
    fun setPrimaryColor(color: Int) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("theme_settings", android.content.Context.MODE_PRIVATE)
            prefs.edit().putInt("primary_color", color).apply()
            _primaryColor.value = color
            _syncMessage.value = context.getString(R.string.primary_color_updated)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun exportExpenses(startDate: LocalDate? = null, endDate: LocalDate? = null) {
        viewModelScope.launch {
            try {
                _exportInProgress.value = true
                _syncMessage.value = context.getString(R.string.export_in_progress)

                val result = exportExpensesUseCase(startDate, endDate)

                if (result.isSuccess) {
                    val filePath = result.getOrNull()
                    _syncMessage.value = context.getString(R.string.export_success, filePath)
                } else {
                    _syncMessage.value = context.getString(
                        R.string.export_failed,
                        result.exceptionOrNull()?.message ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                _syncMessage.value = context.getString(R.string.export_failed, e.message)
            } finally {
                _exportInProgress.value = false
            }
        }
    }

    fun importExpenses(uri: Uri) {
        viewModelScope.launch {
            try {
                _importInProgress.value = true
                _syncMessage.value = context.getString(R.string.import_in_progress)

                val result = importExpensesUseCase(uri)

                if (result.isSuccess) {
                    val importResult = result.getOrNull()!!
                    _syncMessage.value = context.getString(
                        R.string.import_success,
                        importResult.successCount
                    )

                    // 如果有失败的记录，显示错误信息
                    if (importResult.failedCount > 0) {
                        android.util.Log.w("ImportExpenses", "Failed to import ${importResult.failedCount} records")
                        importResult.errors.forEach { error ->
                            android.util.Log.w("ImportExpenses", error)
                        }
                    }
                } else {
                    _syncMessage.value = context.getString(
                        R.string.import_failed,
                        result.exceptionOrNull()?.message ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                _syncMessage.value = context.getString(R.string.import_failed, e.message)
            } finally {
                _importInProgress.value = false
            }
        }
    }
}
