package ua.com.programmer.barcodetest.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.com.programmer.barcodetest.data.BarcodeHistoryItem
import ua.com.programmer.barcodetest.data.BarcodeRepository

data class HistoryUiState(
    val historyItems: List<BarcodeHistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val isEmpty: Boolean = true,
    val error: String? = null
)

class HistoryViewModel(private val context: Context) : ViewModel() {

    private val repository = BarcodeRepository(context)

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val items = repository.getAllHistoryItems()
                _uiState.value = _uiState.value.copy(
                    historyItems = items,
                    isEmpty = items.isEmpty(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            try {
                val success = repository.deleteHistoryItem(itemId)
                if (success) {
                    loadHistory() // Reload after deletion
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun refresh() {
        loadHistory()
    }
}

