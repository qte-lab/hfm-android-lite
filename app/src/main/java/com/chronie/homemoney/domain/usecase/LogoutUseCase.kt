package com.chronie.homemoney.domain.usecase

import com.chronie.homemoney.data.local.PreferencesManager
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    operator fun invoke() {
        preferencesManager.clearUsername()
    }
}
