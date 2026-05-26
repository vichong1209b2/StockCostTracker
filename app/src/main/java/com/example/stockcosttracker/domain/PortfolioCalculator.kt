package com.example.stockcosttracker.domain

import com.example.stockcosttracker.domain.model.FeeConfig
import com.example.stockcosttracker.domain.model.PortfolioSummary
import com.example.stockcosttracker.domain.model.StockQuote
import com.example.stockcosttracker.domain.model.StockTransaction
import com.example.stockcosttracker.domain.model.TransactionType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object PortfolioCalculator {

    fun buildSummaries(
        transactions: List<StockTransaction>,
        quotes: List<StockQuote>,
        feeConfig: FeeConfig,
        stockNameResolver: (String) -> String?
    ): List<PortfolioSummary> {
        val quoteMap = quotes.associateBy { it.stockCode }

        return transactions
            .groupBy { it.stockCode }
            .map { (stockCode, items) ->
                buildSummary(
                    stockCode = stockCode,
                    transactions = items.sortedWith(
                        compareBy<StockTransaction> { it.tradeDate }.thenBy { it.id }
                    ),
                    quote = quoteMap[stockCode],
                    feeConfig = feeConfig,
                    stockName = stockNameResolver(stockCode)
                )
            }
            .sortedBy { it.stockCode }
    }

    fun buildSummary(
        stockCode: String,
        transactions: List<StockTransaction>,
        quote: StockQuote?,
        feeConfig: FeeConfig,
        stockName: String?
    ): PortfolioSummary {
        var holdingShares = 0.0
        var remainingCost = 0.0
        var realizedProfit = 0.0
        var realizedCostBasis = 0.0

        transactions.forEach { transaction ->
            when (transaction.transactionType) {
                TransactionType.BUY -> {
                    val buyFee = calculateBrokerageFee(
                        amount = transaction.grossAmount,
                        feeConfig = feeConfig
                    )

                    holdingShares += transaction.shareCount
                    remainingCost += transaction.grossAmount + buyFee
                }

                TransactionType.SELL -> {
                    if (holdingShares <= 0.0) return@forEach

                    val sellShares = min(transaction.shareCount, holdingShares)
                    if (sellShares <= 0.0) return@forEach

                    val averageCostPerShare = if (holdingShares == 0.0) {
                        0.0
                    } else {
                        remainingCost / holdingShares
                    }

                    val costOfSold = averageCostPerShare * sellShares

                    val grossSellAmount = transaction.pricePerShare * sellShares
                    val sellFee = calculateBrokerageFee(
                        amount = grossSellAmount,
                        feeConfig = feeConfig
                    )
                    val sellTax = grossSellAmount * feeConfig.sellTaxRate
                    val netProceeds = grossSellAmount - sellFee - sellTax

                    realizedCostBasis += costOfSold
                    realizedProfit += netProceeds - costOfSold

                    holdingShares -= sellShares
                    remainingCost -= costOfSold

                    if (abs(holdingShares) < 0.0001) {
                        holdingShares = 0.0
                    }
                    if (abs(remainingCost) < 0.0001) {
                        remainingCost = 0.0
                    }

                    if (holdingShares == 0.0) {
                        remainingCost = 0.0
                    }
                }
            }
        }

        val averageCost = if (holdingShares == 0.0) {
            0.0
        } else {
            remainingCost / holdingShares
        }

        val currentPrice = quote?.currentPrice
        val currentValue = if (holdingShares == 0.0 || currentPrice == null) {
            null
        } else {
            currentPrice * holdingShares
        }

        val estimatedSellFee = currentValue?.let {
            calculateBrokerageFee(
                amount = it,
                feeConfig = feeConfig
            )
        }

        val estimatedSellTax = currentValue?.let { it * feeConfig.sellTaxRate }

        val estimatedSellFeeAndTax = if (holdingShares == 0.0) {
            0.0
        } else if (estimatedSellFee != null && estimatedSellTax != null) {
            estimatedSellFee + estimatedSellTax
        } else {
            null
        }

        val unrealizedProfitNet = if (holdingShares == 0.0) {
            0.0
        } else if (currentValue == null || estimatedSellFeeAndTax == null) {
            null
        } else {
            (currentValue - estimatedSellFeeAndTax) - remainingCost
        }

        val unrealizedProfitRateNet = if (holdingShares == 0.0) {
            0.0
        } else if (remainingCost == 0.0 || unrealizedProfitNet == null) {
            null
        } else {
            unrealizedProfitNet / remainingCost * 100.0
        }

        val realizedProfitRate = if (realizedCostBasis <= 0.0) {
            null
        } else {
            realizedProfit / realizedCostBasis * 100.0
        }

        return PortfolioSummary(
            stockCode = stockCode,
            stockName = stockName,
            holdingLots = holdingShares / 1000.0,
            holdingShares = holdingShares,
            remainingCost = max(0.0, remainingCost),
            averageCostPerShare = averageCost,
            currentPrice = currentPrice,
            unrealizedProfitNet = unrealizedProfitNet,
            unrealizedProfitRateNet = unrealizedProfitRateNet,
            realizedProfit = realizedProfit,
            realizedProfitRate = realizedProfitRate,
            lastQuoteTime = quote?.updatedAt,
            estimatedSellFeeAndTax = estimatedSellFeeAndTax
        )
    }

    fun calculateHoldingShares(
        stockCode: String,
        transactions: List<StockTransaction>
    ): Double {
        return transactions
            .filter { it.stockCode == stockCode }
            .sumOf {
                when (it.transactionType) {
                    TransactionType.BUY -> it.shareCount
                    TransactionType.SELL -> -it.shareCount
                }
            }
    }

    private fun calculateBrokerageFee(amount: Double, feeConfig: FeeConfig): Double {
        val rawFee = amount * feeConfig.brokerageFeeRate * feeConfig.brokerageFeeDiscountRate
        return if (rawFee < feeConfig.brokerageMinimumFee) {
            feeConfig.brokerageMinimumFee
        } else {
            rawFee
        }
    }
}