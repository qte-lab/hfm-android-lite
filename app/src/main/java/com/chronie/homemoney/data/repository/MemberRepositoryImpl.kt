package com.chronie.homemoney.data.repository

import com.chronie.homemoney.data.mapper.MemberMapper
import com.chronie.homemoney.data.remote.api.MemberApi
import com.chronie.homemoney.data.remote.api.AvatarUpdateRequest
import com.chronie.homemoney.data.remote.dto.MemberRequest
import com.chronie.homemoney.domain.model.Member
import com.chronie.homemoney.domain.repository.MemberRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberRepositoryImpl @Inject constructor(
    private val memberApi: MemberApi
) : MemberRepository {

    override suspend fun getOrCreateMember(username: String): Result<Member> {
        return try {
            val response = memberApi.getOrCreateMember(MemberRequest(username))
            if (response.success && response.data != null) {
                Result.success(MemberMapper.toDomain(response.data))
            } else {
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMemberInfo(username: String): Result<Member> {
        return try {
            val response = memberApi.getMemberInfo(username)
            if (response.success && response.data != null) {
                Result.success(MemberMapper.toDomain(response.data))
            } else {
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAvatar(username: String, avatar: String): Result<Member> {
        return try {
            val response = memberApi.updateAvatar(username, AvatarUpdateRequest(avatar))
            if (response.success && response.data != null) {
                Result.success(MemberMapper.toDomain(response.data))
            } else {
                Result.failure(Exception(response.error ?: "更新头像失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
