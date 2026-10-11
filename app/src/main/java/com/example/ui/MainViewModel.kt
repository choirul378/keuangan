package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BackupManager
import com.example.data.TransactionRepository
import com.example.model.CategoryRepository
import com.example.model.TransactionCategory
import com.example.model.TransactionEntity
import com.example.model.TransactionType
import com.example.ui.components.BarChartGroup
import com.example.ui.components.CategorySlice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class MonthlyAnalytics(
    val averageDailyExpense: Long = 0L,
    val topExpenseCategory: TransactionCategory? = null,
    val topExpenseAmount: Long = 0L,
    val topExpensePercentage: Float = 0f,
    val netCashFlow: Long = 0L,
    val totalTransactionsCount: Int = 0
)

data class FinanceUiState(
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH), // 0-indexed
    val allTransactions: List<TransactionEntity> = emptyList(),
    val monthlyTransactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netBalance: Long = 0L,
    val monthlyBudget: Long = 5000000L, // Rp 5.000.000 default budget
    val expenseCategorySlices: List<CategorySlice> = emptyList(),
    val incomeCategorySlices: List<CategorySlice> = emptyList(),
    val weeklyBarData: List<BarChartGroup> = emptyList(),
    val analytics: MonthlyAnalytics = MonthlyAnalytics(),
    val selectedFilterType: TransactionType? = null,
    val searchQuery: String = "",
    val editingTransaction: TransactionEntity? = null,
    val showAddEditSheet: Boolean = false,
    val showBudgetDialog: Boolean = false,
    val showBackupRestoreDialog: Boolean = false,
    val userFeedbackMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _monthlyBudget = MutableStateFlow(5000000L)
    private val _selectedFilterType = MutableStateFlow<TransactionType?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _editingTransaction = MutableStateFlow<TransactionEntity?>(null)
    private val _showAddEditSheet = MutableStateFlow(false)
    private val _showBudgetDialog = MutableStateFlow(false)
    private val _showBackupRestoreDialog = MutableStateFlow(false)
    private val _userFeedbackMessage = MutableStateFlow<String?>(null)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database.transactionDao())

        viewModelScope.launch {
            repository.checkAndSeedIfEmpty()
        }
    }

    val uiState: StateFlow<FinanceUiState> = combine(
        repository.allTransactions,
        _selectedYear,
        _selectedMonth,
        _monthlyBudget,
        _selectedFilterType,
        _searchQuery,
        _editingTransaction,
        _showAddEditSheet,
        _showBudgetDialog,
        _showBackupRestoreDialog,
        _userFeedbackMessage
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val allTx = params[0] as List<TransactionEntity>
        val year = params[1] as Int
        val month = params[2] as Int
        val budget = params[3] as Long
        val filterType = params[4] as TransactionType?
        val query = params[5] as String
        val editingTx = params[6] as TransactionEntity?
        val showSheet = params[7] as Boolean
        val showBudget = params[8] as Boolean
        val showBackup = params[9] as Boolean
        val feedback = params[10] as String?

        // Calculate start and end millis for selected month
        val calStart = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            set(year, month, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val startMillis = calStart.timeInMillis
        val endMillis = calEnd.timeInMillis

        // Filter transactions for selected month
        val monthTx = allTx.filter { it.dateMillis in startMillis..endMillis }

        // Filtered by search query and transaction type
        val filtered = monthTx.filter { item ->
            val matchesType = when (filterType) {
                null -> true
                TransactionType.INCOME -> item.type == "INCOME"
                TransactionType.EXPENSE -> item.type == "EXPENSE"
            }
            val matchesQuery = if (query.isBlank()) true else {
                val cat = CategoryRepository.findCategory(item.categoryId, item.transactionType)
                item.title.contains(query, ignoreCase = true) ||
                        item.note.contains(query, ignoreCase = true) ||
                        item.wallet.contains(query, ignoreCase = true) ||
                        cat.name.contains(query, ignoreCase = true)
            }
            matchesType && matchesQuery
        }

        val totalIncome = monthTx.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense

        // Calculate Expense slices
        val expenseItems = monthTx.filter { it.type == "EXPENSE" }
        val expenseTotal = totalExpense.coerceAtLeast(1L)
        val expenseCategoryMap = expenseItems.groupBy { it.categoryId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        val expenseSlices = expenseCategoryMap.map { (catId, amt) ->
            val cat = CategoryRepository.findCategory(catId, TransactionType.EXPENSE)
            val pct = (amt.toFloat() / expenseTotal) * 100f
            CategorySlice(cat, amt, pct)
        }.sortedByDescending { it.amount }

        // Calculate Income slices
        val incomeItems = monthTx.filter { it.type == "INCOME" }
        val incomeTotal = totalIncome.coerceAtLeast(1L)
        val incomeCategoryMap = incomeItems.groupBy { it.categoryId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        val incomeSlices = incomeCategoryMap.map { (catId, amt) ->
            val cat = CategoryRepository.findCategory(catId, TransactionType.INCOME)
            val pct = (amt.toFloat() / incomeTotal) * 100f
            CategorySlice(cat, amt, pct)
        }.sortedByDescending { it.amount }

        // Calculate Weekly Breakdown (Minggu 1 to Minggu 5)
        val weeklyGroups = mutableListOf<BarChartGroup>()
        val daysInMonth = calStart.getActualMaximum(Calendar.DAY_OF_MONTH)
        val weekRanges = listOf(
            1 to 7,
            8 to 14,
            15 to 21,
            22 to 28,
            29 to daysInMonth
        )

        weekRanges.forEachIndexed { idx, (startDay, endDay) ->
            if (startDay <= daysInMonth) {
                val actualEnd = endDay.coerceAtMost(daysInMonth)
                val wStart = Calendar.getInstance().apply {
                    set(year, month, startDay, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val wEnd = Calendar.getInstance().apply {
                    set(year, month, actualEnd, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                val weekTx = monthTx.filter { it.dateMillis in wStart..wEnd }
                val wIncome = weekTx.filter { it.type == "INCOME" }.sumOf { it.amount }
                val wExpense = weekTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }

                weeklyGroups.add(
                    BarChartGroup(
                        label = "Mgg ${idx + 1}",
                        income = wIncome,
                        expense = wExpense
                    )
                )
            }
        }

        // Calculate Analytics
        val daysPassed = if (year == Calendar.getInstance().get(Calendar.YEAR) &&
            month == Calendar.getInstance().get(Calendar.MONTH)
        ) {
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        } else {
            daysInMonth
        }
        val avgDaily = if (daysPassed > 0) totalExpense / daysPassed else 0L

        val topExpenseSlice = expenseSlices.firstOrNull()
        val analytics = MonthlyAnalytics(
            averageDailyExpense = avgDaily,
            topExpenseCategory = topExpenseSlice?.category,
            topExpenseAmount = topExpenseSlice?.amount ?: 0L,
            topExpensePercentage = topExpenseSlice?.percentage ?: 0f,
            netCashFlow = netBalance,
            totalTransactionsCount = monthTx.size
        )

        FinanceUiState(
            selectedYear = year,
            selectedMonth = month,
            allTransactions = allTx,
            monthlyTransactions = monthTx,
            filteredTransactions = filtered,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance,
            monthlyBudget = budget,
            expenseCategorySlices = expenseSlices,
            incomeCategorySlices = incomeSlices,
            weeklyBarData = weeklyGroups,
            analytics = analytics,
            selectedFilterType = filterType,
            searchQuery = query,
            editingTransaction = editingTx,
            showAddEditSheet = showSheet,
            showBudgetDialog = showBudget,
            showBackupRestoreDialog = showBackup,
            userFeedbackMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceUiState()
    )

    fun selectPreviousMonth() {
        if (_selectedMonth.value == 0) {
            _selectedMonth.value = 11
            _selectedYear.value -= 1
        } else {
            _selectedMonth.value -= 1
        }
    }

    fun selectNextMonth() {
        if (_selectedMonth.value == 11) {
            _selectedMonth.value = 0
            _selectedYear.value += 1
        } else {
            _selectedMonth.value += 1
        }
    }

    fun selectCurrentMonth() {
        val now = Calendar.getInstance()
        _selectedYear.value = now.get(Calendar.YEAR)
        _selectedMonth.value = now.get(Calendar.MONTH)
    }

    fun setFilterType(type: TransactionType?) {
        _selectedFilterType.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMonthlyBudget(budget: Long) {
        _monthlyBudget.value = budget
        _showBudgetDialog.value = false
    }

    fun openAddTransaction() {
        _editingTransaction.value = null
        _showAddEditSheet.value = true
    }

    fun openEditTransaction(transaction: TransactionEntity) {
        _editingTransaction.value = transaction
        _showAddEditSheet.value = true
    }

    fun dismissAddEditSheet() {
        _editingTransaction.value = null
        _showAddEditSheet.value = false
    }

    fun openBudgetDialog() {
        _showBudgetDialog.value = true
    }

    fun dismissBudgetDialog() {
        _showBudgetDialog.value = false
    }

    fun saveTransaction(
        title: String,
        amount: Long,
        type: TransactionType,
        categoryId: String,
        wallet: String,
        dateMillis: Long,
        note: String
    ) {
        viewModelScope.launch {
            val current = _editingTransaction.value
            if (current != null) {
                repository.update(
                    current.copy(
                        title = title,
                        amount = amount,
                        type = type.name,
                        categoryId = categoryId,
                        wallet = wallet,
                        dateMillis = dateMillis,
                        note = note
                    )
                )
            } else {
                repository.insert(
                    TransactionEntity(
                        title = title,
                        amount = amount,
                        type = type.name,
                        categoryId = categoryId,
                        wallet = wallet,
                        dateMillis = dateMillis,
                        note = note
                    )
                )
            }
            dismissAddEditSheet()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun openBackupRestoreDialog() {
        _showBackupRestoreDialog.value = true
    }

    fun dismissBackupRestoreDialog() {
        _showBackupRestoreDialog.value = false
    }

    fun showFeedback(message: String) {
        _userFeedbackMessage.value = message
    }

    fun clearFeedback() {
        _userFeedbackMessage.value = null
    }

    suspend fun getBackupJson(): String {
        val all = repository.getAllTransactionsList()
        return BackupManager.generateBackupJson(all, _monthlyBudget.value)
    }

    suspend fun restoreFromJson(jsonString: String, replaceExisting: Boolean): Result<Int> {
        return try {
            val payload = BackupManager.parseBackupJson(jsonString)
            if (payload.transactions.isEmpty()) {
                Result.failure(Exception("File cadangan tidak memuat data transaksi yang valid"))
            } else {
                repository.restoreTransactions(payload.transactions, replaceExisting)
                if (payload.monthlyBudget > 0) {
                    _monthlyBudget.value = payload.monthlyBudget
                }
                _userFeedbackMessage.value = "Berhasil memulihkan ${payload.transactions.size} transaksi"
                _showBackupRestoreDialog.value = false
                Result.success(payload.transactions.size)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
