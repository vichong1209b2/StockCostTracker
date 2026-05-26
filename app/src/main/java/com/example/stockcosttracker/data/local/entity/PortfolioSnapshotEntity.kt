// PortfolioSnapshotEntity.kt
package com.example.stockcosttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_snapshots")
data class PortfolioSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val capturedAt: String,
    val totalCost: Double,
    val totalValueNet: Double,
    val totalUnrealizedNet: Double,
    val totalRealized: Double,
    val totalProfit: Double,
    val profitRate: Double?
)

