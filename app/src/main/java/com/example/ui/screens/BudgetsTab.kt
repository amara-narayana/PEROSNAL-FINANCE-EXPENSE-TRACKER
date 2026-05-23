package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.data.model.RecurringTransaction
import com.example.data.model.Transaction
import com.example.ui.IconMapper
import com.example.ui.theme.ColorBudget
import com.example.ui.theme.ColorExpense
import com.example.ui.theme.ColorIncome
import com.example.ui.theme.ColorNeutral
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BudgetsTab(
    categories: List<Category>,
    budgets: List<Budget>,
    transactions: List<Transaction>,
    recurringTransactions: List<RecurringTransaction>,
    onSetBudget: (Category, Double) -> Unit,
    onAddRecurring: (Double, Category, String, String, String) -> Unit, // amount, category, note, type, frequency
    onDeleteRecurring: (RecurringTransaction) -> Unit,
    onToggleRecurringActive: (RecurringTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    var subSectionState by remember { mutableStateOf("BUDGETS") } // "BUDGETS" or "RECURRING"

    // Dialog trigger state
    var showBudgetDialogForCategory by remember { mutableStateOf<Category?>(null) }
    var showRecurringDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Dual Selection Pill Tab Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("BUDGETS" to "Category Budgets", "RECURRING" to "Recurring Schedules").forEach { (secKey, label) ->
                val isSelected = subSectionState == secKey
                Button(
                    onClick = { subSectionState = secKey },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("budget_sub_tab_$secKey")
                ) {
                    Text(text = label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (subSectionState == "BUDGETS") {
            // Category Budgets Ledger List
            val categoryBillings = remember(transactions) {
                transactions.filter { it.type == "EXPENSE" }
                    .groupBy { it.categoryId }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }
            }

            // Exclude Income Categories from Expenses Budgets setup to keep things tidy
            val expenseCategories = categories.filter { !it.isIncome }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("budget_category_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 72.dp)
            ) {
                items(expenseCategories) { cat ->
                    val allocatedBudget = budgets.find { b -> b.categoryId == cat.id }
                    val currentSpent = categoryBillings[cat.id] ?: 0.0

                    BudgetCard(
                        category = cat,
                        spentAmount = currentSpent,
                        budget = allocatedBudget,
                        onEditClicked = { showBudgetDialogForCategory = cat }
                    )
                }
            }
        } else {
            // Recurring schedule lists
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Automated Repeats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { showRecurringDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_recurring_trigger_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Schedule", fontSize = 12.sp)
                }
            }

            if (recurringTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = ColorNeutral.copy(alpha = 0.3f),
                            modifier = Modifier.size(54.dp).padding(bottom = 6.dp)
                        )
                        Text(
                            text = "No recurring schedules created.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorNeutral
                        )
                        Text(
                            text = "Set up repetitive monthly utility bills, rentals, or paychecks here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorNeutral,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("recurring_schedules_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(recurringTransactions, key = { it.id }) { rt ->
                        RecurringRow(
                            recurring = rt,
                            onToggleActive = { onToggleRecurringActive(rt) },
                            onDelete = { onDeleteRecurring(rt) }
                        )
                    }
                }
            }
        }
    }

    // Modal dialog for Budget Assignment
    if (showBudgetDialogForCategory != null) {
        val activeCat = showBudgetDialogForCategory!!
        val currentBudgetAmount = budgets.find { b -> b.categoryId == activeCat.id }?.amount ?: 0.0
        var amountText by remember { mutableStateOf(if (currentBudgetAmount > 0.0) currentBudgetAmount.toString() else "") }

        AlertDialog(
            onDismissRequest = { showBudgetDialogForCategory = null },
            title = { Text("Set Monthly Budget limit") },
            text = {
                Column {
                    Text(
                        text = "Set a budget cap for ${activeCat.name}. Enter 0 to clear.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Budget Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_amount_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amtVal = amountText.toDoubleOrNull() ?: 0.0
                        onSetBudget(activeCat, amtVal)
                        showBudgetDialogForCategory = null
                    },
                    modifier = Modifier.testTag("budget_save_btn")
                ) {
                    Text("Save Cap")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialogForCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal dialog to add recurring template
    if (showRecurringDialog) {
        var amountText by remember { mutableStateOf("") }
        var noteText by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
        var frequencyText by remember { mutableStateOf("MONTHLY") } // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
        var selectedCategoryIndex by remember { mutableStateOf(0) }

        val activeCategories = categories.filter { it.isIncome == (selectedType == "INCOME") }

        AlertDialog(
            onDismissRequest = { showRecurringDialog = false },
            title = { Text("Add Recurring Template") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("EXPENSE" to "Expense", "INCOME" to "Income").forEach { (tp, valLbl) ->
                            val isSel = selectedType == tp
                            Button(
                                onClick = {
                                    selectedType = tp
                                    selectedCategoryIndex = 0
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(valLbl, fontSize = 12.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Repeating Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note / Template Name (e.g. rent, Netflix)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Frequency selector
                    Text("Repeat Interval Schedule", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY").forEach { freq ->
                            val isSel = frequencyText == freq
                            Box(
                                modifier = Modifier
                                    .weight(1.0f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { frequencyText = freq }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = freq,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Category dropdown list simulator
                    if (activeCategories.isNotEmpty()) {
                        Text("Category Association", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        var expandedCatList by remember { mutableStateOf(false) }
                        val currCat = activeCategories.getOrNull(selectedCategoryIndex) ?: activeCategories.first()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .clickable { expandedCatList = true }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(currCat.colorHex).copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = IconMapper.getIconByName(currCat.iconName),
                                        contentDescription = null,
                                        tint = Color(currCat.colorHex),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(currCat.name, fontSize = 13.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = expandedCatList,
                                onDismissRequest = { expandedCatList = false }
                            ) {
                                activeCategories.forEachIndexed { index, cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCategoryIndex = index
                                            expandedCatList = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        val chosenCat = activeCategories.getOrNull(selectedCategoryIndex) ?: activeCategories.firstOrNull()
                        if (amt > 0 && chosenCat != null) {
                            onAddRecurring(amt, chosenCat, noteText, selectedType, frequencyText)
                            showRecurringDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_recurring_btn")
                ) {
                    Text("Add Template")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecurringDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BudgetCard(
    category: Category,
    spentAmount: Double,
    budget: Budget?,
    onEditClicked: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(category.colorHex).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = IconMapper.getIconByName(category.iconName),
                            contentDescription = null,
                            tint = Color(category.colorHex),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (budget == null) "No budget cap set" else "Cap: $%,.2f".format(budget.amount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                IconButton(
                    onClick = onEditClicked,
                    modifier = Modifier.testTag("edit_budget_btn_${category.id}")
                ) {
                    Icon(
                        imageVector = if (budget == null) Icons.Default.AddCircleOutline else Icons.Default.Edit,
                        contentDescription = "Set Cap",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (budget != null) {
                Spacer(modifier = Modifier.height(12.dp))

                val spentRatio = (spentAmount / budget.amount).toFloat()
                val warningColor = when {
                    spentRatio > 1.0f -> ColorExpense
                    spentRatio >= 0.75f -> ColorBudget
                    else -> ColorIncome
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Spent: $%,.2f".format(spentAmount),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "%.1f%%".format(spentRatio * 100),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = warningColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { spentRatio.coerceAtMost(1.0f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = warningColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecurringRow(
    recurring: RecurringTransaction,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val nextDateStr = remember(recurring.nextOccurrence) {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.format(Date(recurring.nextOccurrence))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(recurring.categoryColor).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = IconMapper.getIconByName(recurring.categoryIcon),
                    contentDescription = null,
                    tint = Color(recurring.categoryColor),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (recurring.note.isNotEmpty()) recurring.note else recurring.categoryName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Frequency Pill
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = recurring.frequency,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "Next: $nextDateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }

            // Amount with status color
            Text(
                text = "${if (recurring.type == "INCOME") "+" else "-"}$%,.2f".format(recurring.amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (recurring.type == "INCOME") ColorIncome else ColorExpense,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Split Toggle Active
            Switch(
                checked = recurring.isActive,
                onCheckedChange = { onToggleActive() },
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .scale(0.85f)
                    .testTag("toggle_recurring_switch_${recurring.id}")
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("delete_recurring_btn_${recurring.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
