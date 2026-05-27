/*
 * File: StockRepository.kt
 * Version: v1.7
 * Updated: 2026-05-22
 * Package: com.example.stockcosttracker.data.repository
 *
 * Change Summary:
 * - 保留 dailySnapshots 資料流
 * - 新增 observeDailySnapshots()
 * - 新增 upsertDailyPortfolioSnapshot()
 * - 修正 refreshQuotes()：
 *   1. 盤中先 Fugle，失敗 fallback FinMind
 *   2. 非盤中先 FinMind，失敗再補 Fugle
 *   3. 完整 log
 *   4. 依來源寫不同格式 updatedAt，讓 UI 的 PriceSourceLabel 自然工作
 */

package com.example.stockcosttracker.data.repository

import android.util.Log
import com.example.stockcosttracker.BuildConfig
import com.example.stockcosttracker.data.local.StockDao
import com.example.stockcosttracker.data.local.entity.DailyPortfolioSnapshotEntity
import com.example.stockcosttracker.data.local.entity.FeeConfigEntity
import com.example.stockcosttracker.data.local.entity.PortfolioSnapshotEntity
import com.example.stockcosttracker.data.local.entity.StockInfoEntity
import com.example.stockcosttracker.data.local.entity.StockQuoteEntity
import com.example.stockcosttracker.data.local.entity.StockTransactionEntity
import com.example.stockcosttracker.data.remote.FinMindApiService
import com.example.stockcosttracker.data.remote.FugleApiService
import com.example.stockcosttracker.domain.model.Broker
import com.example.stockcosttracker.domain.model.FeeConfig
import com.example.stockcosttracker.domain.model.StockInfo
import com.example.stockcosttracker.domain.model.StockQuote
import com.example.stockcosttracker.domain.model.StockTransaction
import com.example.stockcosttracker.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class StockRepository(
    private val dao: StockDao,
    private val finMindApiService: FinMindApiService,
    private val fugleApiService: FugleApiService
) {

    private val tag = "StockRepository"

    fun observeTransactions(): Flow<List<StockTransaction>> {
        return dao.observeTransactions().map { items -> items.map { it.toDomain() } }
    }

    fun observeQuotes(): Flow<List<StockQuote>> {
        return dao.observeQuotes().map { items -> items.map { it.toDomain() } }
    }

    fun observeStockInfos(): Flow<List<StockInfo>> {
        return dao.observeStockInfos().map { items -> items.map { it.toDomain() } }
    }

    fun observeFeeConfig(): Flow<FeeConfig> {
        return dao.observeFeeConfig().map { it?.toDomain() ?: FeeConfig() }
    }

    fun observePortfolioSnapshots(): Flow<List<PortfolioSnapshotEntity>> {
        return dao.observePortfolioSnapshots()
    }

    fun observeDailySnapshots(): Flow<List<DailyPortfolioSnapshotEntity>> {
        return dao.observeDailyPortfolioSnapshots()
    }

    suspend fun saveFeeConfig(config: FeeConfig) {
        dao.upsertFeeConfig(config.toEntity())
    }

    suspend fun addTransaction(
        stockCode: String,
        tradeDate: String,
        transactionType: TransactionType,
        priceInput: String,
        lotInput: String,
        note: String
    ) {
        val normalizedCode = stockCode.trim().uppercase()
        require(normalizedCode.isNotBlank()) { "請輸入股票代號" }

        val price = priceInput.trim().toDoubleOrNull()
            ?: throw IllegalArgumentException("成交價格格式錯誤")

        val lotCount = lotInput.trim().toDoubleOrNull()
            ?: throw IllegalArgumentException("交易數量格式錯誤")

        require(price > 0) { "成交價格需大於 0" }
        require(lotCount > 0) { "交易數量需大於 0" }
        require(tradeDate.isNotBlank()) { "請輸入交易日期" }

        dao.insertTransaction(
            StockTransactionEntity(
                stockCode = normalizedCode,
                tradeDate = tradeDate,
                transactionType = transactionType.name,
                pricePerShare = price,
                lotCount = lotCount,
                note = note.trim()
            )
        )
    }

    suspend fun addTransaction(transaction: StockTransaction) {
        dao.insertTransaction(transaction.toEntity())
    }

    suspend fun deleteTransaction(transaction: StockTransaction) {
        dao.deleteTransaction(transaction.toEntity())
    }

    suspend fun refreshQuotes(stockCodes: List<String>): QuoteRefreshResult = withContext(Dispatchers.IO) {
        val normalizedCodes = stockCodes
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }
            .distinct()

        if (normalizedCodes.isEmpty()) {
            dao.clearQuotes()
            Log.d(tag, "refreshQuotes: no stock codes, cleared quotes")
            return@withContext QuoteRefreshResult(
                mode = QuoteRefreshMode.NONE,
                successCount = 0,
                failedCount = 0,
                initializedCount = 0,
                fallbackUsedCount = 0,
                message = "目前沒有可更新的持股"
            )
        }

        val existingQuoteMap = dao.getQuotes().associateBy { it.stockCode }
        val marketState = determineMarketState()

        Log.d(tag, "refreshQuotes: start, marketState=$marketState, stockCodes=$normalizedCodes")

        val quotesToUpsert = mutableListOf<StockQuoteEntity>()
        var successCount = 0
        var failedCount = 0
        var initializedCount = 0
        var fallbackUsedCount = 0

        normalizedCodes.forEach { stockCode ->
            val existingQuote = existingQuoteMap[stockCode]
            val hasLocalQuote = existingQuote != null

            Log.d(
                tag,
                "refreshQuotes: processing stockCode=$stockCode, hasLocalQuote=$hasLocalQuote, localUpdatedAt=${existingQuote?.updatedAt}"
            )

            val fetchedQuote = when (marketState) {
                MarketState.TRADING -> {
                    fetchIntradayQuoteFromFugle(stockCode)
                        ?: fetchMinuteQuoteFromFinMind(stockCode)
                }
                MarketState.AFTER_HOURS -> {
                    fetchClosingQuoteFromFinMind(stockCode)
                        ?: fetchMinuteQuoteFromFinMind(stockCode)
                        ?: fetchIntradayQuoteFromFugle(stockCode)
                }
            }

            when {
                fetchedQuote != null -> {
                    quotesToUpsert += fetchedQuote
                    successCount++
                    if (!hasLocalQuote) initializedCount++

                    Log.d(
                        tag,
                        "refreshQuotes: success stockCode=$stockCode, price=${fetchedQuote.currentPrice}, updatedAt=${fetchedQuote.updatedAt}"
                    )
                }

                hasLocalQuote -> {
                    fallbackUsedCount++
                    Log.w(
                        tag,
                        "refreshQuotes: all remote sources failed, keep local quote stockCode=$stockCode, localPrice=${existingQuote?.currentPrice}, localUpdatedAt=${existingQuote?.updatedAt}"
                    )
                }

                else -> {
                    failedCount++
                    Log.e(tag, "refreshQuotes: failed stockCode=$stockCode, no remote quote and no local cache")
                }
            }
        }

        if (quotesToUpsert.isNotEmpty()) {
            dao.upsertQuotes(quotesToUpsert)
            Log.d(tag, "refreshQuotes: upserted ${quotesToUpsert.size} quotes")
        } else {
            Log.w(tag, "refreshQuotes: no quotes to upsert")
        }

        dao.deleteQuotesNotIn(normalizedCodes)
        Log.d(tag, "refreshQuotes: deleteQuotesNotIn done, kept=$normalizedCodes")

        QuoteRefreshResult(
            mode = when (marketState) {
                MarketState.TRADING -> QuoteRefreshMode.INTRADAY
                MarketState.AFTER_HOURS -> QuoteRefreshMode.CLOSING
            },
            successCount = successCount,
            failedCount = failedCount,
            initializedCount = initializedCount,
            fallbackUsedCount = fallbackUsedCount,
            message = buildRefreshMessage(
                marketState = marketState,
                successCount = successCount,
                failedCount = failedCount,
                initializedCount = initializedCount,
                fallbackUsedCount = fallbackUsedCount
            )
        )
    }

    suspend fun refreshStockInfos() = withContext(Dispatchers.IO) {
        val token = BuildConfig.FINMIND_API_TOKEN.trim()
        val authorization = token.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

        val infos = runCatching {
            finMindApiService.getTaiwanStockInfo(authorization = authorization).data.mapNotNull { dto ->
                val code = dto.stock_id.trim()
                val name = dto.stock_name.trim()
                if (code.isBlank() || name.isBlank()) return@mapNotNull null

                StockInfoEntity(
                    stockCode = code,
                    stockName = name,
                    industryCategory = dto.industry_category,
                    type = dto.type
                )
            }
        }.getOrDefault(emptyList())

        if (infos.isNotEmpty()) {
            dao.upsertStockInfos(infos)
        }
    }

    suspend fun capturePortfolioSnapshot(
        capturedAt: String,
        totalCost: Double,
        totalValueNet: Double,
        totalUnrealizedNet: Double,
        totalRealized: Double,
        totalProfit: Double,
        profitRate: Double?
    ) {
        dao.insertPortfolioSnapshot(
            PortfolioSnapshotEntity(
                capturedAt = capturedAt,
                totalCost = totalCost,
                totalValueNet = totalValueNet,
                totalUnrealizedNet = totalUnrealizedNet,
                totalRealized = totalRealized,
                totalProfit = totalProfit,
                profitRate = profitRate
            )
        )
    }

    suspend fun upsertDailyPortfolioSnapshot(
        snapshotDate: String,
        totalCost: Double,
        totalValueNet: Double,
        totalUnrealizedNet: Double,
        totalRealized: Double,
        totalProfit: Double,
        profitRate: Double?
    ) {
        dao.upsertDailyPortfolioSnapshot(
            DailyPortfolioSnapshotEntity(
                snapshotDate = snapshotDate,
                totalCost = totalCost,
                totalValueNet = totalValueNet,
                totalUnrealizedNet = totalUnrealizedNet,
                totalRealized = totalRealized,
                totalProfit = totalProfit,
                profitRate = profitRate,
                createdAt = LocalDateTime.now().toString()
            )
        )
    }

    suspend fun getLatestDailySnapshotDate(): String? = withContext(Dispatchers.IO) {
        dao.getLatestDailySnapshotDate()
    }

    suspend fun getAllDataForSnapshot(): SnapshotData = withContext(Dispatchers.IO) {
        SnapshotData(
            transactions = dao.getTransactions().map { it.toDomain() },
            quotes = dao.getQuotes().map { it.toDomain() },
            stockInfos = dao.getStockInfos().map { it.toDomain() },
            feeConfig = dao.getFeeConfig()?.toDomain() ?: FeeConfig()
        )
    }

    private fun buildRefreshMessage(
        marketState: MarketState,
        successCount: Int,
        failedCount: Int,
        initializedCount: Int,
        fallbackUsedCount: Int
    ): String {
        return when (marketState) {
            MarketState.TRADING -> {
                when {
                    successCount > 0 && initializedCount > 0 && failedCount == 0 && fallbackUsedCount == 0 ->
                        "盤中已更新現價，並完成新追蹤股票初始化"
                    successCount > 0 && failedCount == 0 ->
                        "盤中已更新現價"
                    successCount > 0 ->
                        "盤中部分更新成功，部分股票保留最後價格"
                    fallbackUsedCount > 0 ->
                        "盤中部分更新失敗，已保留最後成功價格"
                    else ->
                        "盤中更新失敗，尚未取得價格"
                }
            }
            MarketState.AFTER_HOURS -> {
                when {
                    successCount > 0 && initializedCount > 0 && failedCount == 0 && fallbackUsedCount == 0 ->
                        "非交易時段，已初始化第一次價格並更新最後收盤價"
                    successCount > 0 && initializedCount > 0 ->
                        "非交易時段，已為部分新追蹤股票初始化價格，其餘顯示最後更新價格"
                    successCount > 0 && failedCount == 0 && fallbackUsedCount == 0 ->
                        "非交易時段，已更新最後收盤價"
                    successCount > 0 || fallbackUsedCount > 0 ->
                        "非交易時段，部分更新收盤價成功，其餘顯示最後更新價格"
                    else ->
                        "非交易時段，尚未取得價格"
                }
            }
        }
    }

    private suspend fun fetchIntradayQuoteFromFugle(stockCode: String): StockQuoteEntity? {
        val apiKey = BuildConfig.FUGLE_API_KEY.trim()
        if (apiKey.isBlank() || apiKey == "請填入你的FugleApiKey") {
            Log.w(tag, "Fugle skipped: API key missing, stockCode=$stockCode")
            return null
        }

        Log.d(tag, "Fugle request start: stockCode=$stockCode")

        return runCatching {
            val quote = fugleApiService.getIntradayQuote(
                apiKey = apiKey,
                symbol = stockCode
            )

            Log.d(
                tag,
                "Fugle response: stockCode=$stockCode, lastTrade=${quote.lastTrade?.price}, lastPrice=${quote.lastPrice}, closePrice=${quote.closePrice}, avgPrice=${quote.avgPrice}, openPrice=${quote.openPrice}, isClose=${quote.isClose}"
            )

            val currentPrice = quote.lastTrade?.price
                ?: quote.lastPrice
                ?: quote.closePrice
                ?: quote.avgPrice
                ?: quote.openPrice
                ?: quote.referencePrice

            if (currentPrice == null) {
                Log.w(tag, "Fugle price null: stockCode=$stockCode, all price fields are null")
                null
            } else {
                val updatedAt = formatFugleTimestamp(
                    quote.lastTrade?.time
                        ?: quote.lastUpdated
                        ?: quote.total?.time
                        ?: quote.closeTime
                ) ?: LocalDateTime.now().toString()

                Log.d(tag, "Fugle success: stockCode=$stockCode, price=$currentPrice, updatedAt=$updatedAt")

                StockQuoteEntity(
                    stockCode = stockCode,
                    currentPrice = currentPrice,
                    updatedAt = updatedAt
                )
            }
        }.onFailure { e ->
            Log.e(tag, "Fugle request failed: stockCode=$stockCode, message=${e.message}", e)
        }.getOrNull()
    }

    private fun formatFugleTimestamp(timestamp: Long?): String? {
        if (timestamp == null || timestamp <= 0L) return null

        val millis = when {
            timestamp > 10_000_000_000_000L -> timestamp / 1_000L
            timestamp > 10_000_000_000L -> timestamp
            else -> timestamp * 1_000L
        }

        return runCatching {
            LocalDateTime.ofInstant(
                Instant.ofEpochMilli(millis),
                ZoneId.systemDefault()
            ).toString()
        }.getOrNull()
    }

    private suspend fun fetchClosingQuoteFromFinMind(stockCode: String): StockQuoteEntity? {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(40)
        val token = BuildConfig.FINMIND_API_TOKEN.trim()
        val authorization = token.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

        Log.d(
            tag,
            "FinMind daily request start: stockCode=$stockCode, startDate=$startDate, endDate=$endDate"
        )

        return runCatching {
            val response = finMindApiService.getTaiwanStockPriceDaily(
                authorization = authorization,
                dataId = stockCode,
                startDate = startDate.toString(),
                endDate = endDate.toString()
            )

            val latest = response.data.lastOrNull { it.close != null }

            if (latest?.close == null) {
                Log.w(tag, "FinMind daily latest close null: stockCode=$stockCode")
                null
            } else {
                Log.d(
                    tag,
                    "FinMind daily success: stockCode=$stockCode, close=${latest.close}, date=${latest.date}"
                )

                StockQuoteEntity(
                    stockCode = stockCode,
                    currentPrice = latest.close,
                    updatedAt = latest.date
                )
            }
        }.onFailure { e ->
            Log.e(tag, "FinMind daily request failed: stockCode=$stockCode, message=${e.message}", e)
        }.getOrNull()
    }

    private suspend fun fetchMinuteQuoteFromFinMind(stockCode: String): StockQuoteEntity? {
        Log.d(tag, "FinMind minute request start: stockCode=$stockCode")

        return runCatching {
            val latest = finMindApiService.getTaiwanStockPriceMinute(stockId = stockCode)
                .data
                .lastOrNull { it.deal_price != null }

            if (latest?.deal_price == null) {
                Log.w(tag, "FinMind minute latest deal_price null: stockCode=$stockCode")
                null
            } else {
                val updatedAt = latest.date?.takeIf { it.isNotBlank() } ?: LocalDateTime.now().toString()

                Log.d(
                    tag,
                    "FinMind minute success: stockCode=$stockCode, price=${latest.deal_price}, updatedAt=$updatedAt"
                )

                StockQuoteEntity(
                    stockCode = stockCode,
                    currentPrice = latest.deal_price,
                    updatedAt = updatedAt
                )
            }
        }.onFailure { e ->
            Log.e(tag, "FinMind minute request failed: stockCode=$stockCode, message=${e.message}", e)
        }.getOrNull()
    }

    private fun determineMarketState(now: LocalDateTime = LocalDateTime.now()): MarketState {
        val isWeekday = when (now.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> false
            else -> true
        }

        val currentTime = now.toLocalTime()
        val tradingStart = LocalTime.of(9, 0)
        val tradingEnd = LocalTime.of(13, 30)

        return if (isWeekday && currentTime >= tradingStart && currentTime <= tradingEnd) {
            MarketState.TRADING
        } else {
            MarketState.AFTER_HOURS
        }
    }

    companion object {
        fun create(dao: StockDao): StockRepository {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()

            val finMindRetrofit = Retrofit.Builder()
                .baseUrl("https://api.finmindtrade.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            val fugleRetrofit = Retrofit.Builder()
                .baseUrl("https://api.fugle.tw/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            return StockRepository(
                dao = dao,
                finMindApiService = finMindRetrofit.create(FinMindApiService::class.java),
                fugleApiService = fugleRetrofit.create(FugleApiService::class.java)
            )
        }
    }
}

data class SnapshotData(
    val transactions: List<StockTransaction>,
    val quotes: List<StockQuote>,
    val stockInfos: List<StockInfo>,
    val feeConfig: FeeConfig
)

data class QuoteRefreshResult(
    val mode: QuoteRefreshMode,
    val successCount: Int,
    val failedCount: Int,
    val initializedCount: Int,
    val fallbackUsedCount: Int,
    val message: String
)

enum class QuoteRefreshMode {
    NONE,
    INTRADAY,
    CLOSING
}

private enum class MarketState {
    TRADING,
    AFTER_HOURS
}

private fun StockTransactionEntity.toDomain(): StockTransaction {
    return StockTransaction(
        id = id,
        stockCode = stockCode,
        tradeDate = tradeDate,
        transactionType = TransactionType.valueOf(transactionType),
        pricePerShare = pricePerShare,
        lotCount = lotCount,
        note = note
    )
}

private fun StockQuoteEntity.toDomain(): StockQuote {
    return StockQuote(
        stockCode = stockCode,
        currentPrice = currentPrice,
        updatedAt = updatedAt
    )
}

private fun StockInfoEntity.toDomain(): StockInfo {
    return StockInfo(
        stockCode = stockCode,
        stockName = stockName,
        industryCategory = industryCategory,
        type = type
    )
}

private fun FeeConfigEntity.toDomain(): FeeConfig {
    return FeeConfig(
        brokerageFeeRate = brokerageFeeRate,
        brokerageFeeDiscountRate = brokerageFeeDiscountRate,
        brokerageMinimumFee = brokerageMinimumFee,
        sellTaxRate = sellTaxRate,
        selectedBroker = try {
            Broker.valueOf(selectedBroker)
        } catch (e: Exception) {
            Broker.YUANTA
        }
    )
}

private fun StockTransaction.toEntity(): StockTransactionEntity {
    return StockTransactionEntity(
        id = id,
        stockCode = stockCode,
        tradeDate = tradeDate,
        transactionType = transactionType.name,
        pricePerShare = pricePerShare,
        lotCount = lotCount,
        note = note
    )
}

private fun FeeConfig.toEntity(): FeeConfigEntity {
    return FeeConfigEntity(
        id = 1,
        brokerageFeeRate = brokerageFeeRate,
        brokerageFeeDiscountRate = brokerageFeeDiscountRate,
        brokerageMinimumFee = brokerageMinimumFee,
        sellTaxRate = sellTaxRate,
        selectedBroker = selectedBroker.name
    )
}
