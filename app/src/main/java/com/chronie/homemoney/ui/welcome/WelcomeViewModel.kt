package com.chronie.homemoney.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronie.homemoney.data.local.PreferencesManager
import com.chronie.homemoney.domain.usecase.CheckLoginStatusUseCase
import com.chronie.homemoney.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val checkLoginStatusUseCase: CheckLoginStatusUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<WelcomeUiState>(WelcomeUiState.CheckingLogin)
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _skipLoginEvent = MutableSharedFlow<Unit>()
    val skipLoginEvent: SharedFlow<Unit> = _skipLoginEvent.asSharedFlow()

    init {
        checkLoginStatus()
    }

    fun checkLoginStatus() {
        viewModelScope.launch {
            val shouldSkipWelcome = checkLoginStatusUseCase()
            if (shouldSkipWelcome) {
                val username = checkLoginStatusUseCase.getUsername()
                if (!username.isNullOrEmpty()) {
                    _uiState.value = WelcomeUiState.LoggedIn(username)
                } else {
                    _skipLoginEvent.emit(Unit)
                }
            } else {
                _uiState.value = WelcomeUiState.NotLoggedIn
            }
        }
    }

    fun onUsernameChange(newUsername: String) {
        _username.value = newUsername
    }

    fun login() {
        if (_username.value.isBlank()) {
            _uiState.value = WelcomeUiState.Error("用户名不能为空")
            return
        }

        viewModelScope.launch {
            _uiState.value = WelcomeUiState.Loading

            // 执行登录
            loginUseCase(_username.value.trim())
                .onSuccess { member ->
                    _uiState.value = WelcomeUiState.LoggedIn(
                        username = member.username
                    )
                }
                .onFailure { error ->
                    _uiState.value = WelcomeUiState.Error(
                        error.message ?: "登录失败"
                    )
                }
        }
    }

    fun skipLogin() {
        preferencesManager.setSkippedLogin(true)
        viewModelScope.launch {
            _skipLoginEvent.emit(Unit)
        }
    }

    fun clearError() {
        if (_uiState.value is WelcomeUiState.Error) {
            _uiState.value = WelcomeUiState.NotLoggedIn
        }
    }
}

sealed class WelcomeUiState {
    object CheckingLogin : WelcomeUiState()
    object NotLoggedIn : WelcomeUiState()
    object Loading : WelcomeUiState()
    data class LoggedIn(val username: String) : WelcomeUiState()
    data class Error(val message: String) : WelcomeUiState()
}
