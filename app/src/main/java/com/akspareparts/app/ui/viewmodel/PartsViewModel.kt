package com.akspareparts.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akspareparts.app.AKApplication
import com.akspareparts.app.data.Part
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PartsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as AKApplication).repository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val parts: StateFlow<List<Part>> = _query
        .flatMapLatest { q -> if (q.isBlank()) repo.allParts() else repo.searchParts(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }

    fun addPart(partNumber: String, price: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.addGlobalPart(partNumber.trim(), price)
            onDone()
        }
    }
}
