package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.IconMapper
import com.example.ui.theme.ColorNeutral
import java.util.*

@Composable
fun AnalyticsTab(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"

    // Filter relevant transactions
    val filteredTxs = transactions.filter { it.type == selectedType }
    val totalAmount = filteredTxs.sumOf { it.amount }

    // Map: Category Name -> Total amount in that category
    val categoryTotals = remember(filteredTxs) {
        filteredTxs.groupBy { it.categoryName }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    // Capture category meta (color & icon)
    val categoryMeta = remember(filteredTxs) {
        filteredTxs.groupBy { it.categoryName }.mapValues { entry ->
            val first = entry.value.first()
            Pair(Color(first.categoryColor), first.categoryIcon)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Tab toggles "Expense" vs "Income"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("EXPENSE" to "Expenses", "INCOME" to "Income").forEach { (typeKey, label) ->
                    val isSelected = selectedType == typeKey
                    Button(
                        onClick = { selectedType = typeKey },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("analytics_toggle_$typeKey")
                    ) {
                        Text(text = label, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (totalAmount <= 0) {
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
                            imageVector = Icons.Default.PieChart,
                            contentDescription = "Empty",
                            modifier = Modifier
                                .size(72.dp)
                                .padding(bottom = 8.dp),
                            tint = ColorNeutral.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "No records for analytics.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorNeutral
                        )
                        Text(
                            text = "Add transactions in the Dashboard to generate category charts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorNeutral,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                if (isWideScreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Donut
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            DonutWithStats(categoryTotals, categoryMeta, totalAmount)
                        }

                        // Right Legend List
                        Box(
                            modifier = Modifier
                                .weight(1.5f)
                                .fillMaxHeight()
                        ) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.testTag("analytics_list_wide")
                            ) {
                                items(categoryTotals) { (catName, sumValue) ->
                                    val percent = ((sumValue / totalAmount) * 100).toFloat()
                                    val meta = categoryMeta[catName]
                                    val catColor = meta?.first ?: Color.Gray
                                    val catIcon = meta?.second ?: "Category"

                                    LegendItem(
                                        name = catName,
                                        amount = sumValue,
                                        percentage = percent,
                                        color = catColor,
                                        iconName = catIcon
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Mobile layout: Stacked
                    DonutWithStats(categoryTotals, categoryMeta, totalAmount, modifier = Modifier.height(240.dp))

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("analytics_list_mobile"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(categoryTotals) { (catName, sumValue) ->
                            val percent = ((sumValue / totalAmount) * 100).toFloat()
                            val meta = categoryMeta[catName]
                            val catColor = meta?.first ?: Color.Gray
                            val catIcon = meta?.second ?: "Category"

                            LegendItem(
                                name = catName,
                                amount = sumValue,
                                percentage = percent,
                                color = catColor,
                                iconName = catIcon
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DonutWithStats(
    categoryTotals: List<Pair<String, Double>>,
    categoryMeta: Map<String, Pair<Color, String>>,
    totalAmount: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(190.dp)
                .testTag("analytics_donut_canvas")
        ) {
            var startAngle = -90f
            for ((catName, sumValue) in categoryTotals) {
                val sweepAngle = ((sumValue / totalAmount) * 360f).toFloat()
                val meta = categoryMeta[catName]
                val color = meta?.first ?: Color.Gray

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 30f, cap = StrokeCap.Round)
                )
                startAngle += sweepAngle
            }
        }

        // Center visual reading labels
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Total Value",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$%,.2f".format(totalAmount),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LegendItem(
    name: String,
    amount: Double,
    percentage: Float,
    color: Color,
    iconName: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = IconMapper.getIconByName(iconName),
                    contentDescription = name,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$%,.2f".format(amount),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Percentage indicator slider progress row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { percentage / 100f },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "%.1f%%".format(percentage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
