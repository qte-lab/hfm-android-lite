package com.chronie.homemoneylite.domain.usecase

import com.chronie.homemoneylite.data.local.PreferencesManager
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    operator fun invoke() {
        preferencesManager.clearUsername()
    }
}
