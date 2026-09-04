package com.chronie.homemoneylite.ui.eol

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import android.widget.Toolbar
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.service.HealthCheckService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * EOL 管理页宿主 Activity。
 * 同时承载两个入口：
 *  1) 设置页「金猪币服务」卡片 → 正常打开（可返回退出）
 *  2) HealthCheckService 检测到 EOL 到期且未购买延期时 → 携带 EXTRA_FORCED 强制跳转至此，
 *     处于「强制模式」：用户必须停留在此页完成绑定/购买延期，返回键与关闭按钮均被拦截，
 *     直到成功购买延期（服务端确认 active 且未到期）后才允许离开。
 *
 * 深链处理：GPC App 在用户同意授权后通过 gpc://oauth/callback?code=... 回跳本 Activity，
 * 此处拦截并交给共享的 EolManageViewModel 兑换 token 完成绑定。
 */
@AndroidEntryPoint
class EolManageActivity : FragmentActivity() {

    @Inject
    lateinit var healthCheckService: HealthCheckService

    private lateinit var viewModel: EolManageViewModel

    /** 是否处于「强制模式」（由服务到期检查跳转而来，必须停留购买） */
    private var forcedMode = false

    /** 在强制模式下，是否已成功购买延期、允许离开 */
    @Volatile
    private var canLeave = false

    /** observeEolStatus 是否已启动收集（防止 onNewIntent 重复启动） */
    private var observing = false

    /** 系统返回键拦截回调：强制模式下且尚未购买成功时拦截返回 */
    private lateinit var backCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eol_manage)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        // 与 Fragment 共享同一 ViewModel 实例（activity 作用域）
        viewModel = ViewModelProvider(this)[EolManageViewModel::class.java]

        forcedMode = intent?.getBooleanExtra(EXTRA_FORCED, false) ?: false

        // 原生 Toolbar（无 AppCompat 委托）：手动设置标题与返回键
        toolbar.setTitle(R.string.eol_manage_title)
        if (forcedMode) {
            // 强制模式下隐藏返回箭头，避免用户误以为可退出
            toolbar.navigationIcon = null
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_nav_back)
            toolbar.setNavigationOnClickListener { handleNavigateUp() }
        }

        backCallback = object : OnBackPressedCallback(forcedMode) {
            override fun handleOnBackPressed() {
                if (forcedMode && !canLeave) {
                    // 强制模式且未购买成功：拦截返回，提示用户需先购买延期
                    showForcedHint()
                } else {
                    // 非强制模式或已放行：恢复默认返回行为
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        // 拦截 GPC 授权回跳深链 gpc://oauth/callback?code=...&state=...
        handleIncomingIntent(intent)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.eolContainer, EolManageFragment())
            }
        }

        applyForcedMode()
    }

    /** 进入/恢复「强制模式」：通知 HealthCheckService 本页已在前台，并启动状态观察 */
    private fun applyForcedMode() {
        if (!forcedMode) return
        healthCheckService.eolForcedActivityVisible = true
        backCallback.isEnabled = true
        observeEolStatus()
    }

    /** 强制模式下：一旦服务端确认已购买延期且仍在有效期，即放行并可自动退出 */
    private fun observeEolStatus() {
        if (observing) return
        observing = true
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eolStatus.collect { st ->
                    if (st?.active == true && st.eolUntil > healthCheckService.serverNowMillis()) {
                        canLeave = true
                        // 购买成功后自动离开强制页，回到正常使用
                        finish()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        // 若本 Activity 已在前台（例如从设置页打开 EOL 后服务到期触发强制跳转），
        // 重新拉起时通过 onNewIntent 带上 EXTRA_FORCED=true，需要重新进入强制模式，
        // 否则强制保持不会生效。
        val reForced = intent?.getBooleanExtra(EXTRA_FORCED, false) ?: false
        if (reForced) {
            forcedMode = true
            applyForcedMode()
        }
        handleIncomingIntent(intent)
    }

    @SuppressLint("IntentReset")
    private fun handleIncomingIntent(intent: Intent?) {
        val uri: Uri? = intent?.data
        if (uri != null && uri.scheme == "gpc" && uri.host == "oauth" && uri.path == "/callback") {
            val code = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")
            if (!code.isNullOrBlank()) {
                viewModel.handleOauthCallback(code)
            } else if (!error.isNullOrBlank()) {
                viewModel.clearToast()
                android.widget.Toast.makeText(this, "GPC 授权被拒绝", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleNavigateUp() {
        if (forcedMode && !canLeave) {
            showForcedHint()
            return
        }
        finish()
    }

    override fun onDestroy() {
        // 通知 HealthCheckService 本强制页已不在前台，下次检查可重新拉起（实现强制保持）
        if (forcedMode) {
            healthCheckService.eolForcedActivityVisible = false
        }
        super.onDestroy()
    }

    private fun showForcedHint() {
        android.widget.Toast.makeText(
            this,
            R.string.eol_forced_stay_hint,
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    companion object {
        /** 强制模式标记：由 HealthCheckService 跳转时传入 */
        const val EXTRA_FORCED = "extra_eol_forced"
    }
}
