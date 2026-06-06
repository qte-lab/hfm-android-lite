package com.chronie.homemoney.domain.repository

import com.chronie.homemoney.domain.model.Member

interface MemberRepository {
    suspend fun getOrCreateMember(username: String): Result<Member>
    suspend fun getMemberInfo(username: String): Result<Member>
    suspend fun updateAvatar(username: String, avatar: String): Result<Member>
}
