package com.chronie.homemoneylite.data.remote.api

import com.chronie.homemoneylite.data.remote.dto.HealthDto
import retrofit2.http.GET
import com.google.gson.annotations.SerializedName

interface MemberApi {
    @GET("api/health/lite")
    suspend fun checkHealth(): HealthDto

}

data class AvatarUpdateRequest(
    @SerializedName("avatar")
    val avatar: String
)
