package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ColorExpense
import com.example.ui.theme.ColorIncome
import com.example.ui.theme.ColorNeutral

@Composable
fun ToolsTab(
    onExportBackup: (String, (String?) -> Unit) -> Unit, // passcode, callback
    onImportBackup: (String, String, (Boolean) -> Unit) -> Unit, // payload, passcode, callback
    onExportCsv: () -> Unit,
    onAddCustomCategory: (String, String, Int, Boolean) -> Unit, // name, icon, colorHex, isIncome
    onWipeDatabase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Passcode states
    var exportPasscode by remember { mutableStateOf("") }
    var importPasscode by remember { mutableStateOf("") }
    var importPayload by remember { mutableStateOf("") }

    // Dialog state for export result display
    var exportedKeyDialogText by remember { mutableStateOf<String?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showWipeConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Card 1: Data Portability (CSV Share / Export)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Data Portability",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Export your entire local finance ledger to high-compatibility CSV format. Open directly in MS Excel, Sheets, or other spreadsheets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onExportCsv,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_csv_btn")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share CSV Statements")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: Encrypted Backup & Restore Lockbox
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Encrypted Lockbox Backup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Generate password-authenticated AES-256 encryptions of your finance ledger to prevent leakages and copy details securely.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                // Sub section 1: Export Local Crypt details
                Text(
                    text = "Create Secure Encrypted Export",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                OutlinedTextField(
                    value = exportPasscode,
                    onValueChange = { exportPasscode = it },
                    label = { Text("Backup Passcode") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_backup_passcode")
                )

                Button(
                    onClick = {
                        if (exportPasscode.trim().isNotEmpty()) {
                            onExportBackup(exportPasscode) { key ->
                                if (key != null) {
                                    exportedKeyDialogText = key
                                }
                            }
                        }
                    },
                    enabled = exportPasscode.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_backup_btn")
                ) {
                    Text("Generate Encrypted Export Payload")
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Sub section 2: Decrypt & Import payload
                Text(
                    text = "Decrypt & Import Restore",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = importPayload,
                    onValueChange = { importPayload = it },
                    label = { Text("Encrypted Backup Raw Payload") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("import_backup_payload")
                )

                OutlinedTextField(
                    value = importPasscode,
                    onValueChange = { importPasscode = it },
                    label = { Text("Decryption Passcode") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_backup_passcode")
                )

                Button(
                    onClick = {
                        if (importPayload.trim().isNotEmpty() && importPasscode.trim().isNotEmpty()) {
                            onImportBackup(importPayload, importPasscode) { success ->
                                if (success) {
                                    importPayload = ""
                                    importPasscode = ""
                                }
                            }
                        }
                    },
                    enabled = importPayload.trim().isNotEmpty() && importPasscode.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restore_backup_btn")
                ) {
                    Text("Verify Passcode & Decrypt Restore")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 3: Custom Categories Manager
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Custom Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Customize your budget filters! Add unique expense categories or sources of personal income tailored for your lifestyle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { showCategoryDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_custom_category_btn")
                ) {
                    Text("+ Create Custom Category")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 4: Danger Zone
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 120.dp) // padding to avoid floating bottom bars
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Danger Zone",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Purge all records. This clears database transactions, budget profiles, and templates securely so you can start fresh. Action is irreversible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { showWipeConfirmation = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wipe_db_trigger_btn")
                ) {
                    Text("Secure Wipe Ledger Database", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Modal dialogue to display the encrypted Base64 copyable payload
    if (exportedKeyDialogText != null) {
        val payloadToCopy = exportedKeyDialogText!!
        AlertDialog(
            onDismissRequest = { exportedKeyDialogText = null },
            title = { Text("AES-256 Encrypted Export Key") },
            text = {
                Column {
                    Text(
                        text = "Your ledger has been encrypted! Double tap below and share or save this backup code string securely.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = payloadToCopy,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Secure Backup String (Token)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("backup_payload_viewer")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Encrypted Finance Backup", payloadToCopy)
                        clipboard.setPrimaryClip(clip)
                        exportedKeyDialogText = null
                    },
                    modifier = Modifier.testTag("copy_backup_payload_btn")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportedKeyDialogText = null }) {
                    Text("Done")
                }
            }
        )
    }

    // Custom Category Add Dialog
    if (showCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        var isIncome by remember { mutableStateOf(false) }
        var selectedIconIndex by remember { mutableStateOf(0) }
        var selectedColorIndex by remember { mutableStateOf(0) }

        val iconOptions = listOf(
            "Star", "Fastfood", "ShoppingCart", "Receipt", "DirectionsCar",
            "House", "MedicalServices", "LocalPlay", "Work", "TrendingUp",
            "AttachMoney", "Build", "School", "Flight", "FitnessCenter"
        )
        val colorOptions = listOf(
            0xFF4F46E5.toInt(), // Indigo
            0xFF10B981.toInt(), // Emerald green
            0xFFF43F5E.toInt(), // Vivid Coral Rose
            0xFFF59E0B.toInt(), // Amber
            0xFF06B6D4.toInt(), // Cyan Blue
            0xFF8E24AA.toInt(), // Orchid Purple
            0xFFE53935.toInt(), // Red
            0xFF008080.toInt(), // Teal
            0xFF757575.toInt(), // Slate Gray
            0xFFD946EF.toInt()  // Fuchsia Pink
        )

        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Create Custom Category") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_cat_name_input")
                    )

                    // Type Toggle (Expense vs Income Category)
                    Text("Type Representation", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(false to "Expense Category", true to "Income Category").forEach { (incBool, lbl) ->
                            val isSel = isIncome == incBool
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { isIncome = incBool }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lbl,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Icon selection list grid row
                    Text("Select Stylized Icon", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Display 5 icons as quick horizontal strip options
                        val previewIcons = iconOptions.take(5)
                        previewIcons.forEachIndexed { idx, name ->
                            val isSel = selectedIconIndex == idx
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedIconIndex = idx },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = com.example.ui.IconMapper.getIconByName(name),
                                    contentDescription = name,
                                    tint = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Color palette strip
                    Text("Select Visual Stamp Color", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val previewColors = colorOptions.take(5)
                        previewColors.forEachIndexed { idx, colorInt ->
                            val isSel = selectedColorIndex == idx
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorInt))
                                    .clickable { selectedColorIndex = idx }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
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
                        if (catName.trim().isNotEmpty()) {
                            onAddCustomCategory(
                                catName,
                                iconOptions[selectedIconIndex],
                                colorOptions[selectedColorIndex],
                                isIncome
                            )
                            showCategoryDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_custom_cat_btn")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Safety wipes dialog confirm
    if (showWipeConfirmation) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmation = false },
            title = { Text("Danger: Wipe Finance Tracker?") },
            text = {
                Text("This permanently deletes all expense/income ledgers, custom templates, and budgets. This action cannot be undone. Are you sure you want to proceed?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onWipeDatabase()
                        showWipeConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("wipe_db_confirm_btn")
                ) {
                    Text("Yes, Wipe All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
