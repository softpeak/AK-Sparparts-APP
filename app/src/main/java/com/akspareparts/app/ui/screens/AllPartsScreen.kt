package com.akspareparts.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akspareparts.app.ui.components.EmptyState
import com.akspareparts.app.ui.components.PartNumberText
import com.akspareparts.app.ui.components.PriceChip
import com.akspareparts.app.ui.components.SearchBar
import com.akspareparts.app.ui.components.StatCard
import com.akspareparts.app.ui.viewmodel.PartsViewModel

@Composable
fun AllPartsScreen(vm: PartsViewModel = viewModel()) {
    val parts by vm.parts.collectAsState()
    val query by vm.query.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        StatCard(
            label = "Parts in Catalog",
            value = parts.size.toString(),
            icon = Icons.Filled.Inventory2,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        SearchBar(value = query, onChange = vm::setQuery)
        Spacer(Modifier.height(12.dp))
        if (parts.isEmpty()) {
            EmptyState(
                title = "No parts found",
                message = if (query.isBlank()) "The catalog is empty."
                    else "No part number matches \"$query\".",
                icon = Icons.Filled.Inventory2
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(parts, key = { it.id }) { part ->
                    Card(
                        Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PartNumberText(part.partNumber, modifier = Modifier.weight(1f))
                            PriceChip(fmtPrice(part.price))
                        }
                    }
                }
            }
        }
    }
}

fun fmtPrice(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
