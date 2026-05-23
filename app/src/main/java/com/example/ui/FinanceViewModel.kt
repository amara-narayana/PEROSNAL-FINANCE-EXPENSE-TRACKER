package com.example.ui

import android.app.Application
import androidx.lifecycle.*
import com.example.data.repository.FinanceRepository
import com.example.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {

    // Current Month Filter ("YYYY-MM")
    private val _selectedMonth = MutableStateFlow("")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    // Backup & export user notices
    private val _toastMessage = MutableSharedFlow<String>(replay = 0)
    val toastMessage = _toastMessage.asSharedFlow()

    // Database entities
    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringTransactions: StateFlow<List<RecurringTransaction>> = repository.recurringTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> = repository.budgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month filtered transactions
    val currentMonthTransactions: StateFlow<List<Transaction>> = combine(
        transactions,
        _selectedMonth
    ) { txs, month ->
        if (month.isEmpty()) {
            txs
        } else {
            val format = SimpleDateFormat("yyyy-MM", Locale.US)
            txs.filter { t ->
                val txMonth = format.format(Date(t.timestamp))
                txMonth == month
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month filtered budgets
    val currentMonthBudgets: StateFlow<List<Budget>> = combine(
        budgets,
        _selectedMonth
    ) { buds, month ->
        buds.filter { b -> b.month == month }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Set default month to current local month
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        _selectedMonth.value = sdf.format(Date())

        viewModelScope.launch {
            // First time preparation
            repository.prepopulateCategoriesIfNeeded()
            // Check recurring items catch up
            repository.triggerRecurringTransactions()
        }
    }

    fun selectMonth(monthOffset: Int) {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        try {
            val current = sdf.parse(_selectedMonth.value) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = current
            cal.add(Calendar.MONTH, monthOffset)
            _selectedMonth.value = sdf.format(cal.time)
        } catch (e: Exception) {
            _selectedMonth.value = sdf.format(Date())
        }
    }

    // Actions
    fun addTransaction(amount: Double, category: Category, note: String, timestamp: Long, type: String) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    amount = amount,
                    categoryId = category.id,
                    categoryName = category.name,
                    categoryIcon = category.iconName,
                    categoryColor = category.colorHex,
                    note = note,
                    timestamp = timestamp,
                    type = type
                )
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun addCategory(name: String, iconName: String, colorHex: Int, isIncome: Boolean) {
        viewModelScope.launch {
            repository.insertCategory(
                Category(
                    name = name,
                    iconName = iconName,
                    colorHex = colorHex,
                    isIncome = isIncome
                )
            )
        }
    }

    fun addRecurringTransaction(amount: Double, category: Category, note: String, type: String, frequency: String) {
        viewModelScope.launch {
            // Find next occurrence starting from today
            val cal = Calendar.getInstance()
            // Clear hour-minute to align on date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            
            repository.insertRecurring(
                RecurringTransaction(
                    amount = amount,
                    categoryId = category.id,
                    categoryName = category.name,
                    categoryIcon = category.iconName,
                    categoryColor = category.colorHex,
                    note = note,
                    type = type,
                    frequency = frequency,
                    nextOccurrence = cal.timeInMillis,
                    isActive = true
                )
            )
            // Trigger check to see if it should run immediately
            repository.triggerRecurringTransactions()
        }
    }

    fun deleteRecurring(recurring: RecurringTransaction) {
        viewModelScope.launch {
            repository.deleteRecurring(recurring)
        }
    }

    fun toggleRecurringActive(recurring: RecurringTransaction) {
        viewModelScope.launch {
            repository.updateRecurring(recurring.copy(isActive = !recurring.isActive))
        }
    }

    fun setBudget(category: Category, amount: Double) {
        viewModelScope.launch {
            // Check if budget for category AND selectedMonth already exists
            val currentMonthVal = _selectedMonth.value
            val existing = currentMonthBudgets.value.find { b -> b.categoryId == category.id && b.month == currentMonthVal }
            
            if (existing != null) {
                if (amount <= 0.0) {
                    repository.deleteBudget(existing)
                } else {
                    repository.insertBudget(existing.copy(amount = amount))
                }
            } else if (amount > 0.0) {
                repository.insertBudget(
                    Budget(
                        categoryId = category.id,
                        categoryName = category.name,
                        categoryColor = category.colorHex,
                        amount = amount,
                        month = currentMonthVal
                    )
                )
            }
        }
    }

    // CSV helper
    fun getCsvString(): Flow<String> = flow {
        emit(repository.exportToCsv())
    }

    // Backups
    fun exportBackup(passcode: String, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val base64Enc = repository.exportEncryptedBackup(passcode)
                onComplete(base64Enc)
                _toastMessage.emit("Backup encrypted successfully!")
            } catch (e: Exception) {
                onComplete(null)
                _toastMessage.emit("Failed to create encrypted backup.")
            }
        }
    }

    fun importBackup(payloadEncrypted: String, passcode: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.importEncryptedBackup(payloadEncrypted.trim(), passcode)
                if (success) {
                    onComplete(true)
                    _toastMessage.emit("Database restored successfully!")
                    // Trigger catch-up routines
                    repository.triggerRecurringTransactions()
                } else {
                    onComplete(false)
                    _toastMessage.emit("Invalid passcode or corrupted backup payload.")
                }
            } catch (e: Exception) {
                onComplete(false)
                _toastMessage.emit("Failed to decrypt: verify passcode and payload.")
            }
        }
    }
}

class FinanceViewModelFactory(private val repository: FinanceRepository) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FinanceViewModel(repository) as T
    }
}
