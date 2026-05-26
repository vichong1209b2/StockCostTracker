// StockQuote.kt
package com.example.stockcosttracker.domain.model

data class StockQuote(
    val stockCode: String,
    val currentPrice: Double,
    val updatedAt: String
)
