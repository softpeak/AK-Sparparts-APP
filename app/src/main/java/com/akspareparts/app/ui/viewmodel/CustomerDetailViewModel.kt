package com.akspareparts.app.ui.viewmodel

import android.app.Application
import android.net.Uri
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
import com.akspareparts.app.data.ExtractedPart
import com.akspareparts.app.network.ClaudeVisionApi
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
    private val session = application.session

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

    // ---- Tab 2: add part (manual) + autosuggest ----
    private val _suggestions = MutableStateFlow<List<com.akspareparts.app.data.Part>>(emptyList())
    val suggestions: StateFlow<List<com.akspareparts.app.data.Part>> = _suggestions

    fun fetchSuggestions(prefix: String) {
        viewModelScope.launch { _suggestions.value = repo.suggestParts(prefix.trim()) }
    }
    fun clearSuggestions() { _suggestions.value = emptyList() }

    fun addPart(partNumber: String, price: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.addCustomerPart(customerId, partNumber.trim(), price)
            onDone()
        }
    }

    fun updatePart(part: CustomerPart, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.updateCustomerPart(part)
            onDone()
        }
    }

    // ---- Tab 2: image extraction via Claude vision ----
    private val _extracting = MutableStateFlow(false)
    val extracting: StateFlow<Boolean> = _extracting

    private val _extracted = MutableStateFlow<List<ExtractedPart>?>(null)
    val extracted: StateFlow<List<ExtractedPart>?> = _extracted

    private val _extractError = MutableStateFlow<String?>(null)
    val extractError: StateFlow<String?> = _extractError

    val hasApiKey: Boolean get() = session.hasApiKey

    fun extractFromImage(uri: Uri) {
        val key = session.apiKey
        if (key.isNullOrBlank()) {
            _extractError.value = "No API key saved. Add it from the menu first."
            return
        }
        _extracting.value = true
        _extractError.value = null
        viewModelScope.launch {
            when (val r = ClaudeVisionApi.extractParts(application, uri, key)) {
                is ClaudeVisionApi.Result.Success -> _extracted.value = r.parts
                is ClaudeVisionApi.Result.Error -> _extractError.value = r.message
            }
            _extracting.value = false
        }
    }

    fun confirmExtracted(parts: List<ExtractedPart>, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.addCustomerParts(customerId, parts.map { it.partNumber to it.price })
            _extracted.value = null
            onDone()
        }
    }

    fun dismissExtracted() { _extracted.value = null }
    fun clearExtractError() { _extractError.value = null }

    // ---- Tab 4: bill generation ----
    private val _draft = MutableStateFlow<List<BillDraftItem>>(emptyList())
    val draft: StateFlow<List<BillDraftItem>> = _draft

    /** Build the draft from this customer's parts (call when opening the Generate Bill tab). */
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

    val grandTotal: Double
        get() = _draft.value.filter { it.selected }.sumOf { it.lineTotal }

    private val _lastPdf = MutableStateFlow<File?>(null)
    val lastPdf: StateFlow<File?> = _lastPdf

    private val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    /** Generates the PDF preview file but does NOT save to history. */
    fun generatePdf(onReady: (File) -> Unit, onError: (String) -> Unit) {
        val cust = customer.value ?: run { onError("Customer not loaded"); return }
        val selected = _draft.value.filter { it.selected }
        if (selected.isEmpty()) { onError("Select at least one part"); return }
        viewModelScope.launch {
            try {
                val items = selected.map {
                    BillItem(billId = 0, partNumber = it.partNumber, qty = it.qty,
                        unitPrice = it.unitPrice, lineTotal = it.lineTotal)
                }
                val date = dateFmt.format(Date())
                val data = BillPdfGenerator.BillData(
                    customerName = cust.name, city = cust.city, date = date,
                    items = items, grandTotal = items.sumOf { it.lineTotal }
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

    /** Explicitly persists the current draft + generated PDF to bill history. */
    fun saveBill(onSaved: () -> Unit, onError: (String) -> Unit) {
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
                val total = items.sumOf { it.lineTotal }
                val pdf = _lastPdf.value ?: BillPdfGenerator.generate(
                    application,
                    BillPdfGenerator.BillData(cust.name, cust.city, date, items, total),
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
