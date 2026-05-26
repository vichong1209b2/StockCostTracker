// StockDao
// V1.1
package com.example.stockcosttracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.stockcosttracker.data.local.entity.DailyPortfolioSnapshotEntity
import com.example.stockcosttracker.data.local.entity.FeeConfigEntity
import com.example.stockcosttracker.data.local.entity.PortfolioSnapshotEntity
import com.example.stockcosttracker.data.local.entity.StockInfoEntity
import com.example.stockcosttracker.data.local.entity.StockQuoteEntity
import com.example.stockcosttracker.data.local.entity.StockTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: StockTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: StockTransactionEntity)

    @Query("""
        SELECT * FROM stock_transactions
        ORDER BY tradeDate DESC, id DESC
    """)
    fun observeTransactions(): Flow<List<StockTransactionEntity>>

    @Query("""
        SELECT * FROM stock_transactions
        ORDER BY tradeDate ASC, id ASC
    """)
    suspend fun getTransactions(): List<StockTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuotes(items: List<StockQuoteEntity>)

    @Query("""
        SELECT * FROM stock_quotes
        ORDER BY stockCode ASC
    """)
    fun observeQuotes(): Flow<List<StockQuoteEntity>>

    @Query("""
        SELECT * FROM stock_quotes
        ORDER BY stockCode ASC
    """)
    suspend fun getQuotes(): List<StockQuoteEntity>

    @Query("DELETE FROM stock_quotes")
    suspend fun clearQuotes()

    @Query("""
        DELETE FROM stock_quotes
        WHERE stockCode NOT IN (:stockCodes)
    """)
    suspend fun deleteQuotesNotIn(stockCodes: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStockInfos(items: List<StockInfoEntity>)

    @Query("""
        SELECT * FROM stock_infos
        ORDER BY stockCode ASC
    """)
    fun observeStockInfos(): Flow<List<StockInfoEntity>>

    @Query("""
        SELECT * FROM stock_infos
        ORDER BY stockCode ASC
    """)
    suspend fun getStockInfos(): List<StockInfoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeeConfig(config: FeeConfigEntity)

    @Query("""
        SELECT * FROM fee_config
        WHERE id = 1
        LIMIT 1
    """)
    fun observeFeeConfig(): Flow<FeeConfigEntity?>

    @Query("""
        SELECT * FROM fee_config
        WHERE id = 1
        LIMIT 1
    """)
    suspend fun getFeeConfig(): FeeConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortfolioSnapshot(snapshot: PortfolioSnapshotEntity)

    @Query("""
        SELECT * FROM portfolio_snapshots
        ORDER BY capturedAt ASC
    """)
    fun observePortfolioSnapshots(): Flow<List<PortfolioSnapshotEntity>>

    @Query("""
        SELECT * FROM portfolio_snapshots
        ORDER BY capturedAt ASC
    """)
    suspend fun getPortfolioSnapshots(): List<PortfolioSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyPortfolioSnapshot(snapshot: DailyPortfolioSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyPortfolioSnapshots(items: List<DailyPortfolioSnapshotEntity>)

    @Query("""
        SELECT * FROM daily_portfolio_snapshots
        ORDER BY snapshotDate ASC
    """)
    fun observeDailyPortfolioSnapshots(): Flow<List<DailyPortfolioSnapshotEntity>>

    @Query("""
        SELECT * FROM daily_portfolio_snapshots
        ORDER BY snapshotDate ASC
    """)
    suspend fun getDailyPortfolioSnapshots(): List<DailyPortfolioSnapshotEntity>

    @Query("""
        SELECT MAX(snapshotDate) FROM daily_portfolio_snapshots
    """)
    suspend fun getLatestDailySnapshotDate(): String?

    @Query("""
        SELECT * FROM daily_portfolio_snapshots
        ORDER BY snapshotDate DESC
        LIMIT :limit
    """)
    suspend fun getLatestDailyPortfolioSnapshots(limit: Int): List<DailyPortfolioSnapshotEntity>
}