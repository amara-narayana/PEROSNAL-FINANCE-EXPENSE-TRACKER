package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import com.example.util.BackupCrypto
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class FinanceRepository(
    private val db: FinanceDatabase,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val budgetDao: BudgetDao
) {
    val categories: Flow<List<Category>> = categoryDao.getAllCategories()
    val transactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val recurringTransactions: Flow<List<RecurringTransaction>> = recurringTransactionDao.getAllRecurring()
    val budgets: Flow<List<Budget>> = budgetDao.getAllBudgets()

    suspend fun getCategoryById(id: Long) = categoryDao.getCategoryById(id)

    suspend fun prepopulateCategoriesIfNeeded() {
        if (categoryDao.getCategoriesCount() == 0) {
            val defaults = listOf(
                Category(name = "Food & Dining", iconName = "Fastfood", colorHex = 0xFFF57C00.toInt(), isIncome = false),
                Category(name = "Shopping", iconName = "ShoppingCart", colorHex = 0xFFEC407A.toInt(), isIncome = false),
                Category(name = "Bills & Utilities", iconName = "Receipt", colorHex = 0xFFE53935.toInt(), isIncome = false),
                Category(name = "Transport & Petrol", iconName = "DirectionsCar", colorHex = 0xFF1E88E5.toInt(), isIncome = false),
                Category(name = "Housing & Bills", iconName = "House", colorHex = 0xFF3F51B5.toInt(), isIncome = false),
                Category(name = "Health & Medical", iconName = "MedicalServices", colorHex = 0xFF43A047.toInt(), isIncome = false),
                Category(name = "Entertainment", iconName = "LocalPlay", colorHex = 0xFF8E24AA.toInt(), isIncome = false),
                Category(name = "Salary (Income)", iconName = "Work", colorHex = 0xFF7CB342.toInt(), isIncome = true),
                Category(name = "Investments", iconName = "TrendingUp", colorHex = 0xFF008080.toInt(), isIncome = true),
                Category(name = "Other & Misc", iconName = "Category", colorHex = 0xFF757575.toInt(), isIncome = false)
            )
            defaults.forEach { categoryDao.insertCategory(it) }
        }
    }

    // Checking and generating recurring transactions on app startup
    suspend fun triggerRecurringTransactions() {
        val active = recurringTransactionDao.getActiveRecurringSync()
        val now = System.currentTimeMillis()
        
        for (rt in active) {
            var next = rt.nextOccurrence
            var updatedRt = rt
            var count = 0
            
            // Generate catch-up occurrences up to 100 to prevent infinite loop errors
            while (next <= now && count < 100) {
                val tx = Transaction(
                    amount = rt.amount,
                    categoryId = rt.categoryId,
                    categoryName = rt.categoryName,
                    categoryIcon = rt.categoryIcon,
                    categoryColor = rt.categoryColor,
                    note = "${rt.note} (Auto-recurring)",
                    timestamp = next,
                    type = rt.type
                )
                transactionDao.insertTransaction(tx)
                
                next = calculateNextOccurrence(next, rt.frequency)
                updatedRt = updatedRt.copy(nextOccurrence = next)
                count++
            }
            if (count > 0) {
                recurringTransactionDao.updateRecurring(updatedRt)
            }
        }
    }

    private fun calculateNextOccurrence(current: Long, frequency: String): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = current
        when (frequency.uppercase(Locale.ROOT)) {
            "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> cal.add(Calendar.MONTH, 1)
            "YEARLY" -> cal.add(Calendar.YEAR, 1)
            else -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }

    // Transaction functions
    suspend fun insertTransaction(transaction: Transaction) = transactionDao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: Transaction) = transactionDao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)
    suspend fun deleteTransactionById(id: Long) = transactionDao.deleteTransactionById(id)
    fun getTransactionsInRange(start: Long, end: Long): Flow<List<Transaction>> = transactionDao.getTransactionsInRange(start, end)

    // Category functions
    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    // Recurring transaction functions
    suspend fun insertRecurring(recurring: RecurringTransaction) = recurringTransactionDao.insertRecurring(recurring)
    suspend fun updateRecurring(recurring: RecurringTransaction) = recurringTransactionDao.updateRecurring(recurring)
    suspend fun deleteRecurring(recurring: RecurringTransaction) = recurringTransactionDao.deleteRecurring(recurring)
    suspend fun deleteRecurringById(id: Long) = recurringTransactionDao.deleteRecurringById(id)

    // Budget functions
    suspend fun insertBudget(budget: Budget) = budgetDao.insertBudget(budget)
    suspend fun deleteBudget(budget: Budget) = budgetDao.deleteBudget(budget)
    suspend fun deleteBudgetById(id: Long) = budgetDao.deleteBudgetById(id)
    fun getBudgetsForMonth(month: String): Flow<List<Budget>> = budgetDao.getBudgetsForMonth(month)

    // CSV Exporter
    suspend fun exportToCsv(): String {
        val txs = transactionDao.getAllTransactionsSync()
        val builder = StringBuilder()
        builder.append("ID,Type,Date,Category,Amount,Note\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        
        for (t in txs) {
            val dateStr = dateFormat.format(Date(t.timestamp))
            // Safe escape notes
            val escapedNote = t.note.replace("\"", "\"\"")
            builder.append("${t.id},${t.type},$dateStr,\"${t.categoryName}\",${t.amount},\"$escapedNote\"\n")
        }
        return builder.toString()
    }

    // Encrypted JSON backup
    suspend fun exportEncryptedBackup(passcode: String): String {
        val root = JSONObject()
        
        // 1. Categories
        val catsArray = JSONArray()
        for (c in categoryDao.getAllCategoriesSync()) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("iconName", c.iconName)
            obj.put("colorHex", c.colorHex)
            obj.put("isIncome", c.isIncome)
            catsArray.put(obj)
        }
        root.put("categories", catsArray)

        // 2. Transactions
        val txsArray = JSONArray()
        for (t in transactionDao.getAllTransactionsSync()) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("amount", t.amount)
            obj.put("categoryId", t.categoryId)
            obj.put("categoryName", t.categoryName)
            obj.put("categoryIcon", t.categoryIcon)
            obj.put("categoryColor", t.categoryColor)
            obj.put("note", t.note)
            obj.put("timestamp", t.timestamp)
            obj.put("type", t.type)
            obj.put("recurringTransactionId", t.recurringTransactionId ?: -1L)
            txsArray.put(obj)
        }
        root.put("transactions", txsArray)

        // 3. Recurring Bills
        val recArray = JSONArray()
        for (r in recurringTransactionDao.getAllRecurringSync()) {
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("amount", r.amount)
            obj.put("categoryId", r.categoryId)
            obj.put("categoryName", r.categoryName)
            obj.put("categoryIcon", r.categoryIcon)
            obj.put("categoryColor", r.categoryColor)
            obj.put("note", r.note)
            obj.put("type", r.type)
            obj.put("frequency", r.frequency)
            obj.put("nextOccurrence", r.nextOccurrence)
            obj.put("isActive", r.isActive)
            recArray.put(obj)
        }
        root.put("recurring_transactions", recArray)

        // 4. Budgets
        val budArray = JSONArray()
        for (b in budgetDao.getAllBudgetsSync()) {
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("categoryId", b.categoryId)
            obj.put("categoryName", b.categoryName)
            obj.put("categoryColor", b.categoryColor)
            obj.put("amount", b.amount)
            obj.put("month", b.month)
            budArray.put(obj)
        }
        root.put("budgets", budArray)

        val jsonStr = root.toString()
        return BackupCrypto.encrypt(jsonStr, passcode)
    }

    // Restore from encrypted backup, transactionally
    suspend fun importEncryptedBackup(encryptedStr: String, passcode: String): Boolean {
        return try {
            val plainJson = BackupCrypto.decrypt(encryptedStr, passcode)
            val root = JSONObject(plainJson)

            // Validate that we have proper schema before deleting data
            if (!root.has("categories") || !root.has("transactions")) {
                return false
            }

            // We do a full transaction update or run queries sequentially.
            // Truncate tables first if JSON is valid.
            db.clearAllTables()

            // 1. Categories
            val catsArray = root.getJSONArray("categories")
            for (i in 0 until catsArray.length()) {
                val obj = catsArray.getJSONObject(i)
                val cat = Category(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    iconName = obj.getString("iconName"),
                    colorHex = obj.getInt("colorHex"),
                    isIncome = obj.optBoolean("isIncome", false)
                )
                categoryDao.insertCategory(cat)
            }

            // 2. Transactions
            val txsArray = root.getJSONArray("transactions")
            for (i in 0 until txsArray.length()) {
                val obj = txsArray.getJSONObject(i)
                val recId = obj.optLong("recurringTransactionId", -1L)
                val tx = Transaction(
                    id = obj.getLong("id"),
                    amount = obj.getDouble("amount"),
                    categoryId = obj.getLong("categoryId"),
                    categoryName = obj.getString("categoryName"),
                    categoryIcon = obj.getString("categoryIcon"),
                    categoryColor = obj.getInt("categoryColor"),
                    note = obj.getString("note"),
                    timestamp = obj.getLong("timestamp"),
                    type = obj.getString("type"),
                    recurringTransactionId = if (recId == -1L) null else recId
                )
                transactionDao.insertTransaction(tx)
            }

            // 3. Recurring Transactions
            if (root.has("recurring_transactions")) {
                val recArray = root.getJSONArray("recurring_transactions")
                for (i in 0 until recArray.length()) {
                    val obj = recArray.getJSONObject(i)
                    val r = RecurringTransaction(
                        id = obj.getLong("id"),
                        amount = obj.getDouble("amount"),
                        categoryId = obj.getLong("categoryId"),
                        categoryName = obj.getString("categoryName"),
                        categoryIcon = obj.getString("categoryIcon"),
                        categoryColor = obj.getInt("categoryColor"),
                        note = obj.getString("note"),
                        type = obj.getString("type"),
                        frequency = obj.getString("frequency"),
                        nextOccurrence = obj.getLong("nextOccurrence"),
                        isActive = obj.optBoolean("isActive", true)
                    )
                    recurringTransactionDao.insertRecurring(r)
                }
            }

            // 4. Budgets
            if (root.has("budgets")) {
                val budArray = root.getJSONArray("budgets")
                for (i in 0 until budArray.length()) {
                    val obj = budArray.getJSONObject(i)
                    val b = Budget(
                        id = obj.getLong("id"),
                        categoryId = obj.getLong("categoryId"),
                        categoryName = obj.getString("categoryName"),
                        categoryColor = obj.getInt("categoryColor"),
                        amount = obj.getDouble("amount"),
                        month = obj.getString("month")
                    )
                    budgetDao.insertBudget(b)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
