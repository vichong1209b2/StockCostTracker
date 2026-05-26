package com.example.stockcosttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_infos")
data class StockInfoEntity(
    @PrimaryKey
    val stockCode: String,
    val stockName: String,
    val industryCategory: String?,
    val type: String?
)

