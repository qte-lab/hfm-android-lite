package com.chronie.homemoney.ui.welcome

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chronie.homemoney.R
import com.chronie.homemoney.ui.components.ExpressiveLoadingIndicator

@Composable
fun WelcomeScreen(
    context: Context,
    onSettingsClick: () -> Unit,
    onGetStartedClick: () -> Unit,
    onNavigateToMembership: () -> Unit = {},
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val username by viewModel.username.collectAsState()

    // 监听跳过登录事件
    LaunchedEffect(Unit) {
        viewModel.skipLoginEvent.collect {
            onGetStartedClick()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is WelcomeUiState.Error) {
            // 错误会在UI中显示，3秒后自动清除
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = context.getString(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = context.getString(R.string.welcome_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(48.dp))

        when (uiState) {
            is WelcomeUiState.CheckingLogin -> {
                ExpressiveLoadingIndicator()
            }
            is WelcomeUiState.NotLoggedIn -> {
                LoginForm(
                    username = username,
                    onUsernameChange = viewModel::onUsernameChange,
                    onLoginClick = viewModel::login,
                    onSkipLogin = { viewModel.skipLogin() },
                    context = context
                )
            }
            is WelcomeUiState.Loading -> {
                ExpressiveLoadingIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = context.getString(R.string.auth_logging_in),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            is WelcomeUiState.LoggedIn -> {
                val state = uiState as WelcomeUiState.LoggedIn
                Text(
                    text = context.getString(R.string.auth_welcome_back, state.username),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onGetStartedClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(context.getString(R.string.getting_started))
                }
            }
            is WelcomeUiState.Error -> {
                val errorMessage = (uiState as WelcomeUiState.Error).message
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                LoginForm(
                    username = username,
                    onUsernameChange = viewModel::onUsernameChange,
                    onLoginClick = viewModel::login,
                    onSkipLogin = { viewModel.skipLogin() },
                    context = context
                )
            }
        }
    }
}

@Composable
private fun LoginForm(
    username: String,
    onUsernameChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onSkipLogin: () -> Unit,
    context: Context
) {
    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        label = { Text(context.getString(R.string.auth_username_label)) },
        placeholder = { Text(context.getString(R.string.auth_username_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onLoginClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = username.isNotBlank()
    ) {
        Text(context.getString(R.string.auth_login_button))
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 跳过登录按钮
    TextButton(
        onClick = onSkipLogin,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(context.getString(R.string.auth_skip_login))
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = context.getString(R.string.auth_login_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}
