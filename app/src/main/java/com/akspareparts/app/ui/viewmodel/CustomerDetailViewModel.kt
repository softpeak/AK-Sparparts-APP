package com.akspareparts.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akspareparts.app.AKApplication
import com.akspareparts.app.data.Bill
import com.akspareparts.app.data.BillDraftItem
import com.akspareparts.app.data.BillItem
import com.akspareparts.app.data.Customer
import com.akspareparts.app.data.CustomerPart
import com.akspareparts.app.data.Part
import com.akspareparts.app.pdf.BillPdfGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerDetailViewModel(
    app: Application,
    private val customerId: Int
) : AndroidViewModel(app) {

    private val application = app as AKApplication
    private val repo = application.repository

    val customer: StateFlow<Customer?> =
        repo.customer(customerId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ---- Tab 1 & 3: customer parts (with search) ----
    private val _partSearch = MutableStateFlow("")
    val partSearch: StateFlow<String> = _partSearch
    fun setPartSearch(q: String) { _partSearch.value = q }

    val customerParts: StateFlow<List<CustomerPart>> = _partSearch
        .flatMapLatest { q ->
            if (q.isBlank()) repo.customerParts(customerId)
            else repo.searchCustomerParts(customerId, q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- Tab 2: browse global catalog + manual add ----
    private val _catalogSearch = MutableStateFlow("")
    val catalogSearch: StateFlow<String> = _catalogSearch
    fun setCatalogSearch(q: String) { _catalogSearch.value = q }

    val catalog: StateFlow<List<Part>> = _catalogSearch
        .flatMapLatest { q -> if (q.isBlank()) repo.allParts() else repo.searchParts(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addPart(partNumber: String, price: Double, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val added = repo.addCustomerPart(customerId, partNumber.trim(), price)
            onResult(added)
        }
    }

    fun updatePart(part: CustomerPart, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.updateCustomerPart(part)
            onDone()
        }
    }

    fun deletePart(part: CustomerPart, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteCustomerPart(part)
            onDone()
        }
    }

    // ---- Tab 4: bill generation ----
    private val _draft = MutableStateFlow<List<BillDraftItem>>(emptyList())
    val draft: StateFlow<List<BillDraftItem>> = _draft

    fun loadDraft() {
        _draft.value = customerParts.value.map {
            BillDraftItem(partNumber = it.partNumber, unitPrice = it.price)
        }
    }

    fun toggleSelected(index: Int) {
        _draft.value = _draft.value.mapIndexed { i, item ->
            if (i == index) item.copy(selected = !item.selected) else item
        }
    }

    fun setQty(index: Int, qty: Int) {
        _draft.value = _draft.value.mapIndexed { i, item ->
            if (i == index) item.copy(qty = qty.coerceAtLeast(1)) else item
        }
    }

    private val _lastPdf = MutableStateFlow<File?>(null)
    val lastPdf: StateFlow<File?> = _lastPdf

    private val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    /** Generates the PDF preview (delivery added if > 0). Does NOT save to history. */
    fun generatePdf(delivery: Double, onReady: (File) -> Unit, onError: (String) -> Unit) {
        val cust = customer.value ?: run { onError("Customer not loaded"); return }
        val selected = _draft.value.filter { it.selected }
        if (selected.isEmpty()) { onError("Select at least one part"); return }
        viewModelScope.launch {
            try {
                val items = selected.map {
                    BillItem(billId = 0, partNumber = it.partNumber, qty = it.qty,
                        unitPrice = it.unitPrice, lineTotal = it.lineTotal)
                }
                val total = items.sumOf { it.lineTotal } + delivery
                val data = BillPdfGenerator.BillData(
                    customerName = cust.name, city = cust.city, date = dateFmt.format(Date()),
                    items = items, grandTotal = total, deliveryCharge = delivery
                )
                val file = BillPdfGenerator.generate(
                    application, data, "bill_${customerId}_${System.currentTimeMillis()}.pdf"
                )
                _lastPdf.value = file
                onReady(file)
            } catch (e: Exception) {
                onError(e.message ?: "PDF generation failed")
            }
        }
    }

    /** Persists the bill (delivery folded into the total + PDF). */
    fun saveBill(delivery: Double, onSaved: () -> Unit, onError: (String) -> Unit) {
        val cust = customer.value ?: run { onError("Customer not loaded"); return }
        val selected = _draft.value.filter { it.selected }
        if (selected.isEmpty()) { onError("Select at least one part"); return }
        viewModelScope.launch {
            try {
                val date = dateFmt.format(Date())
                val items = selected.map {
                    BillItem(billId = 0, partNumber = it.partNumber, qty = it.qty,
                        unitPrice = it.unitPrice, lineTotal = it.lineTotal)
                }
                val total = items.sumOf { it.lineTotal } + delivery
                val pdf = BillPdfGenerator.generate(
                    application,
                    BillPdfGenerator.BillData(cust.name, cust.city, date, items, total, delivery),
                    "bill_${customerId}_${System.currentTimeMillis()}.pdf"
                )
                val bill = Bill(customerId = customerId, date = date,
                    totalAmount = total, pdfPath = pdf.absolutePath)
                repo.saveBill(bill, items)
                onSaved()
            } catch (e: Exception) {
                onError(e.message ?: "Save failed")
            }
        }
    }

    // ---- Tab 5: all bills ----
    val bills: StateFlow<List<Bill>> =
        repo.bills(customerId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun billItems(billId: Int): List<BillItem> = repo.getBillItems(billId)

    class Factory(
        private val app: Application,
        private val customerId: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CustomerDetailViewModel(app, customerId) as T
    }
}
