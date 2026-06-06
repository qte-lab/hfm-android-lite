package com.chronie.homemoney.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MemberDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String,
    @SerializedName("avatar")
    val avatar: String? = null
)

data class MemberRequest(
    @SerializedName("username")
    val username: String
)
