package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.RecurringTransaction
import com.example.data.model.Budget

@Database(
    entities = [
        Category::class,
        Transaction::class,
        RecurringTransaction::class,
        Budget::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getDatabase(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_tracker_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                @Suppress("KotlinConstantConditions")
                INSTANCE = instance
                instance
            }
        }
    }
}
