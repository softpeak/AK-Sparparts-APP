package com.akspareparts.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
            Text("Add to Global Catalog", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = partNumber, onValueChange = { partNumber = it },
                label = { Text("Part Number") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = price, onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Price (AED)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("SAVE PART", fontWeight = FontWeight.Bold) }
        }
    }
}
