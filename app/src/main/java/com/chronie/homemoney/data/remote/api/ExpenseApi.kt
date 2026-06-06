package com.chronie.homemoney.data.remote.api

import com.chronie.homemoney.data.remote.dto.ExpenseDto
import com.chronie.homemoney.data.remote.dto.ExpenseListResponse
import com.chronie.homemoney.data.remote.dto.ExpenseStatisticsDto
import com.chronie.homemoney.data.remote.dto.SyncRequestDto
import com.chronie.homemoney.data.remote.dto.SyncResponseDto
import retrofit2.Response
import retrofit2.http.*

interface ExpenseApi {
    
    @GET("api/expenses")
    suspend fun getExpenses(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("keyword") keyword: String? = null,
        @Query("type") type: String? = null,
        @Query("month") month: String? = null,
        @Query("minAmount") minAmount: Double? = null,
        @Query("maxAmount") maxAmount: Double? = null,
        @Query("sort") sort: String = "dateDesc"
    ): Response<ExpenseListResponse>
    
    @POST("api/expenses")
    suspend fun addExpense(
        @Body expense: ExpenseDto
    ): Response<ExpenseDto>
    
    @POST("api/expenses")
    suspend fun createExpense(
        @Body expense: ExpenseDto
    ): Response<com.chronie.homemoney.data.remote.dto.ApiResponse<ExpenseDto>>
    
    @PUT("api/expenses/{id}")
    suspend fun updateExpense(
        @Path("id") id: String,
        @Body expense: ExpenseDto
    ): Response<com.chronie.homemoney.data.remote.dto.ApiResponse<ExpenseDto>>
    
    @POST("api/expenses/batch")
    suspend fun addExpensesBatch(
        @Body expenses: List<ExpenseDto>
    ): Response<List<ExpenseDto>>
    
    @DELETE("api/expenses/{id}")
    suspend fun deleteExpense(
        @Path("id") id: String
    ): Response<Unit>
    
    @DELETE("api/expenses/{id}/hard")
    suspend fun hardDeleteExpense(
        @Path("id") id: String
    ): Response<Unit>
    
    @GET("api/expenses/statistics")
    suspend fun getStatistics(
        @Query("keyword") keyword: String? = null,
        @Query("type") type: String? = null,
        @Query("month") month: String? = null,
        @Query("minAmount") minAmount: Double? = null,
        @Query("maxAmount") maxAmount: Double? = null
    ): Response<ExpenseStatisticsDto>
    
    @POST("api/expenses/sync")
    suspend fun syncExpenses(
        @Body request: SyncRequestDto
    ): Response<SyncResponseDto>
}
