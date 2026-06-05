package com.akspareparts.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.akspareparts.app.data.BillItem
import com.akspareparts.app.data.CustomerPart
import com.akspareparts.app.data.ExtractedPart
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
                            style = MaterialTheme.typography.titleMedium)
                        customer?.let {
                            Text(it.city, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp) {
                TABS.forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
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
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No parts sold to this customer yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Part Number", fontWeight = FontWeight.Bold)
                Text("Price (AED)", fontWeight = FontWeight.Bold)
            }
        }
        items(parts, key = { it.id }) { p ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(p.partNumber)
                    Text(fmtPrice(p.price), color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/* ---------- Tab 2: Add New Parts (+ image extraction) ---------- */
@Composable
private fun AddPartsTab(vm: CustomerDetailViewModel) {
    val context = LocalContext.current
    var partNumber by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    val suggestions by vm.suggestions.collectAsState()
    val extracting by vm.extracting.collectAsState()
    val extracted by vm.extracted.collectAsState()
    val extractError by vm.extractError.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // capture uri holder for camera
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.extractFromImage(it) } }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraUri?.let { vm.extractFromImage(it) } }

    LaunchedEffect(extractError) {
        extractError?.let { scope.launch { snackbar.showSnackbar(it) }; vm.clearExtractError() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            OutlinedTextField(
                value = partNumber,
                onValueChange = { partNumber = it; vm.fetchSuggestions(it) },
                label = { Text("Part Number") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (suggestions.isNotEmpty() && partNumber.isNotBlank()) {
                Card(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Column {
                        suggestions.forEach { s ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    partNumber = s.partNumber
                                    price = fmtPrice(s.price)
                                    vm.clearSuggestions()
                                }.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(s.partNumber)
                                Text("AED ${fmtPrice(s.price)}",
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = price,
                onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Price (AED)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val p = price.toDoubleOrNull()
                    if (partNumber.isBlank() || p == null) {
                        scope.launch { snackbar.showSnackbar("Enter part number and price") }
                    } else {
                        vm.addPart(partNumber, p) {
                            partNumber = ""; price = ""; vm.clearSuggestions()
                            scope.launch { snackbar.showSnackbar("Part saved") }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("SAVE", fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("Extract from photo", fontWeight = FontWeight.Bold)
            Text("Upload a parts list or invoice photo and Claude will read the part numbers and prices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    enabled = !extracting, modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp)); Text("Gallery")
                }
                OutlinedButton(
                    onClick = {
                        val uri = createImageUri(context)
                        cameraUri = uri
                        cameraLauncher.launch(uri)
                    },
                    enabled = !extracting, modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp)); Text("Camera")
                }
            }
            if (extracting) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Reading image with Claude…")
                }
            }
        }
    }

    extracted?.let { parts ->
        ExtractedReviewDialog(
            parts = parts,
            onConfirm = { confirmed ->
                vm.confirmExtracted(confirmed) {
                    scope.launch { snackbar.showSnackbar("${confirmed.size} parts saved") }
                }
            },
            onDismiss = { vm.dismissExtracted() }
        )
    }
}

@Composable
private fun ExtractedReviewDialog(
    parts: List<ExtractedPart>,
    onConfirm: (List<ExtractedPart>) -> Unit,
    onDismiss: () -> Unit
) {
    val editable = remember {
        mutableStateListOf(*parts.map { it.partNumber to it.price.toString() }.toTypedArray())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review extracted parts (${parts.size})") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(editable) { i, pair ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pair.first,
                            onValueChange = { editable[i] = it to pair.second },
                            label = { Text("Part") }, singleLine = true,
                            modifier = Modifier.weight(1.4f)
                        )
                        OutlinedTextField(
                            value = pair.second,
                            onValueChange = { editable[i] = pair.first to it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Price") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val result = editable.mapNotNull { (pn, pr) ->
                    val price = pr.toDoubleOrNull()
                    if (pn.isNotBlank() && price != null) ExtractedPart(pn.trim(), price) else null
                }
                onConfirm(result)
            }) { Text("Save All") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search part number") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            if (parts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No parts to edit.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(parts, key = { it.id }) { part ->
                        EditableRow(part) { updated ->
                            vm.updatePart(updated) {
                                scope.launch { snackbar.showSnackbar("Saved ${updated.partNumber}") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableRow(part: CustomerPart, onSave: (CustomerPart) -> Unit) {
    var pn by remember(part.id) { mutableStateOf(part.partNumber) }
    var pr by remember(part.id) { mutableStateOf(fmtPrice(part.price)) }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = pn, onValueChange = { pn = it },
                label = { Text("Part") }, singleLine = true, modifier = Modifier.weight(1.4f))
            OutlinedTextField(value = pr,
                onValueChange = { pr = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Price") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f))
            IconButton(onClick = {
                val price = pr.toDoubleOrNull() ?: part.price
                onSave(part.copy(partNumber = pn.trim(), price = price))
            }) {
                Icon(Icons.Filled.Save, contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
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

    // (Re)build the draft whenever the underlying parts list changes.
    LaunchedEffect(customerParts) { vm.loadDraft() }

    val grand = draft.filter { it.selected }.sumOf { it.lineTotal }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (draft.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Add parts to this customer first.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Column
            }
            LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(draft) { i, item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = item.selected,
                                onCheckedChange = { vm.toggleSelected(i) })
                            Column(Modifier.weight(1f)) {
                                Text(item.partNumber, fontWeight = FontWeight.Medium)
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
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Grand Total", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        Text("AED ${fmtPrice(grand)}", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { showPreview = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(Icons.Filled.Receipt, contentDescription = null)
                        Spacer(Modifier.width(8.dp)); Text("PREVIEW BILL", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showPreview) {
        BillPreviewDialog(
            vm = vm, grand = grand,
            onDismiss = { showPreview = false },
            onShared = { file -> sharePdf(context, file) },
            onMessage = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
        )
    }
}

@Composable
private fun BillPreviewDialog(
    vm: CustomerDetailViewModel,
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
        title = { Text("AK Spareparts — Bill Preview") },
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
                LazyColumn(Modifier.heightIn(max = 220.dp)) {
                    items(selected) { it2 ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(it2.partNumber, modifier = Modifier.weight(1.4f))
                            Text(it2.qty.toString(), modifier = Modifier.weight(0.5f))
                            Text(fmtPrice(it2.lineTotal), modifier = Modifier.weight(0.8f))
                        }
                    }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(top = 8.dp),
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
                    vm.generatePdf(
                        onReady = { file -> onShared(file) },
                        onError = { onMessage(it) }
                    )
                }) { Text("Share PDF") }
                TextButton(onClick = {
                    vm.saveBill(
                        onSaved = { onMessage("Bill saved to history"); onDismiss() },
                        onError = { onMessage(it) }
                    )
                }) { Text("Save Bill") }
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
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No saved bills yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(bills, key = { it.id }) { bill ->
            Card(Modifier.fillMaxWidth().clickable {
                bill.pdfPath?.let { path ->
                    val f = File(path)
                    if (f.exists()) sharePdf(context, f)
                }
            }) {
                Row(Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Bill #${bill.id}", fontWeight = FontWeight.Bold)
                        Text(bill.date, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("AED ${fmtPrice(bill.totalAmount)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.Share, contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

/* ---------- helpers ---------- */
private fun createImageUri(context: android.content.Context): Uri {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun sharePdf(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share bill via"))
}
