// DailyPortfolioSnapshotEntity.kt
package com.example.stockcosttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "daily_portfolio_snapshots")
data class DailyPortfolioSnapshotEntity(
    @PrimaryKey
    val snapshotDate: String, // yyyy-MM-dd
    val totalCost: Double,
    val totalValueNet: Double,
    val totalUnrealizedNet: Double,
    val totalRealized: Double,
    val totalProfit: Double,
    val profitRate: Double?,
    val createdAt: String = LocalDateTime.now().toString()
)