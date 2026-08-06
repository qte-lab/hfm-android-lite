package com.chronie.homemoneylite.ui.settings

import android.app.AlertDialog
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
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.databinding.FragmentSettingsBinding
import com.chronie.homemoneylite.domain.model.SyncStatus
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
        setupClickListeners()
        setupObservers()
        setupVersion()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupClickListeners() {
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
            val formatted = formatDateByLocale(datePart)
            if (timePart.isNotEmpty()) "$formatted $timePart" else formatted
        } catch (_: Exception) {
            value
        }
    }
    // endregion

    // region 对话框
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
