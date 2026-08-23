package com.chronie.homemoneylite.ui.goldpigcoin

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.core.common.GpcAppUtils
import com.chronie.homemoneylite.databinding.DialogGpcPaymentBinding
import com.chronie.homemoneylite.domain.model.GpcProduct
import com.chronie.homemoneylite.domain.model.GpcProductType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 金猪币支付对话框：选择商品 → 以 hfm 商户身份创建支付单（返回 intentId）
 * → 直接以深链 gpc://pay/<intentId> 拉起已安装的 GPC App 完成支付。
 *
 * 重要：hfm 客户端不持有 GPC 用户登录态，也不确认支付。登录与支付密码
 * 由用户在已安装的 gold-pig-coin（com.pig.coin）App 内完成。
 * 若未安装 GPC App，则提示用户前往安装。
 */
@AndroidEntryPoint
class GpcProductDialogFragment : DialogFragment() {

    private var _binding: DialogGpcPaymentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoldPigCoinViewModel by viewModels()

    private var selectedType: GpcProductType = GpcProductType.EOL_EXTEND

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val context = requireContext()
        _binding = DialogGpcPaymentBinding.inflate(LayoutInflater.from(context))
        val b = binding

        updateSelectionUI()

        b.cardEol.setOnClickListener {
            selectedType = GpcProductType.EOL_EXTEND
            updateSelectionUI()
        }
        b.cardPort.setOnClickListener {
            selectedType = GpcProductType.FEATURE_PORT
            updateSelectionUI()
        }
        b.editCustomAmount.doAfterTextChanged { b.textError.visibility = android.view.View.GONE }

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.gpc_payment_title)
            .setView(b.root)
            .setPositiveButton(R.string.gpc_pay_now, null)
            .setNegativeButton(R.string.common_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onPayClicked() }
        }

        // 观察「创建支付单」结果：成功 → 拉起 GPC App；失败 → 提示错误
        // 注意：onCreateDialog 阶段 viewLifecycleOwner 尚未就绪，故用 Fragment 自身 lifecycleScope
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.createResult.collect { result ->
                        when (result) {
                            is CreateUiResult.Success -> {
                                val intentId = result.result.intentId
                                viewModel.clearCreateResult()
                                launchGpcApp(intentId)
                            }
                            is CreateUiResult.Failure -> {
                                b.textError.text = result.message
                                b.textError.visibility = android.view.View.VISIBLE
                                viewModel.clearCreateResult()
                            }
                            null -> Unit
                        }
                    }
                }
                launch {
                    viewModel.toast.collect { msg ->
                        msg?.let {
                            android.widget.Toast.makeText(
                                context,
                                it,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            viewModel.clearToast()
                        }
                    }
                }
            }
        }

        return dialog
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun updateSelectionUI() {
        val b = binding
        val ctx = requireContext()
        val selectedColor = androidx.core.content.ContextCompat.getColor(ctx, R.color.holo_blue)
        val baseColor = androidx.core.content.ContextCompat.getColor(ctx, android.R.color.transparent)
        b.cardEol.setCardBackgroundColor(
            if (selectedType == GpcProductType.EOL_EXTEND) selectedColor else baseColor
        )
        b.cardPort.setCardBackgroundColor(
            if (selectedType == GpcProductType.FEATURE_PORT) selectedColor else baseColor
        )
        b.layoutCustomAmount.visibility =
            if (selectedType == GpcProductType.FEATURE_PORT) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun onPayClicked() {
        val b = binding
        val product = viewModel.productList.value.firstOrNull { it.type == selectedType }
            ?: viewModel.productList.value.first()

        // 新功能移植：校验金额区间（与 gold-pig-coin 限额一致，500-5000）
        val custom = if (selectedType == GpcProductType.FEATURE_PORT) {
            val amt = b.editCustomAmount.text.toString().toDoubleOrNull()
            if (amt == null || amt < 500 || amt > 5000) {
                b.textError.text = getString(R.string.gpc_error_amount_range)
                b.textError.visibility = android.view.View.VISIBLE
                return
            }
            amt
        } else null

        // 以 hfm 商户身份创建支付单（免用户 token，HMAC 签名），成功后拉起 GPC App
        viewModel.createIntent(product, custom)
    }

    /**
     * 以深链拉起已安装的 GPC App 完成支付。
     * 若未安装，提示用户并保留对话框，便于复制支付链接或前往安装。
     */
    private fun launchGpcApp(intentId: String) {
        val launched = GpcAppUtils.launchGpc(requireContext(), "gpc://pay/$intentId") {
            // 未安装 GPC App：提示用户，保留对话框
            binding.textError.text = getString(R.string.gpc_error_app_not_installed)
            binding.textError.visibility = android.view.View.VISIBLE
            android.widget.Toast.makeText(
                requireContext(),
                R.string.gpc_error_app_not_installed,
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
        if (launched) {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): GpcProductDialogFragment = GpcProductDialogFragment()
    }
}
