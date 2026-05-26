package com.example.stockcosttracker.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

data class FugleQuoteDto(
    val symbol: String? = null,
    val priceHigh: Double? = null,
    val priceLow: Double? = null,
    val priceOpen: Double? = null,
    val priceAvg: Double? = null,
    val priceClose: Double? = null,
    val priceReference: Double? = null,
    val priceLimitHigh: Double? = null,
    val priceLimitLow: Double? = null,
    val isClose: Boolean? = null,
    val total: FugleTotalDto? = null,
    val trial: Boolean? = null
)

data class FugleTotalDto(
    val tradeValue: Double? = null,
    val tradeVolume: Long? = null,
    val tradeVolumeAtBid: Long? = null,
    val tradeVolumeAtAsk: Long? = null,
    val transaction: Long? = null,
    val time: String? = null
)

interface FugleApiService {

    @GET("marketdata/v1.0/stock/intraday/quote/{symbol}")
    suspend fun getIntradayQuote(
        @Header("X-API-KEY") apiKey: String,
        @Path("symbol") symbol: String
    ): FugleQuoteDto
}