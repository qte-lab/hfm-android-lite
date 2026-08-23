package com.chronie.homemoneylite.ui.eol

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.chronie.homemoneylite.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * EOL 管理页宿主 Activity。
 * 同时承载两个入口：
 *  1) 设置页「金猪币服务」卡片 → 正常打开
 *  2) HealthCheckService 检测到 EOL 到期且未购买延期时 → 强制跳转至此，
 *     用户仍可在此查看状态、通过 GPC 授权登录、购买延期（不会崩溃退出）。
 *
 * 深链处理：GPC App 在用户同意授权后通过 gpc://oauth/callback?code=... 回跳本 Activity，
 * 此处拦截并交给共享的 EolManageViewModel 兑换 token 完成绑定。
 */
@AndroidEntryPoint
class EolManageActivity : AppCompatActivity() {

    private lateinit var viewModel: EolManageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eol_manage)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setTitle(R.string.eol_manage_title)
            setDisplayHomeAsUpEnabled(true)
        }

        // 与 Fragment 共享同一 ViewModel 实例（activity 作用域）
        viewModel = ViewModelProvider(this)[EolManageViewModel::class.java]

        // 拦截 GPC 授权回跳深链 gpc://oauth/callback?code=...&state=...
        handleIncomingIntent(intent)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.eolContainer, EolManageFragment())
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
                // 通过 toast 流提示（ViewModel 已收集）
                android.widget.Toast.makeText(this, "GPC 授权被拒绝", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
