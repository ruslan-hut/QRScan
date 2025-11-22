package ua.com.programmer.barcodetest.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.com.programmer.barcodetest.data.BarcodeHistoryItem
import ua.com.programmer.barcodetest.data.repository.BarcodeRepository
import ua.com.programmer.barcodetest.error.AppError
import ua.com.programmer.barcodetest.error.ErrorMapper
import javax.inject.Inject

data class HistoryUiState(
    val historyItems: List<BarcodeHistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val isEmpty: Boolean = true,
    val error: AppError? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: BarcodeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getAllHistoryItems()
                .onSuccess { items ->
                    _uiState.value = _uiState.value.copy(
                        historyItems = items,
                        isEmpty = items.isEmpty(),
                        isLoading = false
                    )
                }
                .onFailure { throwable ->
                    val appError = ErrorMapper.map(throwable)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = appError
                    )
                }
        }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(itemId)
                .onSuccess {
                    loadHistory() // Reload after deletion
                }
                .onFailure { throwable ->
                    val appError = ErrorMapper.map(throwable)
                    _uiState.value = _uiState.value.copy(error = appError)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refresh() {
        loadHistory()
    }
}

