package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String, // Material icon identifier (e.g., "Category", "ShoppingCart", "Fastfood", "DirectionsCar", "House", "MedicalServices", "TrendingUp", "Work")
    val colorHex: Int, // Hex value of the color for the UI
    val isIncome: Boolean = false
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String, // Cached for convenience & speed
    val categoryIcon: String, // Cached
    val categoryColor: Int, // Cached
    val note: String,
    val timestamp: Long, // Date-time of transaction in epoch milliseconds
    val type: String, // "EXPENSE" or "INCOME"
    val recurringTransactionId: Long? = null // Links back to recurring template if generated
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Int,
    val note: String,
    val type: String, // "EXPENSE" or "INCOME"
    val frequency: String, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val nextOccurrence: Long, // Epoch ms when the transaction should next trigger
    val isActive: Boolean = true
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val categoryName: String, // Cached for UI speed
    val categoryColor: Int, // Cached
    val amount: Double,
    val month: String // Month of budget in "YYYY-MM" format
)
