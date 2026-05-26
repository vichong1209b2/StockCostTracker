// PortfolioSummary
package com.example.stockcosttracker.domain.model

data class PortfolioSummary(
    val stockCode: String,
    val stockName: String?,
    val holdingLots: Double,
    val holdingShares: Double,
    val remainingCost: Double,
    val averageCostPerShare: Double,
    val currentPrice: Double?,
    val unrealizedProfitNet: Double?,
    val unrealizedProfitRateNet: Double?,
    val realizedProfit: Double,
    val realizedProfitRate: Double?,
    val lastQuoteTime: String?,
    val estimatedSellFeeAndTax: Double?
)