package com.example.stockcosttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.stockcosttracker.domain.model.Broker

@Entity(tableName = "fee_config")
data class FeeConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val brokerageFeeRate: Double,
    val brokerageFeeDiscountRate: Double,
    val brokerageMinimumFee: Double,
    val sellTaxRate: Double,
    val selectedBroker: String = Broker.YUANTA.name
)

