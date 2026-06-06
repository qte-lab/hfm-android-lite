package com.chronie.homemoney.domain.usecase

import com.chronie.homemoney.data.local.PreferencesManager
import com.chronie.homemoney.domain.model.Member
import com.chronie.homemoney.domain.repository.MemberRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(username: String): Result<Member> {
        if (username.isBlank()) {
            return Result.failure(Exception("Username cannot be empty"))
        }

        return memberRepository.getOrCreateMember(username).also { result ->
            if (result.isSuccess) {
                preferencesManager.saveUsername(username)
            }
        }
    }
}
