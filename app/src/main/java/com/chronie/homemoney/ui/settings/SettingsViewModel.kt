package com.chronie.homemoney.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronie.homemoney.R
import com.chronie.homemoney.core.common.DeveloperMode
import com.chronie.homemoney.core.common.Language
import com.chronie.homemoney.core.common.LanguageManager
import com.chronie.homemoney.data.sync.SyncScheduler
import com.chronie.homemoney.domain.model.SyncStatus
import com.chronie.homemoney.domain.sync.DeviceInfo
import com.chronie.homemoney.domain.sync.SyncManager
import com.chronie.homemoney.domain.usecase.ExportExpensesUseCase
import com.chronie.homemoney.domain.usecase.ImportExpensesUseCase
import com.chronie.homemoney.ui.theme.PaletteStyle
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
    private val developerMode: DeveloperMode,
    private val syncManager: SyncManager,
    private val syncScheduler: SyncScheduler,
    private val exportExpensesUseCase: ExportExpensesUseCase,
    private val importExpensesUseCase: ImportExpensesUseCase,
    val checkLoginStatusUseCase: com.chronie.homemoney.domain.usecase.CheckLoginStatusUseCase,
    private val logoutUseCase: com.chronie.homemoney.domain.usecase.LogoutUseCase,
    private val memberRepository: com.chronie.homemoney.domain.repository.MemberRepository,
    private val preferencesManager: com.chronie.homemoney.data.local.PreferencesManager,
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel(), com.chronie.homemoney.domain.sync.SyncRequestCallback {

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

    val isDeveloperMode: Flow<Boolean> = developerMode.isDeveloperModeEnabled

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

    private val _currentUsername = MutableStateFlow<String?>(null)
    val currentUsername: StateFlow<String?> = _currentUsername.asStateFlow()

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    // 头像状态
    private val _avatar = MutableStateFlow<String?>(null)
    val avatar: StateFlow<String?> = _avatar.asStateFlow()

    private val _avatarLoading = MutableStateFlow(false)
    val avatarLoading: StateFlow<Boolean> = _avatarLoading.asStateFlow()

    // 设备名称
    private val _deviceName = MutableStateFlow("")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    // 同步进度状态
    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    private val _syncProgressMessage = MutableStateFlow("")
    val syncProgressMessage: StateFlow<String> = _syncProgressMessage.asStateFlow()

    private val _showSyncProgress = MutableStateFlow(false)
    val showSyncProgress: StateFlow<Boolean> = _showSyncProgress.asStateFlow()

    // 同步请求确认状态
    private val _pendingSyncRequest = MutableStateFlow<DeviceInfo?>(null)
    val pendingSyncRequest: StateFlow<DeviceInfo?> = _pendingSyncRequest.asStateFlow()

    private val _showSyncRequestDialog = MutableStateFlow(false)
    val showSyncRequestDialog: StateFlow<Boolean> = _showSyncRequestDialog.asStateFlow()

    // 服务器端被动同步进度（被搜索方）
    val serverSyncProgress: StateFlow<com.chronie.homemoney.domain.sync.SyncProgressInfo> =
        syncManager.getDeviceSyncManager().syncProgress

    // 收到的同步请求（被搜索方）
    private val _incomingSyncRequest = MutableStateFlow<com.chronie.homemoney.domain.sync.SyncRequestInfo?>(null)
    val incomingSyncRequest: StateFlow<com.chronie.homemoney.domain.sync.SyncRequestInfo?> = _incomingSyncRequest.asStateFlow()

    // 同步请求回调的continuation
    private var syncRequestContinuation: kotlin.coroutines.Continuation<Boolean>? = null

    init {
        loadSyncInfo()
        loadAIApiKey()
        loadCurrentUser()
        loadDynamicColorSettings()
        loadAvatar()
        loadDeviceName()

        // 设置同步请求回调
        syncManager.getDeviceSyncManager().setSyncRequestCallback(this)
    }

    /**
     * 同步请求回调实现
     */
    override suspend fun onSyncRequest(requestInfo: com.chronie.homemoney.domain.sync.SyncRequestInfo): Boolean {
        return kotlin.coroutines.suspendCoroutine { continuation ->
            syncRequestContinuation = continuation
            _incomingSyncRequest.value = requestInfo
        }
    }

    /**
     * 接受 incoming 同步请求
     */
    fun acceptIncomingSyncRequest() {
        syncRequestContinuation?.resume(true)
        syncRequestContinuation = null
        _incomingSyncRequest.value = null
    }

    /**
     * 拒绝 incoming 同步请求
     */
    fun rejectIncomingSyncRequest() {
        syncRequestContinuation?.resume(false)
        syncRequestContinuation = null
        _incomingSyncRequest.value = null
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _currentUsername.value = checkLoginStatusUseCase.getUsername()
        }
    }

    private fun loadAvatar() {
        viewModelScope.launch {
            // 首先从本地加载头像
            val localAvatar = preferencesManager.getAvatar()
            _avatar.value = localAvatar

            // 然后尝试从后端获取最新头像
            fetchAvatarFromBackend()
        }
    }

    private suspend fun fetchAvatarFromBackend() {
        val username = checkLoginStatusUseCase.getUsername()
        if (username.isNullOrEmpty()) return

        _avatarLoading.value = true
        try {
            // 使用memberRepository获取会员信息，包括头像
            val result = memberRepository.getMemberInfo(username)
            if (result.isSuccess) {
                val member = result.getOrNull()
                if (member != null && member.avatar != null) {
                    // 日志记录头像数据的前50个字符，以检查格式
                    android.util.Log.d("SettingsViewModel", "Fetched avatar data: ${member.avatar.take(50)}...")
                    _avatar.value = member.avatar
                    preferencesManager.saveAvatar(member.avatar)
                }
            } else {
                android.util.Log.e("SettingsViewModel", "Failed to fetch avatar from backend: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            // 网络请求失败，使用本地头像
            android.util.Log.e("SettingsViewModel", "Failed to fetch avatar from backend", e)
        } finally {
            _avatarLoading.value = false
        }
    }

    fun updateAvatar(avatarData: String) {
        viewModelScope.launch {
            _avatarLoading.value = true
            try {
                // 更新本地头像
                _avatar.value = avatarData
                preferencesManager.saveAvatar(avatarData)
                android.util.Log.d("SettingsViewModel", "Avatar saved locally")

                // 更新后端头像
                val username = checkLoginStatusUseCase.getUsername()
                if (username.isNullOrEmpty()) {
                    android.util.Log.w("SettingsViewModel", "Username is null or empty, cannot update avatar on backend")
                } else {
                    android.util.Log.d("SettingsViewModel", "Updating avatar on backend for user: $username")
                    val result = memberRepository.updateAvatar(username, avatarData)
                    if (result.isSuccess) {
                        android.util.Log.d("SettingsViewModel", "Avatar updated successfully on backend")
                    } else {
                        val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                        android.util.Log.e("SettingsViewModel", "Failed to update avatar on backend: $errorMessage")
                        throw Exception("更新头像失败: $errorMessage")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to update avatar", e)
                // 添加错误处理逻辑，显示错误消息
                _syncMessage.value = context.getString(R.string.update_avatar_failed) + ": ${e.message}"
            } finally {
                _avatarLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _currentUsername.value = null
            _avatar.value = null
            preferencesManager.clearAvatar()
            _logoutEvent.emit(Unit)
        }
    }

    fun clearSkippedLogin() {
        preferencesManager.setSkippedLogin(false)
    }

    fun setLanguage(language: Language) {
        languageManager.setLanguage(language)
    }

    fun toggleDeveloperMode() {
        viewModelScope.launch {
            developerMode.toggleDeveloperMode()
        }
    }

    fun manualSync() {
        viewModelScope.launch {
            try {
                _syncMessage.value = null
                val result = syncScheduler.manualSync()

                if (result.isSuccess) {
                    val syncResult = result.getOrNull()
                    if (syncResult?.success == true) {
                        _syncMessage.value = context.getString(R.string.device_sync_success)
                        loadSyncInfo()
                    } else {
                        _syncMessage.value = context.getString(R.string.device_sync_failed, syncResult?.error ?: "Unknown error")
                    }
                } else {
                    _syncMessage.value = context.getString(R.string.device_sync_failed, result.exceptionOrNull()?.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                _syncMessage.value = context.getString(R.string.device_sync_failed, e.message ?: "Unknown error")
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    /**
     * 搜索局域网设备
     */
    fun searchDevices(): Flow<DeviceInfo> {
        return syncManager.getDeviceSyncManager().searchDevices()
    }

    /**
     * 与设备同步
     */
    fun deviceSync(deviceInfo: DeviceInfo) {
        viewModelScope.launch {
            try {
                android.util.Log.d("SettingsViewModel", "Starting device sync with: ${deviceInfo.deviceName} at ${deviceInfo.address}")
                _syncMessage.value = null
                _showSyncProgress.value = true
                _syncProgress.value = 0f
                _syncProgressMessage.value = context.getString(R.string.device_sync_connecting, deviceInfo.deviceName)

                // 获取设备同步管理器
                val deviceSyncManager = syncManager.getDeviceSyncManager()
                android.util.Log.d("SettingsViewModel", "Got device sync manager")

                // 更新进度 - 连接中
                _syncProgress.value = 0.1f
                _syncProgressMessage.value = context.getString(R.string.device_sync_connecting, deviceInfo.deviceName)

                android.util.Log.d("SettingsViewModel", "Calling syncWithDevice...")
                val syncResult = deviceSyncManager.syncWithDevice(deviceInfo)
                android.util.Log.d("SettingsViewModel", "syncWithDevice returned: success=${syncResult.success}, error=${syncResult.error}")

                // 更新进度 - 完成或失败
                _syncProgress.value = 1f
                if (syncResult.success) {
                    _syncProgressMessage.value = context.getString(R.string.device_sync_success)
                    _syncMessage.value = context.getString(R.string.device_sync_success)
                    loadSyncInfo()
                } else {
                    _syncProgressMessage.value = context.getString(R.string.device_sync_failed, syncResult.error)
                    _syncMessage.value = context.getString(R.string.device_sync_failed, syncResult.error)
                }

                // 延迟关闭进度对话框
                kotlinx.coroutines.delay(1500)
                _showSyncProgress.value = false

            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Device sync failed", e)
                _syncProgress.value = 1f
                _syncProgressMessage.value = context.getString(R.string.device_sync_failed, e.message)
                _syncMessage.value = context.getString(R.string.device_sync_failed, e.message)
                kotlinx.coroutines.delay(1500)
                _showSyncProgress.value = false
            }
        }
    }

    /**
     * 显示同步进度
     */
    fun showSyncProgress() {
        _showSyncProgress.value = true
        _syncProgress.value = 0f
    }

    /**
     * 隐藏同步进度
     */
    fun hideSyncProgress() {
        _showSyncProgress.value = false
    }

    /**
     * 更新同步进度
     */
    fun updateSyncProgress(progress: Float, message: String) {
        _syncProgress.value = progress
        _syncProgressMessage.value = message
    }

    /**
     * 显示同步请求确认对话框
     */
    fun showSyncRequestDialog(deviceInfo: DeviceInfo) {
        _pendingSyncRequest.value = deviceInfo
        _showSyncRequestDialog.value = true
    }

    /**
     * 隐藏同步请求确认对话框
     */
    fun hideSyncRequestDialog() {
        _showSyncRequestDialog.value = false
        _pendingSyncRequest.value = null
    }

    /**
     * 接受同步请求
     */
    fun acceptSyncRequest() {
        val deviceInfo = _pendingSyncRequest.value
        if (deviceInfo != null) {
            hideSyncRequestDialog()
            deviceSync(deviceInfo)
        }
    }

    /**
     * 拒绝同步请求
     */
    fun rejectSyncRequest() {
        hideSyncRequestDialog()
    }

    /**
     * 清除服务器端同步进度
     */
    fun clearServerSyncProgress() {
        syncManager.getDeviceSyncManager().clearSyncProgress()
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

    // 加载设备名称
    private fun loadDeviceName() {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("sync_prefs", android.content.Context.MODE_PRIVATE)
            _deviceName.value = prefs.getString("device_custom_name", android.os.Build.MODEL ?: "Android Device") ?: "Android Device"
        }
    }

    // 设置设备名称
    fun setDeviceName(name: String) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("sync_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("device_custom_name", name).apply()
            _deviceName.value = name
            _syncMessage.value = context.getString(R.string.device_name_updated)
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
