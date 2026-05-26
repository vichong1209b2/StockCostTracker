// StockQuoteEntity
package com.example.stockcosttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_quotes")
data class StockQuoteEntity(
    @PrimaryKey
    val stockCode: String,
    val currentPrice: Double,
    val updatedAt: String
)
