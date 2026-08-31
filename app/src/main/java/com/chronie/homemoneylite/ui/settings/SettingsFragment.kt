package com.chronie.homemoneylite.ui.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.databinding.FragmentSettingsBinding
import com.chronie.homemoneylite.domain.model.SyncStatus
import com.chronie.homemoneylite.ui.common.collectWithLifecycle
import com.chronie.homemoneylite.ui.expense.formatDateByLocale
import com.chronie.homemoneylite.ui.eol.EolManageActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

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
        binding.btnOpenGoldPigCoin.setOnClickListener {
            startActivity(Intent(requireContext(), EolManageActivity::class.java))
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
