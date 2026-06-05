package com.akspareparts.app.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akspareparts.app.ui.components.EmptyState
import com.akspareparts.app.ui.components.SearchBar
import com.akspareparts.app.ui.viewmodel.PartsViewModel

@Composable
fun AllPartsScreen(vm: PartsViewModel = viewModel()) {
    val parts by vm.parts.collectAsState()
    val query by vm.query.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        SearchBar(value = query, onChange = vm::setQuery)
        Spacer(Modifier.height(8.dp))
        Text("${parts.size} parts in catalog",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        if (parts.isEmpty()) {
            EmptyState("No parts found.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(parts, key = { it.id }) { part ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(part.partNumber, fontWeight = FontWeight.Medium)
                            Text("AED ${fmtPrice(part.price)}",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

fun fmtPrice(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
