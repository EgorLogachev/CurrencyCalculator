package com.example.currencycalculator.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.currencycalculator.data.Currency
import com.example.currencycalculator.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

// todo save date
// todo save history

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val repo: Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<State>(State.Loading)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    init {
        fetchCurrencies()
        // ToDo fetch history here
    }

    fun fetchCurrencies(date: Date = Date()) {
        viewModelScope.launch {
            _uiState.emit(State.Loading)
            _uiState.emit(repo.getCurrencies(date).fold({
                State.Success(it)
            }, {
                State.Error(it)
            }))
        }

    }
}

sealed class State {
    data class Success(val currencies: List<Currency>) : State()
    object Loading : State()
    class Error(val e: Throwable) : State()
}
