package com.chronie.homemoneylite.domain.repository

import com.chronie.homemoneylite.domain.model.Member

interface MemberRepository {
    suspend fun getOrCreateMember(username: String): Result<Member>
    suspend fun getMemberInfo(username: String): Result<Member>
    suspend fun updateAvatar(username: String, avatar: String): Result<Member>
}
