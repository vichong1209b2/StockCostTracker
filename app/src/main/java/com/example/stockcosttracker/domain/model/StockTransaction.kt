// StockTransaction.kt
package com.example.stockcosttracker.domain.model

data class StockTransaction(
    val id: Long = 0L,
    val stockCode: String,
    val tradeDate: String,
    val transactionType: TransactionType,
    val pricePerShare: Double,
    val lotCount: Double,
    val note: String = ""
) {
    val shareCount: Double
        get() = lotCount * 1000.0

    val grossAmount: Double
        get() = pricePerShare * shareCount
}
