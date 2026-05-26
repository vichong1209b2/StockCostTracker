// StockTransactionEntity.kt
package com.example.stockcosttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_transactions")
data class StockTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val stockCode: String,
    val tradeDate: String,
    val transactionType: String,
    val pricePerShare: Double,
    val lotCount: Double,
    val note: String
)
