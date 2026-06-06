package com.chronie.homemoney.ui.membership

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronie.homemoney.data.local.PreferencesManager
import com.chronie.homemoney.domain.model.Member
import com.chronie.homemoney.domain.usecase.CheckLoginStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MembershipViewModel @Inject constructor(
    private val checkLoginStatusUseCase: CheckLoginStatusUseCase,
    private val preferencesManager: PreferencesManager,
    private val memberRepository: com.chronie.homemoney.domain.repository.MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MembershipUiState>(MembershipUiState.Loading)
    val uiState: StateFlow<MembershipUiState> = _uiState.asStateFlow()

    init {
        loadMembershipData()
    }

    fun loadMembershipData() {
        viewModelScope.launch {
            _uiState.value = MembershipUiState.Loading

            try {
                val username = checkLoginStatusUseCase.getUsername()
                if (username.isNullOrEmpty()) {
                    _uiState.value = MembershipUiState.Error("未登录")
                    return@launch
                }

                // 获取会员信息
                val memberResult = memberRepository.getMemberInfo(username)
                if (memberResult.isSuccess) {
                    _uiState.value = MembershipUiState.Success(
                        member = memberResult.getOrNull()
                    )
                } else {
                    _uiState.value = MembershipUiState.Error(
                        memberResult.exceptionOrNull()?.message ?: "获取会员信息失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = MembershipUiState.Error(
                    e.message ?: "加载失败"
                )
            }
        }
    }

    fun updateAvatar(avatar: String) {
        viewModelScope.launch {
            try {
                val username = checkLoginStatusUseCase.getUsername()
                if (username.isNullOrEmpty()) {
                    _uiState.value = MembershipUiState.Error("未登录")
                    return@launch
                }

                val result = memberRepository.updateAvatar(username, avatar)
                if (result.isSuccess) {
                    val currentState = _uiState.value
                    if (currentState is MembershipUiState.Success) {
                        _uiState.value = currentState.copy(member = result.getOrNull())
                    }
                } else {
                    _uiState.value = MembershipUiState.Error(
                        result.exceptionOrNull()?.message ?: "更新头像失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = MembershipUiState.Error(
                    e.message ?: "更新头像失败"
                )
            }
        }
    }

    fun logout(onLogout: () -> Unit) {
        preferencesManager.clearUsername()
        onLogout()
    }
}

sealed class MembershipUiState {
    object Loading : MembershipUiState()
    data class Success(
        val member: Member?
    ) : MembershipUiState()
    data class Error(val message: String) : MembershipUiState()
}
