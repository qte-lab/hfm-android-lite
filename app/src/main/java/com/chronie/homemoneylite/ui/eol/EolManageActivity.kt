package com.chronie.homemoneylite.ui.eol

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
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
class EolManageActivity : AppCompatActivity() {

    @Inject
    lateinit var healthCheckService: HealthCheckService

    private lateinit var viewModel: EolManageViewModel

    /** 是否处于「强制模式」（由服务到期检查跳转而来，必须停留购买） */
    private var forcedMode = false

    /** 在强制模式下，是否已成功购买延期、允许离开 */
    @Volatile
    private var canLeave = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eol_manage)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // 与 Fragment 共享同一 ViewModel 实例（activity 作用域）
        viewModel = ViewModelProvider(this)[EolManageViewModel::class.java]

        forcedMode = intent?.getBooleanExtra(EXTRA_FORCED, false) ?: false

        supportActionBar?.apply {
            setTitle(R.string.eol_manage_title)
            // 强制模式下隐藏返回箭头，避免用户误以为可退出
            setDisplayHomeAsUpEnabled(!forcedMode)
        }

        // 拦截 GPC 授权回跳深链 gpc://oauth/callback?code=...&state=...
        handleIncomingIntent(intent)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.eolContainer, EolManageFragment())
            }
        }

        if (forcedMode) {
            observeEolStatus()
        }
    }

    /** 强制模式下：一旦服务端确认已购买延期且仍在有效期，即放行并可自动退出 */
    private fun observeEolStatus() {
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

    /**
     * 拦截系统返回键。
     *  - 强制模式且尚未购买成功：阻止退出，提示用户需先购买延期。
     *  - 其它情况：正常返回。
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (forcedMode && !canLeave) {
            showForcedHint()
            return
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        if (forcedMode && !canLeave) {
            showForcedHint()
            return true
        }
        finish()
        return true
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
