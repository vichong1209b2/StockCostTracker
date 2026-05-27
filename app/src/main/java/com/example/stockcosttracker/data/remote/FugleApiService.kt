package com.example.stockcosttracker.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

data class FugleQuoteDto(
    val date: String? = null,
    val type: String? = null,
    val exchange: String? = null,
    val market: String? = null,
    val symbol: String? = null,
    val name: String? = null,
    val referencePrice: Double? = null,
    val previousClose: Double? = null,
    val openPrice: Double? = null,
    val openTime: Long? = null,
    val highPrice: Double? = null,
    val highTime: Long? = null,
    val lowPrice: Double? = null,
    val lowTime: Long? = null,
    val closePrice: Double? = null,
    val closeTime: Long? = null,
    val avgPrice: Double? = null,
    val lastPrice: Double? = null,
    val lastSize: Long? = null,
    val isClose: Boolean? = null,
    val isTrial: Boolean? = null,
    val lastUpdated: Long? = null,
    val lastTrade: FugleTradeDto? = null,
    val total: FugleTotalDto? = null,
    val trial: Boolean? = null
)

data class FugleTotalDto(
    val tradeValue: Double? = null,
    val tradeVolume: Long? = null,
    val tradeVolumeAtBid: Long? = null,
    val tradeVolumeAtAsk: Long? = null,
    val transaction: Long? = null,
    val time: Long? = null
)

data class FugleTradeDto(
    val bid: Double? = null,
    val ask: Double? = null,
    val price: Double? = null,
    val size: Long? = null,
    val time: Long? = null,
    val serial: Long? = null
)

interface FugleApiService {

    @GET("marketdata/v1.0/stock/intraday/quote/{symbol}")
    suspend fun getIntradayQuote(
        @Header("X-API-KEY") apiKey: String,
        @Path("symbol") symbol: String
    ): FugleQuoteDto
}
