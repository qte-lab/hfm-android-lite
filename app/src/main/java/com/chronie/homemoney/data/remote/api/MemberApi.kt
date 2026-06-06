package com.chronie.homemoney.data.remote.api

import com.chronie.homemoney.data.remote.dto.ApiResponse
import com.chronie.homemoney.data.remote.dto.HealthDto
import com.chronie.homemoney.data.remote.dto.MemberDto
import com.chronie.homemoney.data.remote.dto.MemberRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import com.google.gson.annotations.SerializedName

interface MemberApi {
    @GET("api/health/lite")
    suspend fun checkHealth(): HealthDto

    @POST("api/members/members")
    suspend fun getOrCreateMember(@Body request: MemberRequest): ApiResponse<MemberDto>

    @GET("api/members/members/{username}")
    suspend fun getMemberInfo(@Path("username") username: String): ApiResponse<MemberDto>

    @PUT("api/members/members/{username}/avatar")
    suspend fun updateAvatar(@Path("username") username: String, @Body request: AvatarUpdateRequest): ApiResponse<MemberDto>
}

data class AvatarUpdateRequest(
    @SerializedName("avatar")
    val avatar: String
)
