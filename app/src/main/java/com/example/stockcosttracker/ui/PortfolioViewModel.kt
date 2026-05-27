// PortfolioViewModel.kt
// V1.2
package com.example.stockcosttracker.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stockcosttracker.data.local.StockDatabase
import com.example.stockcosttracker.data.local.entity.DailyPortfolioSnapshotEntity
import com.example.stockcosttracker.data.repository.StockRepository
import com.example.stockcosttracker.domain.model.FeeConfig
import com.example.stockcosttracker.domain.model.PortfolioSummary
import com.example.stockcosttracker.domain.model.StockInfo
import com.example.stockcosttracker.domain.model.StockQuote
import com.example.stockcosttracker.domain.model.StockTransaction
import com.example.stockcosttracker.domain.model.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

data class TransactionRecordUi(
    val transaction: StockTransaction,
    val brokerageFee: Double,
    val tax: Double,
    val netAmount: Double,
    val costBasis: Double? = null,
    val realizedProfit: Double? = null,
    val realizedProfitRate: Double? = null
)

data class TransactionGroupUi(
    val stockCode: String,
    val stockName: String,
    val transactions: List<StockTransaction>,
    val transactionRecords: List<TransactionRecordUi>,
    val summary: PortfolioSummary?,
    val isExpanded: Boolean = false
)

data class PortfolioUiState(
    val isRefreshingQuotes: Boolean = false,
    val feeConfig: FeeConfig = FeeConfig(),
    val stockInfos: List<StockInfo> = emptyList(),
    val summaries: List<PortfolioSummary> = emptyList(),
    val transactionGroups: List<TransactionGroupUi> = emptyList(),
    val dailySnapshots: List<DailyPortfolioSnapshotEntity> = emptyList()
)

class PortfolioViewModel(
    application: Application,
    private val repository: StockRepository
) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "stockcosttracker_settings"
        private const val KEY_AUTO_AFTER_HOURS_SYNC_ENABLED = "auto_after_hours_sync_enabled"
        private const val KEY_LAST_AUTO_SYNC_DATE = "last_auto_sync_date"

        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    val dao = StockDatabase.getInstance(application).stockDao()
                    val repository = StockRepository.create(dao)
                    return PortfolioViewModel(application, repository) as T
                }
            }
        }
    }

    private enum class SnackbarSource {
        MANUAL_REFRESH,
        AUTO_SYNC,
        GENERAL
    }

    private val prefs: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val tradingCalendarHelper = TradingCalendarHelper(prefs)
    private val portfolioAutoSyncHelper = PortfolioAutoSyncHelper(tradingCalendarHelper)

    private val expandedTransactionGroups = MutableStateFlow<Set<String>>(emptySet())
    private val isRefreshingQuotes = MutableStateFlow(false)

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    private val transactionsFlow = repository.observeTransactions()
    private val quotesFlow = repository.observeQuotes()
    private val stockInfosFlow = repository.observeStockInfos()
    private val feeConfigFlow = repository.observeFeeConfig()
    private val dailySnapshotsFlow = repository.observeDailySnapshots()

    private data class PortfolioBaseData(
        val transactions: List<StockTransaction>,
        val quotes: List<StockQuote>,
        val stockInfos: List<StockInfo>,
        val feeConfig: FeeConfig,
        val dailySnapshots: List<DailyPortfolioSnapshotEntity>
    )

    val uiState: StateFlow<PortfolioUiState> =
        combine(
            combine(
                transactionsFlow,
                quotesFlow,
                stockInfosFlow,
                feeConfigFlow,
                dailySnapshotsFlow
            ) { transactions, quotes, stockInfos, feeConfig, dailySnapshots ->
                PortfolioBaseData(
                    transactions = transactions,
                    quotes = quotes,
                    stockInfos = stockInfos,
                    feeConfig = feeConfig,
                    dailySnapshots = dailySnapshots
                )
            },
            expandedTransactionGroups,
            isRefreshingQuotes
        ) { baseData, expandedCodes, refreshing ->

            val summaries = buildPortfolioSummaries(
                transactions = baseData.transactions,
                quotes = baseData.quotes,
                stockInfos = baseData.stockInfos,
                feeConfig = baseData.feeConfig
            )

            PortfolioUiState(
                isRefreshingQuotes = refreshing,
                feeConfig = baseData.feeConfig,
                stockInfos = baseData.stockInfos,
                summaries = summaries,
                transactionGroups = buildTransactionGroups(
                    transactions = baseData.transactions,
                    stockInfos = baseData.stockInfos,
                    summaries = summaries,
                    feeConfig = baseData.feeConfig,
                    expandedCodes = expandedCodes
                ),
                dailySnapshots = baseData.dailySnapshots
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PortfolioUiState()
        )

    fun onAppLaunch() {
        viewModelScope.launch {
            tradingCalendarHelper.ensureTradingCalendarLoaded()
            runAfterHoursAutoSyncOnLaunch()
            ensureTodayDailySnapshot()
        }
    }

    fun isAutoAfterHoursSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_AFTER_HOURS_SYNC_ENABLED, false)
    }

    fun setAutoAfterHoursSyncEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_AUTO_AFTER_HOURS_SYNC_ENABLED, enabled)
            .apply()
    }

    private suspend fun runAfterHoursAutoSyncOnLaunch() {
        if (!isAutoAfterHoursSyncEnabled()) {
            return
        }

        if (isRefreshingQuotes.value) return

        val decision = portfolioAutoSyncHelper.evaluateLaunchAutoSync(
            now = LocalDateTime.now(),
            summaries = uiState.value.summaries,
            lastAutoSyncDate = getLastAutoSyncDate()
        )

        // 改成靜默：不需要同步時不顯示 Snackbar
        if (!decision.shouldSync) {
            return
        }

        // 改成靜默：沒有持股可同步時不顯示 Snackbar
        if (decision.stockCodes.isEmpty()) {
            return
        }

        isRefreshingQuotes.value = true
        try {
            val result = performQuoteRefresh(decision.stockCodes)
            saveLastAutoSyncDate(LocalDate.now())
            captureTodayDailySnapshot()

            val rawMessage = result.message.trim()
            val detail = if (rawMessage.isNotBlank()) {
                rawMessage
            } else {
                "啟動時已補齊盤後資料並更新今日快照"
            }

            emitSnackbar(
                source = SnackbarSource.AUTO_SYNC,
                message = detail
            )
        } catch (e: Exception) {
            emitSnackbar(
                source = SnackbarSource.AUTO_SYNC,
                message = "盤後同步失敗：${e.message ?: "未知錯誤"}"
            )
        } finally {
            isRefreshingQuotes.value = false
        }
    }

    private fun getLastAutoSyncDate(): LocalDate? {
        val raw = prefs.getString(KEY_LAST_AUTO_SYNC_DATE, null) ?: return null
        return try {
            LocalDate.parse(raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun saveLastAutoSyncDate(date: LocalDate) {
        prefs.edit()
            .putString(KEY_LAST_AUTO_SYNC_DATE, date.toString())
            .apply()
    }

    fun refreshQuotes() {
        viewModelScope.launch {
            if (isRefreshingQuotes.value) return@launch

            val stockCodes = uiState.value.summaries
                .map { it.stockCode }
                .distinct()

            if (stockCodes.isEmpty()) {
                emitSnackbar(
                    source = SnackbarSource.MANUAL_REFRESH,
                    message = "目前沒有需要同步的持股"
                )
                return@launch
            }

            isRefreshingQuotes.value = true
            try {
                val result = performQuoteRefresh(stockCodes)
                captureTodayDailySnapshot()

                val rawMessage = result.message.trim()
                val detail = if (rawMessage.isNotBlank()) rawMessage else "現價更新完成"

                emitSnackbar(
                    source = SnackbarSource.MANUAL_REFRESH,
                    message = detail
                )
            } catch (e: Exception) {
                emitSnackbar(
                    source = SnackbarSource.MANUAL_REFRESH,
                    message = "現價更新失敗：${e.message ?: "未知錯誤"}"
                )
            } finally {
                isRefreshingQuotes.value = false
            }
        }
    }

    private suspend fun performQuoteRefresh(stockCodes: List<String>) =
        repository.refreshQuotes(stockCodes)

    private suspend fun emitSnackbar(
        source: SnackbarSource,
        message: String
    ) {
        val prefix = when (source) {
            SnackbarSource.MANUAL_REFRESH -> "[手動更新] "
            SnackbarSource.AUTO_SYNC -> "[自動同步] "
            SnackbarSource.GENERAL -> ""
        }
        _messages.emit(prefix + message)
    }

    fun refreshStockInfos() {
        viewModelScope.launch {
            repository.refreshStockInfos()
        }
    }

    fun saveFeeConfig(config: FeeConfig) {
        viewModelScope.launch {
            repository.saveFeeConfig(config)
        }
    }

    fun addTransaction(
        stockCode: String,
        tradeDate: String,
        transactionType: TransactionType,
        priceInput: String,
        lotInput: String,
        note: String
    ) {
        viewModelScope.launch {
            runCatching {
                repository.addTransaction(
                    stockCode = stockCode,
                    tradeDate = tradeDate,
                    transactionType = transactionType,
                    priceInput = priceInput,
                    lotInput = lotInput,
                    note = note
                )
                ensureTodayDailySnapshot()
            }.onSuccess {
                emitSnackbar(
                    source = SnackbarSource.GENERAL,
                    message = "已新增交易紀錄"
                )
            }.onFailure { error ->
                emitSnackbar(
                    source = SnackbarSource.GENERAL,
                    message = error.message ?: "新增交易失敗"
                )
            }
        }
    }

    fun deleteTransaction(transaction: StockTransaction) {
        viewModelScope.launch {
            runCatching {
                repository.deleteTransaction(transaction)
                ensureTodayDailySnapshot()
            }.onSuccess {
                emitSnackbar(
                    source = SnackbarSource.GENERAL,
                    message = "已刪除交易紀錄"
                )
            }.onFailure { error ->
                emitSnackbar(
                    source = SnackbarSource.GENERAL,
                    message = error.message ?: "刪除交易失敗"
                )
            }
        }
    }

    fun toggleTransactionGroup(stockCode: String) {
        val current = expandedTransactionGroups.value
        expandedTransactionGroups.value = if (current.contains(stockCode)) {
            current - stockCode
        } else {
            current + stockCode
        }
    }

    private suspend fun ensureTodayDailySnapshot() {
        val today = LocalDate.now().toString()
        val latest = repository.getLatestDailySnapshotDate()
        if (latest == today) return
        captureDailySnapshot(snapshotDate = today)
    }

    private suspend fun captureTodayDailySnapshot() {
        captureDailySnapshot(snapshotDate = LocalDate.now().toString())
    }

    private suspend fun captureDailySnapshot(snapshotDate: String) {
        val data = repository.getAllDataForSnapshot()

        val summaries = buildPortfolioSummaries(
            transactions = data.transactions,
            quotes = data.quotes,
            stockInfos = data.stockInfos,
            feeConfig = data.feeConfig
        )

        val aggregate = buildAggregateSummary(summaries)

        repository.upsertDailyPortfolioSnapshot(
            snapshotDate = snapshotDate,
            totalCost = aggregate.totalCost,
            totalValueNet = aggregate.totalValueNet,
            totalUnrealizedNet = aggregate.totalUnrealized,
            totalRealized = aggregate.totalRealized,
            totalProfit = aggregate.totalProfit,
            profitRate = aggregate.profitRate
        )
    }

    private data class AggregatePortfolioSummary(
        val totalCost: Double,
        val totalValueNet: Double,
        val totalUnrealized: Double,
        val totalRealized: Double,
        val totalProfit: Double,
        val profitRate: Double?
    )

    private fun buildAggregateSummary(
        summaries: List<PortfolioSummary>
    ): AggregatePortfolioSummary {
        val totalCost = summaries.sumOf { it.remainingCost }
        val totalValueNet = summaries.sumOf { summary ->
            val price = summary.currentPrice ?: 0.0
            val grossValue = price * summary.holdingShares
            val feeTax = summary.estimatedSellFeeAndTax ?: 0.0
            grossValue - feeTax
        }
        val totalUnrealized = summaries.sumOf { it.unrealizedProfitNet ?: 0.0 }
        val totalRealized = summaries.sumOf { it.realizedProfit }
        val totalProfit = totalUnrealized + totalRealized
        val profitRate = if (totalCost > 0.0) {
            totalProfit / totalCost * 100.0
        } else {
            null
        }

        return AggregatePortfolioSummary(
            totalCost = totalCost,
            totalValueNet = totalValueNet,
            totalUnrealized = totalUnrealized,
            totalRealized = totalRealized,
            totalProfit = totalProfit,
            profitRate = profitRate
        )
    }

    private fun buildTransactionGroups(
        transactions: List<StockTransaction>,
        stockInfos: List<StockInfo>,
        summaries: List<PortfolioSummary>,
        feeConfig: FeeConfig,
        expandedCodes: Set<String>
    ): List<TransactionGroupUi> {
        val stockNameMap = stockInfos.associateBy { it.stockCode }
        val summaryMap = summaries.associateBy { it.stockCode }

        return transactions
            .groupBy { it.stockCode }
            .map { (stockCode, txs) ->
                val records = buildTransactionRecords(
                    transactions = txs,
                    feeConfig = feeConfig
                )
                TransactionGroupUi(
                    stockCode = stockCode,
                    stockName = stockNameMap[stockCode]?.stockName.orEmpty(),
                    transactions = records.map { it.transaction },
                    transactionRecords = records,
                    summary = summaryMap[stockCode],
                    isExpanded = expandedCodes.contains(stockCode)
                )
            }
            .sortedBy { it.stockCode }
    }

    private fun buildTransactionRecords(
        transactions: List<StockTransaction>,
        feeConfig: FeeConfig
    ): List<TransactionRecordUi> {
        var holdingShares = 0.0
        var remainingCost = 0.0

        val chronologicalRecords = transactions
            .sortedWith(compareBy<StockTransaction> { it.tradeDate }.thenBy { it.id })
            .map { tx ->
                val gross = tx.grossAmount

                when (tx.transactionType) {
                    TransactionType.BUY -> {
                        val buyFee = calculateBuyFee(gross, feeConfig)
                        val netAmount = gross + buyFee

                        holdingShares += tx.shareCount
                        remainingCost += netAmount

                        TransactionRecordUi(
                            transaction = tx,
                            brokerageFee = buyFee,
                            tax = 0.0,
                            netAmount = netAmount
                        )
                    }

                    TransactionType.SELL -> {
                        val sellFee = calculateSellFee(gross, feeConfig)
                        val sellTax = calculateSellTax(gross, feeConfig)
                        val netAmount = gross - sellFee - sellTax

                        val sellShares = min(tx.shareCount, holdingShares)
                        val averageCostPerShare = if (holdingShares > 0.0) {
                            remainingCost / holdingShares
                        } else {
                            0.0
                        }
                        val costBasis = averageCostPerShare * sellShares
                        val realizedProfit = if (sellShares > 0.0) {
                            netAmount - costBasis
                        } else {
                            null
                        }
                        val realizedProfitRate = if (realizedProfit != null && costBasis > 0.0) {
                            realizedProfit / costBasis * 100.0
                        } else {
                            null
                        }

                        holdingShares -= sellShares
                        remainingCost -= costBasis

                        if (holdingShares < 0.0) holdingShares = 0.0
                        if (remainingCost < 0.0) remainingCost = 0.0

                        TransactionRecordUi(
                            transaction = tx,
                            brokerageFee = sellFee,
                            tax = sellTax,
                            netAmount = netAmount,
                            costBasis = costBasis,
                            realizedProfit = realizedProfit,
                            realizedProfitRate = realizedProfitRate
                        )
                    }
                }
            }

        return chronologicalRecords.sortedWith(
            compareByDescending<TransactionRecordUi> { it.transaction.tradeDate }
                .thenByDescending { it.transaction.id }
        )
    }

    private fun buildPortfolioSummaries(
        transactions: List<StockTransaction>,
        quotes: List<StockQuote>,
        stockInfos: List<StockInfo>,
        feeConfig: FeeConfig
    ): List<PortfolioSummary> {
        val quoteMap = quotes.associateBy { it.stockCode }
        val stockInfoMap = stockInfos.associateBy { it.stockCode }

        return transactions
            .groupBy { it.stockCode }
            .map { (stockCode, txs) ->
                buildSinglePortfolioSummary(
                    stockCode = stockCode,
                    stockName = stockInfoMap[stockCode]?.stockName,
                    transactions = txs.sortedWith(
                        compareBy<StockTransaction> { it.tradeDate }
                            .thenBy { it.id }
                    ),
                    quote = quoteMap[stockCode],
                    feeConfig = feeConfig
                )
            }
            .sortedBy { it.stockCode }
    }

    private fun buildSinglePortfolioSummary(
        stockCode: String,
        stockName: String?,
        transactions: List<StockTransaction>,
        quote: StockQuote?,
        feeConfig: FeeConfig
    ): PortfolioSummary {
        var holdingShares = 0.0
        var remainingCost = 0.0
        var realizedProfit = 0.0
        var realizedCostBasis = 0.0

        transactions.forEach { tx ->
            val shares = tx.shareCount
            val gross = tx.grossAmount

            when (tx.transactionType) {
                TransactionType.BUY -> {
                    val buyFee = calculateBuyFee(gross, feeConfig)
                    holdingShares += shares
                    remainingCost += gross + buyFee
                }

                TransactionType.SELL -> {
                    if (holdingShares <= 0.0) return@forEach

                    val sellShares = min(shares, holdingShares)
                    val avgCostPerShare =
                        if (holdingShares == 0.0) 0.0 else remainingCost / holdingShares

                    val removedCost = avgCostPerShare * sellShares
                    val sellGross = tx.pricePerShare * sellShares
                    val sellFee = calculateSellFee(sellGross, feeConfig)
                    val sellTax = calculateSellTax(sellGross, feeConfig)
                    val proceedsNet = sellGross - sellFee - sellTax
                    val profit = proceedsNet - removedCost

                    holdingShares -= sellShares
                    remainingCost -= removedCost
                    realizedProfit += profit
                    realizedCostBasis += removedCost

                    if (holdingShares < 0.0) holdingShares = 0.0
                    if (remainingCost < 0.0) remainingCost = 0.0
                }
            }
        }

        val safeHoldingShares = max(holdingShares, 0.0)
        val safeRemainingCost = max(remainingCost, 0.0)

        val averageCostPerShare =
            if (safeHoldingShares <= 0.0) 0.0 else safeRemainingCost / safeHoldingShares

        val currentPrice = quote?.currentPrice
        val estimatedSellFeeAndTax = if (currentPrice != null && safeHoldingShares > 0.0) {
            val grossValue = currentPrice * safeHoldingShares
            calculateSellFee(grossValue, feeConfig) + calculateSellTax(grossValue, feeConfig)
        } else {
            null
        }

        val unrealizedProfitNet = if (currentPrice != null && safeHoldingShares > 0.0) {
            val grossValue = currentPrice * safeHoldingShares
            val netValue = grossValue - (estimatedSellFeeAndTax ?: 0.0)
            netValue - safeRemainingCost
        } else {
            null
        }

        val unrealizedProfitRateNet = if (unrealizedProfitNet != null && safeRemainingCost > 0.0) {
            unrealizedProfitNet / safeRemainingCost * 100.0
        } else {
            null
        }

        val realizedProfitRate = if (realizedCostBasis > 0.0) {
            realizedProfit / realizedCostBasis * 100.0
        } else {
            null
        }

        return PortfolioSummary(
            stockCode = stockCode,
            stockName = stockName,
            holdingLots = safeHoldingShares / 1000.0,
            holdingShares = safeHoldingShares,
            remainingCost = safeRemainingCost,
            averageCostPerShare = averageCostPerShare,
            currentPrice = currentPrice,
            unrealizedProfitNet = unrealizedProfitNet,
            unrealizedProfitRateNet = unrealizedProfitRateNet,
            realizedProfit = realizedProfit,
            realizedProfitRate = realizedProfitRate,
            lastQuoteTime = quote?.updatedAt,
            estimatedSellFeeAndTax = estimatedSellFeeAndTax
        )
    }

    private fun calculateBuyFee(amount: Double, feeConfig: FeeConfig): Double {
        val fee = amount * feeConfig.brokerageFeeRate * feeConfig.effectiveDiscountRate
        return max(feeConfig.brokerageMinimumFee, fee)
    }

    private fun calculateSellFee(amount: Double, feeConfig: FeeConfig): Double {
        val fee = amount * feeConfig.brokerageFeeRate * feeConfig.effectiveDiscountRate
        return max(feeConfig.brokerageMinimumFee, fee)
    }

    private fun calculateSellTax(amount: Double, feeConfig: FeeConfig): Double {
        return amount * feeConfig.sellTaxRate
    }
}
