// StockDatabase.kt
package com.example.stockcosttracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.stockcosttracker.data.local.entity.DailyPortfolioSnapshotEntity
import com.example.stockcosttracker.data.local.entity.FeeConfigEntity
import com.example.stockcosttracker.data.local.entity.PortfolioSnapshotEntity
import com.example.stockcosttracker.data.local.entity.StockInfoEntity
import com.example.stockcosttracker.data.local.entity.StockQuoteEntity
import com.example.stockcosttracker.data.local.entity.StockTransactionEntity

@Database(
    entities = [
        StockTransactionEntity::class,
        StockQuoteEntity::class,
        FeeConfigEntity::class,
        StockInfoEntity::class,
        PortfolioSnapshotEntity::class,
        DailyPortfolioSnapshotEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StockDatabase : RoomDatabase() {

    abstract fun stockDao(): StockDao

    companion object {
        @Volatile
        private var INSTANCE: StockDatabase? = null

        fun getInstance(context: Context): StockDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StockDatabase::class.java,
                    "stock_cost_tracker.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}