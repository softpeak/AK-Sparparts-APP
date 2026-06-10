package com.akspareparts.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.akspareparts.app.data.CustomerPart
import com.akspareparts.app.ui.components.EmptyState
import com.akspareparts.app.ui.components.PartNumberText
import com.akspareparts.app.ui.components.PriceChip
import com.akspareparts.app.ui.components.SectionHeader
import com.akspareparts.app.ui.viewmodel.CustomerDetailViewModel
import kotlinx.coroutines.launch
import java.io.File

private val TABS = listOf("Sold Parts", "Add Parts", "Edit Parts", "Generate Bill", "All Bills")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    vm: CustomerDetailViewModel,
    onBack: () -> Unit
) {
    val customer by vm.customer.collectAsState()
    var tab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(customer?.name ?: "Customer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        customer?.let {
                            Text(it.city, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            ScrollableTabRow(
                selectedTabIndex = tab,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                TABS.forEachIndexed { i, title ->
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = {
                            Text(title,
                                fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Medium)
                        }
                    )
                }
            }
            when (tab) {
                0 -> SoldPartsTab(vm)
                1 -> AddPartsTab(vm)
                2 -> EditPartsTab(vm)
                3 -> GenerateBillTab(vm)
                4 -> AllBillsTab(vm)
            }
        }
    }
}

/* ---------- Tab 1: Already Sold Parts ---------- */
@Composable
private fun SoldPartsTab(vm: CustomerDetailViewModel) {
    val parts by vm.customerParts.collectAsState()
    if (parts.isEmpty()) {
        EmptyState(
            title = "No parts yet",
            message = "No parts sold to this customer yet. Use the Add Parts tab.",
            icon = Icons.Filled.Inventory2
        )
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SectionHeader("${parts.size} parts sold")
        }
        items(parts, key = { it.id }) { p ->
            Card(
                Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PartNumberText(p.partNumber, modifier = Modifier.weight(1f))
                    PriceChip(fmtPrice(p.price))
                }
            }
        }
    }
}

/* ---------- Tab 2: Add New Parts (manual + catalog picker) ---------- */
@Composable
private fun AddPartsTab(vm: CustomerDetailViewModel) {
    var partNumber by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    val catalog by vm.catalog.collectAsState()
    val catalogSearch by vm.catalogSearch.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun toast(msg: String) { scope.launch { snackbar.showSnackbar(msg) } }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {

            SectionHeader("Add a new part")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = partNumber, onValueChange = { partNumber = it },
                    label = { Text("Part Number") }, singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.4f)
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Price") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    val p = price.toDoubleOrNull()
                    if (partNumber.isBlank() || p == null) {
                        toast("Enter part number and price")
                    } else {
                        vm.addPart(partNumber, p) { added ->
                            if (added) {
                                partNumber = ""; price = ""
                                toast("Part added")
                            } else {
                                toast("This part is already in this customer's list")
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("SAVE NEW PART", fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader("Or pick from the list (${catalog.size})")
                TextButton(
                    onClick = {
                        if (catalog.isEmpty()) {
                            toast("Nothing to add")
                        } else {
                            vm.addAll(catalog) { added, skipped ->
                                toast("Added $added new" + if (skipped > 0) ", skipped $skipped already added" else "")
                            }
                        }
                    }
                ) {
                    Icon(Icons.Filled.DoneAll, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add all")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = catalogSearch, onValueChange = vm::setCatalogSearch,
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                placeholder = { Text("Search part number") }, singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            if (catalog.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matching parts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(catalog, key = { it.id }) { part ->
                        Card(
                            Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 6.dp, end = 12.dp,
                                    top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledIconButton(
                                    onClick = {
                                        vm.addPart(part.partNumber, part.price) { added ->
                                            toast(
                                                if (added) "Added ${part.partNumber}"
                                                else "${part.partNumber} is already in this customer's list"
                                            )
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add to customer")
                                }
                                Spacer(Modifier.width(8.dp))
                                PartNumberText(part.partNumber, modifier = Modifier.weight(1f))
                                PriceChip(fmtPrice(part.price), emphasized = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ---------- Tab 3: Edit Parts ---------- */
@Composable
private fun EditPartsTab(vm: CustomerDetailViewModel) {
    val parts by vm.customerParts.collectAsState()
    val search by vm.partSearch.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            OutlinedTextField(
                value = search, onValueChange = vm::setPartSearch,
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                placeholder = { Text("Search part number") }, singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            if (parts.isEmpty()) {
                EmptyState(
                    title = "Nothing to edit",
                    message = "No parts match your search.",
                    icon = Icons.Filled.EditNote
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(parts, key = { it.id }) { part ->
                        EditableRow(
                            part = part,
                            onSave = { updated ->
                                vm.updatePart(updated) {
                                    scope.launch { snackbar.showSnackbar("Saved ${updated.partNumber}") }
                                }
                            },
                            onDelete = {
                                vm.deletePart(part) {
                                    scope.launch { snackbar.showSnackbar("Deleted ${part.partNumber}") }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableRow(
    part: CustomerPart,
    onSave: (CustomerPart) -> Unit,
    onDelete: () -> Unit
) {
    var pn by remember(part.id) { mutableStateOf(part.partNumber) }
    var pr by remember(part.id) { mutableStateOf(fmtPrice(part.price)) }
    var confirmDelete by remember { mutableStateOf(false) }

    Card(
        Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(value = pn, onValueChange = { pn = it },
                label = { Text("Part") }, singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.4f))
            OutlinedTextField(value = pr,
                onValueChange = { pr = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Price") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f))
            IconButton(onClick = {
                val price = pr.toDoubleOrNull() ?: part.price
                onSave(part.copy(partNumber = pn.trim(), price = price))
            }) {
                Icon(Icons.Filled.Save, contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(Icons.Filled.Delete, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Delete part?") },
            text = { Text("Remove ${part.partNumber} from this customer?") },
            confirmButton = {
                Button(
                    onClick = { confirmDelete = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

/* ---------- Tab 4: Generate Bill ---------- */
@Composable
private fun GenerateBillTab(vm: CustomerDetailViewModel) {
    val context = LocalContext.current
    val customerParts by vm.customerParts.collectAsState()
    val draft by vm.draft.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showPreview by remember { mutableStateOf(false) }
    var deliveryText by remember { mutableStateOf("") }
    var billSearch by remember { mutableStateOf("") }

    LaunchedEffect(customerParts) { vm.loadDraft() }

    val delivery = deliveryText.toDoubleOrNull() ?: 0.0
    val selectedCount = draft.count { it.selected }
    val partsTotal = draft.filter { it.selected }.sumOf { it.lineTotal }
    val grand = partsTotal + delivery

    // Filtered view of the draft; each entry keeps its ORIGINAL index so
    // selection/qty updates always hit the right item.
    val visible = draft.withIndex().filter {
        billSearch.isBlank() || it.value.partNumber.contains(billSearch, ignoreCase = true)
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (draft.isEmpty()) {
                EmptyState(
                    title = "No parts available",
                    message = "Add parts to this customer first.",
                    icon = Icons.Filled.ReceiptLong
                )
                return@Column
            }

            // Search box for the bill part list
            Column(Modifier.padding(horizontal = 12.dp)) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = billSearch,
                    onValueChange = { billSearch = it },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (billSearch.isNotEmpty()) {
                            IconButton(onClick = { billSearch = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    placeholder = { Text("Search part number") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (selectedCount > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$selectedCount part${if (selectedCount > 1) "s" else ""} selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (visible.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No part matches \"$billSearch\".",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(visible, key = { it.value.partNumber }) { entry ->
                        val i = entry.index
                        val item = entry.value
                        Card(
                            Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.selected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = item.selected,
                                    onCheckedChange = { vm.toggleSelected(i) })
                                Column(Modifier.weight(1f)) {
                                    PartNumberText(item.partNumber)
                                    Text("AED ${fmtPrice(item.unitPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (item.selected) {
                                    OutlinedTextField(
                                        value = item.qty.toString(),
                                        onValueChange = {
                                            vm.setQty(i, it.filter { c -> c.isDigit() }.toIntOrNull() ?: 1)
                                        },
                                        label = { Text("Qty") }, singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("AED ${fmtPrice(item.lineTotal)}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    OutlinedTextField(
                        value = deliveryText,
                        onValueChange = { deliveryText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Delivery charges (AED) - optional") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    if (delivery > 0.0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("AED ${fmtPrice(partsTotal)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Delivery", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("AED ${fmtPrice(delivery)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Grand Total", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        Text("AED ${fmtPrice(grand)}", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { showPreview = true },
                        enabled = selectedCount > 0,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(Icons.Filled.Receipt, contentDescription = null)
                        Spacer(Modifier.width(8.dp)); Text("PREVIEW BILL", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showPreview) {
        BillPreviewDialog(
            vm = vm, partsTotal = partsTotal, delivery = delivery, grand = grand,
            onDismiss = { showPreview = false },
            onShared = { file -> sharePdf(context, file) },
            onMessage = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
        )
    }
}

@Composable
private fun BillPreviewDialog(
    vm: CustomerDetailViewModel,
    partsTotal: Double,
    delivery: Double,
    grand: Double,
    onDismiss: () -> Unit,
    onShared: (File) -> Unit,
    onMessage: (String) -> Unit
) {
    val customer by vm.customer.collectAsState()
    val draft by vm.draft.collectAsState()
    val selected = draft.filter { it.selected }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(Icons.Filled.ReceiptLong, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text("AK Spareparts - Bill Preview", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(customer?.name ?: "", fontWeight = FontWeight.Bold)
                Text(customer?.city ?: "", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Part", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f))
                    Text("Qty", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                    Text("Total", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
                }
                HorizontalDivider()
                LazyColumn(Modifier.heightIn(max = 200.dp)) {
                    items(selected) { it2 ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(it2.partNumber, modifier = Modifier.weight(1.4f))
                            Text(it2.qty.toString(), modifier = Modifier.weight(0.5f))
                            Text(fmtPrice(it2.lineTotal), modifier = Modifier.weight(0.8f))
                        }
                    }
                }
                HorizontalDivider()
                if (delivery > 0.0) {
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal"); Text("AED ${fmtPrice(partsTotal)}")
                    }
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery"); Text("AED ${fmtPrice(delivery)}")
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Grand Total", fontWeight = FontWeight.Bold)
                    Text("AED ${fmtPrice(grand)}", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    vm.generatePdf(delivery,
                        onReady = { file -> onShared(file) },
                        onError = { onMessage(it) })
                }) { Text("Share PDF") }
                Button(
                    onClick = {
                        vm.saveBill(delivery,
                            onSaved = { onMessage("Bill saved to history"); onDismiss() },
                            onError = { onMessage(it) })
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save Bill") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

/* ---------- Tab 5: All Bills ---------- */
@Composable
private fun AllBillsTab(vm: CustomerDetailViewModel) {
    val context = LocalContext.current
    val bills by vm.bills.collectAsState()
    if (bills.isEmpty()) {
        EmptyState(
            title = "No saved bills",
            message = "Bills you save from the Generate Bill tab will appear here.",
            icon = Icons.Filled.ReceiptLong
        )
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(bills, key = { it.id }) { bill ->
            Card(
                Modifier.fillMaxWidth().clickable {
                    bill.pdfPath?.let { path ->
                        val f = File(path)
                        if (f.exists()) sharePdf(context, f)
                    }
                },
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.ReceiptLong, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Bill #${bill.id}", fontWeight = FontWeight.Bold)
                        Text(bill.date, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    PriceChip(fmtPrice(bill.totalAmount))
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.Share, contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/* ---------- helpers ---------- */
private fun sharePdf(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share bill via"))
}
