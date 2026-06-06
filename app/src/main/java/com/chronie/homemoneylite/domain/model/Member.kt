package com.chronie.homemoneylite.domain.model

data class Member(
    val id: String,
    val username: String,
    val createdAt: Long,
    val updatedAt: Long,
    val avatar: String? = null
)
