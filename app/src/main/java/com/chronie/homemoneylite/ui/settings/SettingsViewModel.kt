package com.chronie.homemoneylite.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.core.common.LanguageManager
import com.chronie.homemoneylite.data.sync.SyncScheduler
import com.chronie.homemoneylite.domain.model.SyncStatus
import com.chronie.homemoneylite.domain.sync.SyncManager
import com.chronie.homemoneylite.domain.usecase.ExportExpensesUseCase
import com.chronie.homemoneylite.domain.usecase.ImportExpensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val walletRepository: com.chronie.homemoneylite.data.repository.WalletRepository
) : ViewModel() {

    private val _aiServerUrl = MutableStateFlow("")
    val aiServerUrl: StateFlow<String> = _aiServerUrl.asStateFlow()

    /** 钱包余额 */
    private val _walletBalance = MutableStateFlow(0.0)
    val walletBalance: StateFlow<Double> = _walletBalance.asStateFlow()

    /** 是否被封禁 */
    private val _isBanned = MutableStateFlow(false)
    val isBanned: StateFlow<Boolean> = _isBanned.asStateFlow()

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
        loadAIServerUrl()
        loadWalletInfo()
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

    fun setAIServerUrl(url: String) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("ai_settings", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("ollama_base_url", url).apply()
            _aiServerUrl.value = url
            _syncMessage.value = context.getString(R.string.settings_ai_server_saved)
        }
    }

    private fun loadAIServerUrl() {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("ai_settings", android.content.Context.MODE_PRIVATE)
            _aiServerUrl.value = prefs.getString("ollama_base_url", "") ?: ""
        }
    }

    private fun loadWalletInfo() {
        viewModelScope.launch {
            try {
                val wallet = walletRepository.getWalletInfo()
                _walletBalance.value = wallet.balance
                _isBanned.value = wallet.isBanned
            } catch (e: Exception) {
                // 钱包服务未启动或网络不通时忽略，界面显示默认值
                android.util.Log.w("SettingsViewModel", "Failed to load wallet info", e)
            }
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
