package com.chronie.homemoneylite.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.core.common.Language
import com.chronie.homemoneylite.core.common.LanguageManager
import com.chronie.homemoneylite.data.sync.SyncScheduler
import com.chronie.homemoneylite.domain.model.SyncStatus
import com.chronie.homemoneylite.domain.sync.SyncManager
import com.chronie.homemoneylite.domain.usecase.ExportExpensesUseCase
import com.chronie.homemoneylite.domain.usecase.ImportExpensesUseCase
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
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

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
    }

    fun setLanguage(language: Language) {
        languageManager.setLanguage(language)
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun manualSync() {
        viewModelScope.launch {
            try {
                val result = syncScheduler.manualSync()
                _syncMessage.value = if (result.isSuccess) {
                    context.getString(R.string.sync_status_success)
                } else {
                    context.getString(R.string.sync_status_failed) + ": " + (result.exceptionOrNull()?.message ?: "Unknown error")
                }
                loadSyncInfo()
            } catch (e: Exception) {
                _syncMessage.value = context.getString(R.string.sync_status_failed) + ": " + e.message
            }
        }
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
