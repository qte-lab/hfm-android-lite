package com.chronie.homemoneylite.ui.eol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronie.homemoneylite.data.remote.GpcAccountManager
import com.chronie.homemoneylite.domain.model.CreateIntentResult
import com.chronie.homemoneylite.domain.model.EolStatus
import com.chronie.homemoneylite.domain.model.GpcProduct
import com.chronie.homemoneylite.domain.model.GpcProductType
import com.chronie.homemoneylite.domain.repository.GoldPigCoinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * EOL 管理页 ViewModel。
 * 职责：
 *  - 维护 GPC 账号绑定/解绑状态（GpcAccountManager）
 *  - 查询当前 hfm 用户的 EOL 延期状态（getEolStatus）
 *  - 发起购买：EOL 延期（按月，带 months + hfmUserId）/ 新功能移植（自定义金额）
 * 拿到 intentId 后由 UI 以深链 gpc://pay/<intentId> 拉起 GPC App 完成支付。
 */
@HiltViewModel
class EolManageViewModel @Inject constructor(
    private val repository: GoldPigCoinRepository,
    private val accountManager: GpcAccountManager,
    private val products: List<GpcProduct>
) : ViewModel() {

    /** 默认 EOL 截止日（北京时间 2026-08-31 23:59:59 → epoch ms） */
    val DEFAULT_EOL_UNTIL: Long = 1756665599000L // 2026-08-31T15:59:59Z

    private val _boundUserId = MutableStateFlow<String?>(accountManager.getBoundUserId())
    val boundUserId: StateFlow<String?> = _boundUserId.asStateFlow()

    private val _eolStatus = MutableStateFlow<EolStatus?>(null)
    val eolStatus: StateFlow<EolStatus?> = _eolStatus.asStateFlow()

    private val _loadingStatus = MutableStateFlow(false)
    val loadingStatus: StateFlow<Boolean> = _loadingStatus.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _purchaseResult = MutableStateFlow<PurchaseResult?>(null)
    val purchaseResult: StateFlow<PurchaseResult?> = _purchaseResult.asStateFlow()

    private val _purchasing = MutableStateFlow(false)
    val purchasing: StateFlow<Boolean> = _purchasing.asStateFlow()

    /** 新功能移植当前是否可购买（以服务端 eol-status 的 featurePortAvailable 为准；本地仅作兜底判断） */
    val featurePortAvailable: Boolean
        get() = _eolStatus.value?.featurePortAvailable
            ?: (LocalDate.now().year < 2027)

    fun clearToast() { _toast.value = null }
    fun clearPurchaseResult() { _purchaseResult.value = null }

    /**
     * 通过 OAuth 授权结果完成绑定（保存 userId / username / access_token）。
     * @return 绑定成功后是否需要立即刷新 EOL 状态（始终 true，由调用方决定）
     */
    fun bindOAuth(result: com.chronie.homemoneylite.domain.model.GpcOAuthResult) {
        accountManager.bindOAuth(result.userId, result.username, result.accessToken)
        _boundUserId.value = result.userId
        _toast.value = "已通过 GPC 授权登录"
    }

    /** 兼容：手动绑定（仅演示/兜底用） */
    fun bindAccount(userId: String) {
        val trimmed = userId.trim()
        if (trimmed.isEmpty()) {
            _toast.value = "请输入 GPC 用户标识"
            return
        }
        accountManager.bind(trimmed)
        _boundUserId.value = trimmed
        _toast.value = "已绑定 GPC 账号"
        refreshEolStatus()
    }

    /**
     * 处理 GPC 授权回跳（gpc://oauth/callback?code=...）。
     * 用授权码兑换 access_token + 用户标识，完成绑定并刷新 EOL 状态。
     */
    fun handleOauthCallback(code: String) {
        if (code.isBlank()) {
            _toast.value = "授权失败：缺少授权码"
            return
        }
        viewModelScope.launch {
            _loadingStatus.value = true
            val result = repository.exchangeCode(code)
            _loadingStatus.value = false
            result.onSuccess { oauth ->
                bindOAuth(oauth)
                refreshEolStatus()
            }.onFailure {
                _toast.value = it.message ?: "GPC 授权登录失败"
            }
        }
    }

    /** 解绑 GPC 账号 */
    fun unbindAccount() {
        accountManager.unbind()
        _boundUserId.value = null
        _eolStatus.value = null
        _toast.value = "已解除绑定"
    }

    /** 查询当前绑定用户的 EOL 延期状态（无绑定则跳过） */
    fun refreshEolStatus() {
        val uid = _boundUserId.value ?: return
        viewModelScope.launch {
            _loadingStatus.value = true
            val result = repository.getEolStatus(uid)
            _loadingStatus.value = false
            result.onSuccess { st ->
                _eolStatus.value = st
                accountManager.cacheEolUntil(if (st.active) st.eolUntil else null)
            }.onFailure {
                _toast.value = it.message ?: "查询 EOL 状态失败"
            }
        }
    }

    /**
     * 发起 EOL 延期购买。
     * 月数（months）与单价完全由 GPC 服务端决定（eol-status 返回），客户端不可修改。
     * 此处仅把服务端给出的 months 透传给 create-intent，金额由服务端按梯度重新计算。
     */
    fun purchaseEolExtend() {
        val product = products.firstOrNull { it.type == GpcProductType.EOL_EXTEND } ?: return
        val uid = _boundUserId.value
        if (uid.isNullOrBlank()) {
            _toast.value = "请先通过 GPC 授权登录"
            return
        }
        val st = _eolStatus.value
        if (st == null || !st.purchasable) {
            _toast.value = "当前无法购买 EOL 延期"
            return
        }
        val mon = st.months.coerceAtLeast(1)
        // amount 仅作占位（服务端会按梯度重新计算，忽略客户端值）
        val placeholder = st.totalAmount ?: (st.monthlyPrice ?: product.amount)
        viewModelScope.launch {
            _purchasing.value = true
            val result = repository.createIntent(
                amount = placeholder,
                orderNo = product.orderNo(),
                description = product.description,
                months = mon,
                hfmUserId = uid
            )
            _purchasing.value = false
            handleCreateResult(result)
        }
    }

    /**
     * 发起新功能移植购买（自定义金额）。
     * @param customAmount 自定义金额（500-5000）
     */
    fun purchaseFeaturePort(customAmount: Double) {
        val product = products.firstOrNull { it.type == GpcProductType.FEATURE_PORT } ?: return
        if (!featurePortAvailable) {
            _toast.value = "新功能移植服务已于 2027 年停止销售"
            return
        }
        if (customAmount < 500 || customAmount > 5000) {
            _toast.value = "金额需在 500-5000 GPC 之间"
            return
        }
        viewModelScope.launch {
            _purchasing.value = true
            val result = repository.createIntent(
                amount = customAmount,
                orderNo = product.orderNo(),
                description = product.description,
                months = 1,
                hfmUserId = null
            )
            _purchasing.value = false
            handleCreateResult(result)
        }
    }

    private fun handleCreateResult(result: Result<CreateIntentResult>) {
        result.onSuccess { r ->
            _purchaseResult.value = PurchaseResult.Success(r)
        }.onFailure {
            _purchaseResult.value = PurchaseResult.Failure(it.message ?: "创建支付单失败")
        }
    }

    /** 购买结果封装 */
    sealed interface PurchaseResult {
        data class Success(val result: CreateIntentResult) : PurchaseResult
        data class Failure(val message: String) : PurchaseResult
    }
}
