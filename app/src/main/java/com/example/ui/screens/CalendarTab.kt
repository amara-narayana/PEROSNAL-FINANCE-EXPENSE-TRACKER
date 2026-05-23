package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.theme.ColorExpense
import com.example.ui.theme.ColorIncome
import com.example.ui.theme.ColorNeutral
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarTab(
    transactions: List<Transaction>,
    selectedMonth: String, // format "YYYY-MM"
    onDeleteTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val calendar = remember(selectedMonth) {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = sdf.parse(selectedMonth) ?: Date()
            cal.time = date
        } catch (e: Exception) {
            // fallback current
        }
        cal
    }

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) // 0-indexed

    // Generate days grid
    val daysList = remember(year, month) {
        getDaysInMonth(year, month)
    }

    // Currently clicked day in the calendar (default to today if in this month, or 1st day of month)
    var selectedDayCal by remember(year, month) {
        val today = Calendar.getInstance()
        val defaultCal = Calendar.getInstance()
        if (today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) == month) {
            defaultCal.time = today.time
        } else {
            defaultCal.set(year, month, 1)
        }
        defaultCal.set(Calendar.HOUR_OF_DAY, 0)
        defaultCal.set(Calendar.MINUTE, 0)
        defaultCal.set(Calendar.SECOND, 0)
        defaultCal.set(Calendar.MILLISECOND, 0)
        mutableStateOf(defaultCal.time)
    }

    // Filter transactions on selectedDay
    val dayTransactions = remember(transactions, selectedDayCal) {
        transactions.filter { tx ->
            isSameDay(tx.timestamp, selectedDayCal)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Weekday labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            weekdays.forEach { dayName ->
                Text(
                    text = dayName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Grid System
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calendar_grid_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                val chunkedDays = daysList.chunked(7)
                chunkedDays.forEach { week ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        week.forEach { dateItem ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dateItem != null) {
                                    val isCurrentSelected = isSameDay(dateItem.time, selectedDayCal)

                                    // Check if this date has income and/or expense
                                    val dateTxs = transactions.filter { isSameDay(it.timestamp, dateItem) }
                                    val hasIncome = dateTxs.any { it.type == "INCOME" }
                                    val hasExpense = dateTxs.any { it.type == "EXPENSE" }

                                    val calItem = Calendar.getInstance().apply { time = dateItem }
                                    val dayNum = calItem.get(Calendar.DAY_OF_MONTH)

                                    val cellBg = if (isCurrentSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    }
                                    val cellTextColor = if (isCurrentSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .fillMaxSize(0.9f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(cellBg)
                                            .clickable { selectedDayCal = dateItem }
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isCurrentSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = cellTextColor
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // Tiny marker bullet dots
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (hasIncome) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .background(ColorIncome, CircleShape)
                                                )
                                            }
                                            if (hasExpense) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .background(ColorExpense, CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ledger footer
        val formattedSelectedDay = remember(selectedDayCal) {
            val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            sdf.format(selectedDayCal)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ledger of $formattedSelectedDay",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (dayTransactions.isNotEmpty()) {
                val dayNet = dayTransactions.sumOf { if (it.type == "INCOME") it.amount else -it.amount }
                Text(
                    text = "Net: %s$%,.2f".format(if (dayNet >= 0) "+" else "", dayNet),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (dayNet >= 0) ColorIncome else ColorExpense
                )
            }
        }

        if (dayTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = ColorNeutral.copy(alpha = 0.3f),
                        modifier = Modifier.size(54.dp).padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Clean ledger today.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorNeutral
                    )
                    Text(
                        text = "No income or expense entries.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorNeutral,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .testTag("calendar_day_transactions"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(dayTransactions, key = { it.id }) { tx ->
                    TransactionRow(
                        transaction = tx,
                        onDelete = { onDeleteTransaction(tx) }
                    )
                }
            }
        }
    }
}
}

private fun getDaysInMonth(year: Int, month: Int): List<Date?> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month)
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, etc

    val list = mutableListOf<Date?>()

    // Pad days before the first day of the week
    for (i in 1 until firstDayOfWeek) {
        list.add(null)
    }

    // Fill days
    for (day in 1..maxDays) {
        val dayCal = Calendar.getInstance()
        dayCal.set(year, month, day, 0, 0, 0)
        dayCal.set(Calendar.MILLISECOND, 0)
        list.add(dayCal.time)
    }

    return list
}

private fun isSameDay(t1: Long, date: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val cal2 = Calendar.getInstance().apply { time = date }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
