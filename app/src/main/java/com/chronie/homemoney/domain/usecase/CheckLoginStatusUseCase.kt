package com.chronie.homemoney.domain.usecase

import com.chronie.homemoney.data.local.PreferencesManager
import javax.inject.Inject

class CheckLoginStatusUseCase @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    operator fun invoke(): Boolean {
        return preferencesManager.shouldSkipWelcome()
    }

    fun isLoggedIn(): Boolean {
        return preferencesManager.isLoggedIn()
    }

    fun getUsername(): String? {
        return preferencesManager.getUsername()
    }
}
