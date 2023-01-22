package com.example.currencycalculator.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.currencycalculator.data.Conversion
import com.example.currencycalculator.data.Currency
import com.example.currencycalculator.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class State(
    val currencies: List<Currency> = emptyList(),
    val history: List<Conversion> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val repo: Repository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _currencies = MutableStateFlow<List<Currency>>(emptyList())
    private val _history = MutableStateFlow<List<Conversion>>(emptyList())

    val uiState = combine(_currencies, _history, _isLoading) { currencies, history, loading ->
        State(currencies, history, loading)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        State(isLoading = true),
    )

    init {
        fetchCurrencies()
        fetchHistory()
    }

    fun fetchCurrencies(date: Date = Date()) {
        viewModelScope.launch {
            _isLoading.emit(false)
            val result = repo.getCurrencies(date)
            _currencies.emit(result.getOrThrow())
        }
    }

    private fun fetchHistory() = _updateHistory()

    private fun _updateHistory(conversion: Conversion? = null) {
        viewModelScope.launch {
            val result = repo.updateHistory(conversion)
            _history.emit(result.getOrThrow().toList())
        }
    }

    fun updateHistory(conversion: Conversion) = _updateHistory(conversion)
}

