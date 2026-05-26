/*
 * File: FinMindApiService.kt
 * Version: v2.0
 * Updated: 2026-05-09
 * Description:
 * - Use FinMind v4 daily API for TaiwanStockPrice
 * - Use data_id + start_date + end_date
 * - Keep minute API for fallback initialization
 */

package com.example.stockcosttracker.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class FinMindMinutePriceItemDto(
    val date: String? = null,
    val stock_id: String? = null,
    val deal_price: Double? = null
)

data class FinMindMinutePriceResponseDto(
    val msg: String? = null,
    val status: Int? = null,
    val data: List<FinMindMinutePriceItemDto> = emptyList()
)

data class FinMindDailyPriceItemDto(
    val date: String,
    val stock_id: String? = null,
    val close: Double? = null
)

data class FinMindDailyPriceResponseDto(
    val msg: String? = null,
    val status: Int? = null,
    val data: List<FinMindDailyPriceItemDto> = emptyList()
)

data class FinMindStockInfoItemDto(
    val stock_id: String,
    val stock_name: String,
    val industry_category: String?,
    val type: String?
)

data class FinMindStockInfoResponseDto(
    val msg: String? = null,
    val status: Int? = null,
    val data: List<FinMindStockInfoItemDto> = emptyList()
)

interface FinMindApiService {

    @GET("api/v4/data")
    suspend fun getTaiwanStockPriceDaily(
        @Header("Authorization") authorization: String? = null,
        @Query("dataset") dataset: String = "TaiwanStockPrice",
        @Query("data_id") dataId: String,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): FinMindDailyPriceResponseDto

    @GET("api/v3/data")
    suspend fun getTaiwanStockPriceMinute(
        @Query("dataset") dataset: String = "TaiwanStockPriceMinute",
        @Query("stock_id") stockId: String
    ): FinMindMinutePriceResponseDto

    @GET("api/v4/data")
    suspend fun getTaiwanStockInfo(
        @Header("Authorization") authorization: String? = null,
        @Query("dataset") dataset: String = "TaiwanStockInfo"
    ): FinMindStockInfoResponseDto
}