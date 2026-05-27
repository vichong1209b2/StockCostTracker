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
enum class Broker(
    val displayName: String,
    val discountRate: Double
) {
    SHINKO("新光證券", 0.28),
    YUANTA("元大證券", 0.6),
    TAISHIN("永豐證券", 0.7),
    FUBON("富邦證券", 0.8),
    CATHAY("國泰證券", 0.9),
    FIRST("第一證券", 1.0)
}

data class FeeConfig(
    val brokerageFeeRate: Double = 0.001425,
    val brokerageFeeDiscountRate: Double = 0.6,
    val brokerageMinimumFee: Double = 20.0,
    val sellTaxRate: Double = 0.003,
    val selectedBroker: Broker = Broker.YUANTA
) {
    val effectiveDiscountRate: Double
        get() = selectedBroker.discountRate.takeIf { it > 0.0 } ?: brokerageFeeDiscountRate
}
