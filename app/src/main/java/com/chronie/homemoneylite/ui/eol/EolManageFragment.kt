package com.chronie.homemoneylite.ui.eol

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.core.common.GpcAppUtils
import com.chronie.homemoneylite.data.remote.GpcOauthConfig
import com.chronie.homemoneylite.databinding.FragmentEolManageBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * EOL 管理页：用户可在服务停止（EOL）后查看当前到期日、绑定/解绑 GPC 账号、
 * 购买 EOL 延期或新功能移植服务。
 * 购买时以 hfm 商户身份创建支付单（返回 intentId），再以深链 gpc://pay/<intentId>
 * 拉起已安装的 GPC App 完成支付。
 */
@AndroidEntryPoint
class EolManageFragment : Fragment() {

    private var _binding: FragmentEolManageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EolManageViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEolManageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 新功能移植卡片可见性由服务端 eol-status 的 featurePortAvailable 决定（observers 内更新）
        setupListeners()
        setupObservers()
        // 进入页面即尝试刷新 EOL 状态（若已绑定）
        viewModel.refreshEolStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        val b = binding

        // 绑定 = 拉起 GPC 授权登录深链（OAuth2 授权码流程）
        b.btnBind.setOnClickListener { launchGpcOauth() }
        b.btnUnbind.setOnClickListener { viewModel.unbindAccount() }

        // EOL 延期：月数/单价由服务端决定，客户端不可选，点击直接购买
        b.btnBuyEol.setOnClickListener { viewModel.purchaseEolExtend() }

        b.btnBuyPort.setOnClickListener {
            val amt = b.editPortAmount.text.toString().toDoubleOrNull()
            if (amt == null) {
                showError(getString(R.string.eol_error_amount))
                return@setOnClickListener
            }
            viewModel.purchaseFeaturePort(amt)
        }
    }

    /**
     * 通过系统浏览器打开 GPC 授权页（OAuth2 authorize 授权码流程）。
     *
     * 注意：必须用系统浏览器（而非拉起 GPC App 内部 WebView），否则 GPC 服务端
     * 授权成功后的 302 重定向 gpc://oauth/callback?code=... 会被 GPC 的 WebView
     * 吞掉、无法回跳本应用，导致「只能打开 GPC 主页、绑定卡死」。浏览器环境下
     * 该重定向会按 BROWSABLE intent-filter 路由给本应用的 EolManageActivity。
     */
    private fun launchGpcOauth() {
        val state = java.util.UUID.randomUUID().toString().take(8)
        val url = "${GpcOauthConfig.AUTHORIZE_URL}" +
            "?client_id=${GpcOauthConfig.CLIENT_ID}" +
            "&redirect_uri=${Uri.encode(GpcOauthConfig.REDIRECT_URI)}" +
            "&response_type=code" +
            "&scope=${GpcOauthConfig.SCOPE}" +
            "&state=$state"
        val opened = GpcAppUtils.openBrowserAuthorize(requireContext(), url)
        if (!opened) {
            binding.textError.text = getString(R.string.gpc_error_no_browser)
            binding.textError.visibility = View.VISIBLE
            android.widget.Toast.makeText(
                requireContext(),
                R.string.gpc_error_no_browser,
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupObservers() {
        val b = binding
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.boundUserId.collect { uid ->
                        b.tvBindStatus.text = if (uid.isNullOrBlank()) {
                            getString(R.string.eol_not_bound)
                        } else {
                            getString(R.string.eol_bound_format, uid)
                        }
                        b.btnUnbind.isEnabled = !uid.isNullOrBlank()
                    }
                }
                launch {
                    viewModel.eolStatus.collect { st ->
                        val until = st?.eolUntil ?: viewModel.DEFAULT_EOL_UNTIL
                        b.tvEolDate.text = formatDate(until)
                        if (st?.active == true) {
                            b.tvEolHint.text = getString(R.string.eol_active_hint)
                        } else {
                            b.tvEolHint.text = getString(R.string.eol_expired_hint)
                        }
                        // 服务端决定的单价 / 月数 / 总价（客户端不可改）
                        if (st?.monthlyPrice != null && st.purchasable) {
                            val unit = st.monthlyPrice.toInt()
                            val mon = st.months
                            val total = (st.totalAmount ?: (st.monthlyPrice * mon)).toInt()
                            b.tvEolPrice.text = getString(R.string.eol_price_format, unit, mon)
                            b.tvEolTotal.text = getString(R.string.eol_total_format, total)
                            b.btnBuyEol.isEnabled = true
                        } else {
                            b.tvEolPrice.text = getString(R.string.eol_price_unavailable)
                            b.tvEolTotal.text = ""
                            b.btnBuyEol.isEnabled = false
                        }
                        // 新功能移植可见性由服务端决定
                        b.cardFeaturePort.visibility =
                            if (viewModel.featurePortAvailable) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.purchasing.collect { buying ->
                        b.btnBuyEol.isEnabled = !buying && (viewModel.eolStatus.value?.monthlyPrice != null)
                        b.btnBuyPort.isEnabled = !buying && viewModel.featurePortAvailable
                    }
                }
                launch {
                    viewModel.toast.collect { msg ->
                        msg?.let {
                            android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.clearToast()
                        }
                    }
                }
                launch {
                    viewModel.purchaseResult.collect { result ->
                        when (result) {
                            is EolManageViewModel.PurchaseResult.Success -> {
                                viewModel.clearPurchaseResult()
                                launchGpcApp(result.result.intentId)
                            }
                            is EolManageViewModel.PurchaseResult.Failure -> {
                                showError(result.message)
                                viewModel.clearPurchaseResult()
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun showError(msg: String) {
        binding.textError.text = msg
        binding.textError.visibility = View.VISIBLE
    }

    /**
     * 以深链拉起已安装的 GPC App 完成支付。
     * 若未安装，提示用户。
     */
    private fun launchGpcApp(intentId: String) {
        GpcAppUtils.launchGpc(requireContext(), "gpc://pay/$intentId") {
            binding.textError.text = getString(R.string.gpc_error_app_not_installed)
            binding.textError.visibility = View.VISIBLE
            android.widget.Toast.makeText(
                requireContext(),
                R.string.gpc_error_app_not_installed,
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun formatDate(epochMs: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        return fmt.format(Date(epochMs))
    }
}
