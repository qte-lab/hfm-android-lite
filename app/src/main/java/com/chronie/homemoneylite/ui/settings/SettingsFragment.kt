package com.chronie.homemoneylite.ui.settings

import android.content.Context
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
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.databinding.DialogSettingsApiKeyBinding
import com.chronie.homemoneylite.databinding.DialogSettingsBudgetBinding
import com.chronie.homemoneylite.databinding.FragmentSettingsBinding
import com.chronie.homemoneylite.databinding.ItemSettingsLicenseGroupBinding
import com.chronie.homemoneylite.databinding.ItemSettingsLicenseItemBinding
import com.chronie.homemoneylite.domain.model.SyncStatus
import com.chronie.homemoneylite.ui.budget.BudgetViewModel
import com.chronie.homemoneylite.ui.common.collectWithLifecycle
import com.chronie.homemoneylite.ui.expense.formatDateByLocale
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
        buildLicenses()
        setupVersion()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // region 分类切换（无动画，直接切换内容）
    private fun setupTabs() {
        binding.tabsCategory.addTab(
            binding.tabsCategory.newTab().setText(R.string.settings_category_function)
        )
        binding.tabsCategory.addTab(
            binding.tabsCategory.newTab().setText(R.string.settings_category_data_sync)
        )
        binding.tabsCategory.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = switchCategory(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        switchCategory(0)
    }

    private fun switchCategory(position: Int) {
        val isFunction = position == 0
        binding.functionContent.visibility = if (isFunction) View.VISIBLE else View.GONE
        binding.dataSyncContent.visibility = if (isFunction) View.GONE else View.VISIBLE
    }
    // endregion

    private fun setupClickListeners() {
        binding.rowBudget.setOnClickListener { showBudgetDialog() }
        binding.rowAi.setOnClickListener { showApiKeyDialog() }
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
        collectWithLifecycle(viewModel.aiApiKey) { key ->
            if (key.isNotEmpty()) {
                binding.tvAiStatus.visibility = View.VISIBLE
                binding.tvAiStatus.text = getString(R.string.api_key_set, key.take(8))
            } else {
                binding.tvAiStatus.visibility = View.GONE
            }
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
    private fun showApiKeyDialog() {
        val dialogBinding = DialogSettingsApiKeyBinding.inflate(layoutInflater)
        dialogBinding.etApiKey.setText(viewModel.aiApiKey.value)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_ai_api_key)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                viewModel.setAIApiKey(dialogBinding.etApiKey.text.toString().trim())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
        dialogBinding.tvGetApiKey.setOnClickListener {
            openLink(requireContext(), getString(R.string.settings_ai_get_key_url))
        }
    }

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

        val dialog = MaterialAlertDialogBuilder(requireContext())
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
        edit: com.google.android.material.textfield.TextInputEditText,
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
        MaterialAlertDialogBuilder(requireContext())
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
        val startPicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.export_start_date)
            .build()
        startPicker.addOnPositiveButtonClickListener { millis ->
            val start = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            val endPicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.export_end_date)
                .build()
            endPicker.addOnPositiveButtonClickListener { endMillis ->
                val end = Instant.ofEpochMilli(endMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                viewModel.exportExpenses(start, end)
            }
            endPicker.show(childFragmentManager, "export_end")
        }
        startPicker.show(childFragmentManager, "export_start")
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

    // region 开源许可证（内联，按许可证分组，可点击跳转浏览器）
    private fun buildLicenses() {
        binding.tvLicensesCount.text = getString(R.string.settings_licenses_count, libraries.size)
        val grouped = libraries.groupBy { it.license }
        val ctx = requireContext()
        grouped.forEach { (license, libs) ->
            val groupBinding = ItemSettingsLicenseGroupBinding.inflate(layoutInflater)
            groupBinding.tvLicenseName.text = license
            groupBinding.root.setOnClickListener { openLink(ctx, libs.first().licenseUrl) }
            binding.licensesContainer.addView(groupBinding.root)

            libs.forEach { lib ->
                val itemBinding = ItemSettingsLicenseItemBinding.inflate(layoutInflater)
                itemBinding.tvLibName.text = lib.name
                itemBinding.tvLibUrl.text = lib.projectUrl
                itemBinding.tvLibVersion.text = lib.version
                itemBinding.root.setOnClickListener { openLink(ctx, lib.projectUrl) }
                binding.licensesContainer.addView(itemBinding.root)
            }
        }
    }

    private fun openLink(context: Context, url: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
            // 忽略无法打开链接的异常
        }
    }
    // endregion

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

    companion object {
        /** 开源库信息（传统 XML View 版，按许可证分组展示）。 */
        private val libraries = listOf(
            LibraryInfo("Kotlin Coroutines Android", "1.6.0", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
            LibraryInfo("Kotlin Coroutines Test", "1.6.1", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
            LibraryInfo("AndroidX Desugar JDK Libs", "1.1.9", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://github.com/google/desugar_jdk_libs"),
            LibraryInfo("AndroidX Core KTX", "1.7.0", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/core"),
            LibraryInfo("AndroidX AppCompat", "1.4.2", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/appcompat"),
            LibraryInfo("Material Components", "1.5.0", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/material"),
            LibraryInfo("AndroidX ConstraintLayout", "2.1.4", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/constraintlayout"),
            LibraryInfo("AndroidX CoordinatorLayout", "1.2.0", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/coordinatorlayout"),
            LibraryInfo("AndroidX RecyclerView", "1.2.1", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/recyclerview"),
            LibraryInfo("AndroidX Core Splashscreen", "1.0.0", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/core"),
            LibraryInfo("Dagger Hilt Android", "2.41", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://dagger.dev/hilt/"),
            LibraryInfo("AndroidX Hilt Work", "1.0.0", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/hilt"),
            LibraryInfo("AndroidX Room Runtime", "2.4.3", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/room"),
            LibraryInfo("AndroidX Room KTX", "2.4.3", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/room"),
            LibraryInfo("Retrofit", "2.9.0", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://github.com/square/retrofit"),
            LibraryInfo("Retrofit Gson Converter", "2.9.0", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://github.com/square/retrofit/tree/master/retrofit-converters/gson"),
            LibraryInfo("OkHttp Logging Interceptor", "4.10.0", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://github.com/square/okhttp"),
            LibraryInfo("AndroidX Paging Runtime KTX", "3.1.1", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/paging"),
            LibraryInfo("AndroidX Navigation Fragment", "2.4.2", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/navigation"),
            LibraryInfo("Coil", "1.4.0", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://github.com/coil-kt/coil"),
            LibraryInfo("AndroidX Security Crypto", "1.0.0", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/security"),
            LibraryInfo("SQLCipher Android", "4.5.7", "BSD 3-Clause License", "https://opensource.org/licenses/BSD-3-Clause", "https://www.zetetic.net/sqlcipher/"),
            LibraryInfo("AndroidX SQLite", "2.2.0", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/sqlite"),
            LibraryInfo("AndroidX Work Runtime KTX", "2.7.1", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/work"),
            LibraryInfo("FastExcel", "0.12.15", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://github.com/dhatim/fastexcel"),
            LibraryInfo("FastExcel Reader", "0.12.15", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://github.com/dhatim/fastexcel"),
            LibraryInfo("Aalto XML", "1.3.2", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://github.com/FasterXML/aalto-xml"),
            LibraryInfo("XZ", "1.9", "Public Domain", "https://tukaani.org/xz/legal.html", "https://tukaani.org/xz/"),
            LibraryInfo("UCrop", "2.2.8", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://github.com/Yalantis/uCrop"),
            LibraryInfo("JUnit", "4.13.2", "Eclipse Public License 1.0", "https://www.eclipse.org/legal/epl-v10.html", "https://junit.org/junit4/"),
            LibraryInfo("AndroidX Test JUnit", "1.1.3", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/test"),
            LibraryInfo("AndroidX Test Espresso", "3.4.0", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://developer.android.com/jetpack/androidx/releases/test"),
            LibraryInfo("MockK", "1.12.4", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "https://mockk.io/")
        )
    }
}

private data class LibraryInfo(
    val name: String,
    val version: String,
    val license: String,
    val licenseUrl: String,
    val projectUrl: String
)
