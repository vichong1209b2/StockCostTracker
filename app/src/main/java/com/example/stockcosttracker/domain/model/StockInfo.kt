// StockInfo.kt
package com.example.stockcosttracker.domain.model

data class StockInfo(
    val stockCode: String,
    val stockName: String,
    val industryCategory: String? = null,
    val type: String? = null
)

