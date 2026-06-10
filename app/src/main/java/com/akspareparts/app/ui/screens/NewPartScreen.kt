package com.akspareparts.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akspareparts.app.ui.viewmodel.PartsViewModel
import kotlinx.coroutines.launch

@Composable
fun NewPartScreen(vm: PartsViewModel = viewModel()) {
    var partNumber by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Card(
                Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AddBox, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Add to Global Catalog",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text("New parts appear in every customer's picker",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = partNumber, onValueChange = { partNumber = it },
                        label = { Text("Part Number") }, singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Filled.Numbers, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Price (AED)") }, singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Filled.Payments, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val p = price.toDoubleOrNull()
                            if (partNumber.isBlank() || p == null) {
                                scope.launch { snackbar.showSnackbar("Enter a valid part number and price") }
                            } else {
                                vm.addPart(partNumber, p) {
                                    partNumber = ""; price = ""
                                    scope.launch { snackbar.showSnackbar("Part saved to catalog") }
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text("SAVE PART", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
