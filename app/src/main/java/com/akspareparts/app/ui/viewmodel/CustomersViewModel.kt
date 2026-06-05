package com.akspareparts.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akspareparts.app.AKApplication
import com.akspareparts.app.data.Customer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomersViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as AKApplication).repository

    val customers: StateFlow<List<Customer>> =
        repo.customers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCustomer(name: String, city: String, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val id = repo.addCustomer(name.trim(), city.trim())
            onDone(id)
        }
    }
}
