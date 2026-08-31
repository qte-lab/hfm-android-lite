package com.chronie.homemoneylite.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.data.sync.SyncScheduler
import com.chronie.homemoneylite.domain.model.SyncStatus
import com.chronie.homemoneylite.domain.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val syncScheduler: SyncScheduler,
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : ViewModel() {

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


    init {
        loadSyncInfo()
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
}
