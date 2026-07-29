package com.chronie.homemoneylite.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.widget.Button
import android.widget.EditText
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.databinding.DialogSettingsBudgetBinding
import com.chronie.homemoneylite.databinding.FragmentSettingsBinding
import com.chronie.homemoneylite.domain.model.SyncStatus
import com.chronie.homemoneylite.ui.budget.BudgetViewModel
import com.chronie.homemoneylite.ui.common.collectWithLifecycle
import com.chronie.homemoneylite.ui.expense.formatDateByLocale
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.util.Locale

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()
    private val budgetViewModel: BudgetViewModel by viewModels()

    private var pendingAction: (() -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            pendingAction?.invoke()
        } else {
            Toast.makeText(requireContext(), R.string.permission_storage_required, Toast.LENGTH_LONG).show()
        }
        pendingAction = null
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importExpenses(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        setupClickListeners()
        setupObservers()
        setupVersion()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // region 分类切换（无动画，直接切换内容；Holo 用 LinearLayout + 两个 Button 替代 TabLayout）
    private fun setupTabs() {
        val tab0 = binding.tabsCategory.findViewById<Button>(R.id.tab_category_0)
        val tab1 = binding.tabsCategory.findViewById<Button>(R.id.tab_category_1)
        tab0.text = getString(R.string.settings_category_function)
        tab1.text = getString(R.string.settings_category_data_sync)
        tab0.setOnClickListener { switchCategory(0) }
        tab1.setOnClickListener { switchCategory(1) }
        switchCategory(0)
    }

    private fun switchCategory(position: Int) {
        val isFunction = position == 0
        binding.functionContent.visibility = if (isFunction) View.VISIBLE else View.GONE
        binding.dataSyncContent.visibility = if (isFunction) View.GONE else View.VISIBLE
        updateCategoryTabSelected(position)
    }

    private fun updateCategoryTabSelected(position: Int) {
        val ctx = requireContext()
        val tabs = listOf(
            binding.tabsCategory.findViewById<Button>(R.id.tab_category_0),
            binding.tabsCategory.findViewById<Button>(R.id.tab_category_1)
        )
        tabs.forEachIndexed { index, btn ->
            val selected = index == position
            btn.isSelected = selected
            btn.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (selected) R.color.holo_blue else R.color.text_secondary
                )
            )
        }
    }
    // endregion

    private fun setupClickListeners() {
        binding.rowBudget.setOnClickListener { showBudgetDialog() }
        binding.btnManualSync.setOnClickListener { viewModel.manualSync() }
        binding.btnExport.setOnClickListener {
            checkAndRequestPermissions { showExportChoice() }
        }
        binding.btnImport.setOnClickListener {
            checkAndRequestPermissions {
                importLauncher.launch(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            }
        }
    }

    // region 数据观察
    private fun setupObservers() {
        // AI 记录设备 ID
        collectWithLifecycle(viewModel.deviceId) { id ->
            if (id.isNotEmpty()) {
                binding.tvDeviceId.visibility = View.VISIBLE
                binding.tvDeviceId.text = id
            } else {
                binding.tvDeviceId.visibility = View.GONE
            }
        }

        // 账户状态（正常 / 已封禁）
        collectWithLifecycle(viewModel.isBanned) { banned ->
            if (banned) {
                binding.tvAccountStatus.visibility = View.VISIBLE
                binding.tvAccountStatus.text = getString(R.string.settings_ai_wallet_banned)
                binding.tvAccountStatus.setTextColor(themeColor(R.color.app_error))
                binding.tvWalletStatus.visibility = View.VISIBLE
                binding.tvWalletStatus.text = getString(R.string.settings_ai_wallet_banned)
            } else {
                binding.tvAccountStatus.visibility = View.VISIBLE
                binding.tvAccountStatus.text = getString(R.string.settings_ai_wallet_normal)
                binding.tvAccountStatus.setTextColor(themeColor(R.color.brand_primary))
                binding.tvWalletStatus.visibility = View.GONE
            }
        }

        // 钱包余额
        collectWithLifecycle(viewModel.walletBalance) { balance ->
            val symbol = getString(R.string.currency_symbol)
            binding.tvWalletBalance.text = getString(R.string.currency_format, symbol, balance)
        }

        collectWithLifecycle(viewModel.syncStatus) { status ->
            val (textRes, colorRes) = when (status) {
                SyncStatus.IDLE -> R.string.sync_status_idle to R.color.text_primary
                SyncStatus.SYNCING -> R.string.sync_status_syncing to R.color.brand_primary
                SyncStatus.SUCCESS -> R.string.sync_status_success to R.color.brand_primary
                SyncStatus.FAILED -> R.string.sync_status_failed to R.color.app_error
                SyncStatus.CONFLICT -> R.string.sync_status_conflict to R.color.app_error
            }
            binding.tvSyncStatus.setText(textRes)
            binding.tvSyncStatus.setTextColor(themeColor(colorRes))
            val syncing = status == SyncStatus.SYNCING
            binding.btnManualSync.isEnabled = !syncing
            binding.btnManualSync.setText(
                if (syncing) R.string.sync_syncing else R.string.sync_manual_trigger
            )
        }

        collectWithLifecycle(viewModel.lastSyncTime) { value ->
            binding.tvSyncLastTime.text = formatLastSync(value)
        }

        collectWithLifecycle(viewModel.pendingSyncCount) { count ->
            binding.tvSyncPending.text = count.toString()
            binding.tvSyncPending.setTextColor(
                themeColor(if (count > 0) R.color.brand_primary else R.color.text_primary)
            )
        }

        collectWithLifecycle(viewModel.syncMessage) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSyncMessage()
            }
        }

        collectWithLifecycle(viewModel.exportInProgress) { _ -> updateImportExportState() }
        collectWithLifecycle(viewModel.importInProgress) { _ -> updateImportExportState() }

        collectWithLifecycle(budgetViewModel.uiState) { state ->
            val budget = state.budget
            binding.tvBudgetStatus.visibility = View.VISIBLE
            if (budget?.isEnabled == true) {
                val symbol = getString(R.string.currency_symbol)
                binding.tvBudgetStatus.text = getString(R.string.budget_enable_feature) + ": " +
                    getString(R.string.currency_format, symbol, budget.monthlyLimit)
                binding.tvBudgetStatus.setTextColor(themeColor(R.color.brand_primary))
            } else {
                binding.tvBudgetStatus.text = getString(R.string.budget_enable_title)
                binding.tvBudgetStatus.setTextColor(themeColor(R.color.text_primary))
            }
        }
    }

    private fun updateImportExportState() {
        val exportBusy = viewModel.exportInProgress.value
        val importBusy = viewModel.importInProgress.value
        val busy = exportBusy || importBusy
        binding.btnExport.isEnabled = !busy
        binding.btnExport.text = if (exportBusy) {
            getString(R.string.export_in_progress)
        } else {
            getString(R.string.export_data)
        }
        binding.btnImport.isEnabled = !busy
        binding.btnImport.text = if (importBusy) {
            getString(R.string.import_in_progress)
        } else {
            getString(R.string.import_data)
        }
    }

    private fun formatLastSync(value: String?): String {
        if (value == null) return getString(R.string.sync_never)
        return try {
            val parts = value.split(' ')
            val datePart = parts.getOrNull(0) ?: value
            val timePart = if (parts.size > 1) parts[1] else ""
            val formatted = formatDateByLocale(datePart, localeTag())
            if (timePart.isNotEmpty()) "$formatted $timePart" else formatted
        } catch (e: Exception) {
            value
        }
    }
    // endregion

    // region 对话框
    private fun showBudgetDialog() {
        val budget = budgetViewModel.uiState.value.budget
        val dialogBinding = DialogSettingsBudgetBinding.inflate(layoutInflater)
        var enabled = budget?.isEnabled ?: false
        dialogBinding.switchBudgetEnabled.isChecked = enabled
        dialogBinding.etBudgetLimit.setText(budget?.monthlyLimit?.toString() ?: "")
        dialogBinding.etBudgetThreshold.setText(((budget?.warningThreshold ?: 0.8) * 100).toString())
        updateBudgetEditsEnabled(dialogBinding, enabled)

        dialogBinding.switchBudgetEnabled.setOnCheckedChangeListener { _, isChecked ->
            enabled = isChecked
            updateBudgetEditsEnabled(dialogBinding, enabled)
        }
        dialogBinding.btnBudgetMinus.setOnClickListener {
            adjust(dialogBinding.etBudgetLimit, -1000.0, 0.0, null)
            dialogBinding.tvBudgetError.visibility = View.GONE
        }
        dialogBinding.btnBudgetPlus.setOnClickListener {
            adjust(dialogBinding.etBudgetLimit, 1000.0, null, null)
            dialogBinding.tvBudgetError.visibility = View.GONE
        }
        dialogBinding.btnThresholdMinus.setOnClickListener {
            adjust(dialogBinding.etBudgetThreshold, -10.0, 0.0, 100.0)
            dialogBinding.tvBudgetError.visibility = View.GONE
        }
        dialogBinding.btnThresholdPlus.setOnClickListener {
            adjust(dialogBinding.etBudgetThreshold, 10.0, 0.0, 100.0)
            dialogBinding.tvBudgetError.visibility = View.GONE
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.budget_settings_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.common_save, null)
            .setNegativeButton(R.string.common_cancel, null)
            .show()

        // 校验失败时保持对话框打开
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val limit = dialogBinding.etBudgetLimit.text.toString().toDoubleOrNull()
            val threshold = dialogBinding.etBudgetThreshold.text.toString().toDoubleOrNull()
            when {
                enabled && (limit == null || limit <= 0) -> {
                    dialogBinding.tvBudgetError.setText(R.string.budget_error_invalid_limit)
                    dialogBinding.tvBudgetError.visibility = View.VISIBLE
                }
                enabled && (threshold == null || threshold < 0 || threshold > 100) -> {
                    dialogBinding.tvBudgetError.setText(R.string.budget_error_invalid_threshold)
                    dialogBinding.tvBudgetError.visibility = View.VISIBLE
                }
                else -> {
                    budgetViewModel.saveBudget(limit ?: 0.0, (threshold ?: 80.0) / 100, enabled)
                    dialog.dismiss()
                }
            }
        }
    }

    private fun adjust(
        edit: EditText,
        delta: Double,
        min: Double?,
        max: Double?
    ) {
        val current = edit.text.toString().toDoubleOrNull() ?: 0.0
        var next = current + delta
        if (min != null) next = next.coerceAtLeast(min)
        if (max != null) next = next.coerceAtMost(max)
        edit.setText(next.toString())
    }

    private fun updateBudgetEditsEnabled(
        binding: DialogSettingsBudgetBinding,
        enabled: Boolean
    ) {
        binding.etBudgetLimit.isEnabled = enabled
        binding.etBudgetThreshold.isEnabled = enabled
        binding.btnBudgetMinus.isEnabled = enabled
        binding.btnBudgetPlus.isEnabled = enabled
        binding.btnThresholdMinus.isEnabled = enabled
        binding.btnThresholdPlus.isEnabled = enabled
    }

    private fun showExportChoice() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.export_data)
            .setItems(
                arrayOf(
                    getString(R.string.settings_export_all),
                    getString(R.string.settings_export_range)
                )
            ) { _, which ->
                if (which == 0) {
                    viewModel.exportExpenses(null, null)
                } else {
                    pickExportRange()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun pickExportRange() {
        // Holo 原生日期选择器（框架 DatePickerDialog）做起止区间选择
        val startListener = android.app.DatePickerDialog.OnDateSetListener { _, y, m, d ->
            val start = LocalDate.of(y, m + 1, d)
            val endPicker = android.app.DatePickerDialog(
                requireContext(),
                { _, ey, em, ed ->
                    val end = LocalDate.of(ey, em + 1, ed)
                    viewModel.exportExpenses(start, end)
                },
                y, m, d
            )
            endPicker.setTitle(R.string.export_end_date)
            endPicker.show()
        }
        val startPicker = android.app.DatePickerDialog(
            requireContext(),
            startListener,
            2000, 0, 1
        )
        startPicker.setTitle(R.string.export_start_date)
        startPicker.show()
    }
    // endregion

    // region 权限
    private fun checkAndRequestPermissions(onGranted: () -> Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf("android.permission.READ_MEDIA_IMAGES")
        } else {
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            onGranted()
        } else {
            pendingAction = onGranted
            permissionLauncher.launch(permissions)
        }
    }
    // endregion

    // region 开源许可证（已移除：不再在设置页展示许可证声明）

    private fun setupVersion() {
        val ctx = requireContext()
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        val versionName = info.versionName
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        binding.tvVersion.text = "Version $versionName ($versionCode)"
    }

    private fun localeTag(): String = Locale.getDefault().toLanguageTag()

    private fun themeColor(@androidx.annotation.ColorRes colorRes: Int): Int =
        ContextCompat.getColor(requireContext(), colorRes)
}
