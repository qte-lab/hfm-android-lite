package com.chronie.homemoney.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["is_synced"]),
        Index(value = ["updated_at"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: String,
    
    @ColumnInfo(name = "type")
    @SerializedName("type")
    val type: String,
    
    @ColumnInfo(name = "remark")
    @SerializedName("remark")
    val remark: String?,
    
    @ColumnInfo(name = "amount")
    @SerializedName("amount")
    val amount: Double,
    
    @ColumnInfo(name = "date")
    @SerializedName("date")
    val date: String,
    
    @ColumnInfo(name = "version")
    @SerializedName("version")
    val version: Int = 1,
    
    @ColumnInfo(name = "updated_at")
    @SerializedName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "deleted_at")
    @SerializedName("deletedAt")
    val deletedAt: Long? = null,
    
    @ColumnInfo(name = "is_synced")
    @SerializedName("is_synced")
    val isSynced: Boolean = false
)
