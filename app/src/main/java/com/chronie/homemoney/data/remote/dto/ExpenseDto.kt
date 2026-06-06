package com.chronie.homemoney.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ExpenseDto(
    @SerializedName("id")
    val id: String? = null,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("remark")
    val remark: String? = null,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("date")
    val date: String,
    
    @SerializedName("version")
    val version: Int = 1,
    
    @SerializedName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @SerializedName("deletedAt")
    val deletedAt: Long? = null
)

data class ExpenseListResponse(
    @SerializedName("data")
    val data: List<ExpenseDto>,
    
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("limit")
    val limit: Int,
    
    @SerializedName("meta")
    val meta: ExpenseMetaDto? = null
)

data class ExpenseMetaDto(
    @SerializedName("uniqueTypes")
    val uniqueTypes: List<String>,
    
    @SerializedName("availableMonths")
    val availableMonths: List<String>
)

data class ExpenseStatisticsDto(
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("totalAmount")
    val totalAmount: Double,
    
    @SerializedName("averageAmount")
    val averageAmount: Double,
    
    @SerializedName("medianAmount")
    val medianAmount: Double,
    
    @SerializedName("minAmount")
    val minAmount: Double,
    
    @SerializedName("maxAmount")
    val maxAmount: Double,
    
    @SerializedName("typeDistribution")
    val typeDistribution: Map<String, TypeDistributionDto>
)

data class TypeDistributionDto(
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("percentage")
    val percentage: Int
)

data class SyncRequestDto(
    @SerializedName("lastSyncTime")
    val lastSyncTime: Long?,
    
    @SerializedName("changes")
    val changes: List<ExpenseDto>?,
    
    @SerializedName("localIds")
    val localIds: List<String>?
)

data class SyncResponseDto(
    @SerializedName("serverChanges")
    val serverChanges: List<ExpenseDto>,
    
    @SerializedName("conflicts")
    val conflicts: List<ConflictDto>,
    
    @SerializedName("syncTime")
    val syncTime: Long
)

data class ConflictDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("clientVersion")
    val clientVersion: Int,
    
    @SerializedName("serverVersion")
    val serverVersion: Int,
    
    @SerializedName("clientUpdatedAt")
    val clientUpdatedAt: Long,
    
    @SerializedName("serverUpdatedAt")
    val serverUpdatedAt: Long,
    
    @SerializedName("serverData")
    val serverData: ExpenseDto?
)
