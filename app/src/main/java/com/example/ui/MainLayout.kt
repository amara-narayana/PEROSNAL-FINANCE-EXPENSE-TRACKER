package com.example.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.data.model.Category
import com.example.ui.screens.*
import com.example.ui.theme.ColorExpense
import com.example.ui.theme.ColorIncome
import java.text.SimpleDateFormat
import java.util.*

enum class FinanceTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val tag: String) {
    DASHBOARD("Ledger", Icons.Default.Dashboard, "tab_dashboard"),
    ANALYTICS("Analytics", Icons.Default.PieChart, "tab_analytics"),
    CALENDAR("Calendar", Icons.Default.CalendarMonth, "tab_calendar"),
    BUDGETS("budgets", Icons.Default.AssignmentTurnedIn, "tab_budgets"),
    TOOLS("Tools", Icons.Default.Settings, "tab_tools")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainLayout(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(FinanceTab.DASHBOARD) }
    var showAddDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Observe flows from ViewModel
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val currentMonthTransactions by viewModel.currentMonthTransactions.collectAsStateWithLifecycle()
    val currentMonthBudgets by viewModel.currentMonthBudgets.collectAsStateWithLifecycle()
    val recurringTransactions by viewModel.recurringTransactions.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    // Observe active toast signals
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                FinanceTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title.capitalize(Locale.getDefault()), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        },
        floatingActionButton = {
            // Show Add FAB on transactional segments: Dashboard, Calendar, budgets
            if (activeTab == FinanceTab.DASHBOARD || activeTab == FinanceTab.CALENDAR) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                        .testTag("quick_add_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(28.dp))
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                FinanceTab.DASHBOARD -> DashboardTab(
                    transactions = currentMonthTransactions,
                    categories = categories,
                    budgets = currentMonthBudgets,
                    selectedMonth = selectedMonth,
                    onMonthOffset = { offset -> viewModel.selectMonth(offset) },
                    onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) }
                )

                FinanceTab.ANALYTICS -> AnalyticsTab(
                    transactions = currentMonthTransactions
                )

                FinanceTab.CALENDAR -> CalendarTab(
                    transactions = currentMonthTransactions,
                    selectedMonth = selectedMonth,
                    onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) }
                )

                FinanceTab.BUDGETS -> BudgetsTab(
                    categories = categories,
                    budgets = currentMonthBudgets,
                    transactions = currentMonthTransactions,
                    recurringTransactions = recurringTransactions,
                    onSetBudget = { cat, amt -> viewModel.setBudget(cat, amt) },
                    onAddRecurring = { amt, cat, note, type, freq -> viewModel.addRecurringTransaction(amt, cat, note, type, freq) },
                    onDeleteRecurring = { rt -> viewModel.deleteRecurring(rt) },
                    onToggleRecurringActive = { rt -> viewModel.toggleRecurringActive(rt) }
                )

                FinanceTab.TOOLS -> ToolsTab(
                    onExportBackup = { pass, cb -> viewModel.exportBackup(pass, cb) },
                    onImportBackup = { enc, pass, cb -> viewModel.importBackup(enc, pass, cb) },
                    onExportCsv = {
                        coroutineScope.launchInFlowCollection(viewModel.getCsvString(), context)
                    },
                    onAddCustomCategory = { name, icon, col, isInc -> viewModel.addCategory(name, icon, col, isInc) },
                    onWipeDatabase = {
                        // Reset everything in database and pre-populate standard categories
                        viewModel.importBackup("{}", "") { // feeding blank triggers reset safely under custom wipe action
                            // clear database
                            Toast.makeText(context, "Ledger records wiped clean.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    // Comprehensive transaction input sheet dialog modal
    if (showAddDialog) {
        AddTransactionDialog(
            categories = categories,
            onDismiss = { showAddDialog = false },
            onSave = { amount, category, note, timestamp, type ->
                viewModel.addTransaction(amount, category, note, timestamp, type)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddTransactionDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Double, Category, String, Long, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var trxType by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
    var selectedCategoryIndex by remember { mutableStateOf(-1) }

    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    val context = LocalContext.current
    val filteredCategories = categories.filter { it.isIncome == (trxType == "INCOME") }

    val dateLabel = remember(selectedTimestamp) {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.format(Date(selectedTimestamp))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Track Expense or Income", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Expense vs Income toggle buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("EXPENSE" to "Expense", "INCOME" to "Income").forEach { (tp, valLbl) ->
                        val isSel = trxType == tp
                        Button(
                            onClick = {
                                trxType = tp
                                selectedCategoryIndex = -1 // Reset selection on toggle
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) {
                                    if (tp == "INCOME") ColorIncome else ColorExpense
                                } else Color.Transparent,
                                contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(valLbl, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Numeric Amount keyboard
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_tx_amount_input")
                )

                // Notes Field
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Optional Notes or tag") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_tx_note_input")
                )

                // Date Picker trigger button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable {
                            val c = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newC = Calendar.getInstance()
                                    newC.set(y, m, d)
                                    selectedTimestamp = newC.timeInMillis
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, size = 16.dp, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transaction Date", fontSize = 13.sp)
                    }
                    Text(dateLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }

                Text("Associated Category", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                // Visual grid selection of categories
                if (filteredCategories.isEmpty()) {
                    Text("No categories found. Build one in Tools.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("add_tx_categories_grid"),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(filteredCategories) { index, cat ->
                            val isSelected = selectedCategoryIndex == index
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                border = if (isSelected) BorderStroke(2.dp, Color(cat.colorHex)) else null,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(cat.colorHex).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCategoryIndex = index }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(cat.colorHex).copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = IconMapper.getIconByName(cat.iconName),
                                            contentDescription = null,
                                            tint = Color(cat.colorHex),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = cat.name,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        color = if (isSelected) Color(cat.colorHex) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amtVal = amountText.toDoubleOrNull() ?: 0.0
                    val chosenCat = filteredCategories.getOrNull(selectedCategoryIndex)
                    if (amtVal > 0 && chosenCat != null) {
                        onSave(amtVal, chosenCat, noteText, selectedTimestamp, trxType)
                    } else if (amtVal <= 0) {
                        Toast.makeText(context, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Select a category.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.testTag("add_tx_confirm_btn")
            ) {
                Text("Track Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Inline helper to collect CSV sharing flows on Android cleanly using ACTION_SEND
private fun kotlinx.coroutines.CoroutineScope.launchInFlowCollection(flow: kotlinx.coroutines.flow.Flow<String>, context: android.content.Context) {
    this.launch {
        flow.collect { csvContent ->
            try {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, csvContent)
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Finance Tracker Ledger Export")
                }
                val shareIntent = Intent.createChooser(sendIntent, "Export Ledger CSV")
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(shareIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Spreadsheet share failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// Compact size vector icon helper
@Composable
private fun Icon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    size: androidx.compose.ui.unit.Dp,
    tint: Color
) {
    androidx.compose.material3.Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size)
    )
}
