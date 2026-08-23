package com.chronie.homemoneylite.ui.goldpigcoin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronie.homemoneylite.domain.model.CreateIntentResult
import com.chronie.homemoneylite.domain.model.GpcProduct
import com.chronie.homemoneylite.domain.model.GpcProductType
import com.chronie.homemoneylite.domain.repository.GoldPigCoinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 金猪币（GPC）跨应用支付 ViewModel。
 * 职责：展示可购买商品、以 hfm 商户身份创建支付单（返回 intentId）。
 * 注意：hfm 客户端不持有 GPC 用户登录态，也不确认支付——
 * 拿到 intentId 后由 GpcProductDialogFragment 以深链 gpc://pay/<intentId>
 * 拉起已安装的 GPC App，登录与支付密码由用户在 GPC App 内完成。
 */
@HiltViewModel
class GoldPigCoinViewModel @Inject constructor(
    private val repository: GoldPigCoinRepository,
    private val products: List<GpcProduct>
) : ViewModel() {

    /** 可购买商品目录 */
    val productList: StateFlow<List<GpcProduct>> =
        MutableStateFlow(products).asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    /** 发起支付单的结果（拿到 intentId 后由 UI 拉起 GPC App） */
    private val _createResult = MutableStateFlow<CreateUiResult?>(null)
    val createResult: StateFlow<CreateUiResult?> = _createResult.asStateFlow()

    fun clearToast() { _toast.value = null }
    fun clearCreateResult() { _createResult.value = null }

    /**
     * 以 hfm 商户身份创建支付单。
     * 对 EOL_EXTEND 使用固定金额（商品.amount）；对 FEATURE_PORT 使用调用方传入的自定义金额。
     * @param product 选中的商品
     * @param customAmount 自定义金额（仅 FEATURE_PORT 使用）
     */
    fun createIntent(product: GpcProduct, customAmount: Double?) {
        val amount = if (product.type == GpcProductType.FEATURE_PORT) {
            customAmount ?: product.amount
        } else {
            product.amount
        }
        viewModelScope.launch {
            _loading.value = true
            val result = repository.createIntent(
                amount = amount,
                orderNo = product.orderNo(),
                description = product.description
            )
            _loading.value = false
            result.onSuccess { r ->
                _createResult.value = CreateUiResult.Success(r)
            }.onFailure {
                _createResult.value = CreateUiResult.Failure(it.message ?: "创建支付单失败")
            }
        }
    }
}

/** 发起支付单的 UI 结果封装 */
sealed interface CreateUiResult {
    data class Success(val result: CreateIntentResult) : CreateUiResult
    data class Failure(val message: String) : CreateUiResult
}
