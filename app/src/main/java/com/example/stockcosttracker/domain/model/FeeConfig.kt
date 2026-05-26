// FeeConfig.kt
package com.example.stockcosttracker.domain.model

/**
 * 台股常見費用預設值（可自行調整）：
 * - 券商手續費（買/賣）：0.1425%
 * - 證交稅（賣出）：0.3%
 *
 * discountRate 用來表示「折扣倍率」：
 * - 1.0 = 無折扣
 * - 0.6 = 6 折
 */
data class FeeConfig(
    val brokerageFeeRate: Double = 0.001425,
    val brokerageFeeDiscountRate: Double = 0.6,
    val brokerageMinimumFee: Double = 20.0,
    val sellTaxRate: Double = 0.003
)

