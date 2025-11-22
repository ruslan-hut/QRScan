package ua.com.programmer.barcodetest.viewmodel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.com.programmer.barcodetest.Utils
import ua.com.programmer.barcodetest.data.repository.BarcodeRepository
import ua.com.programmer.barcodetest.data.repository.RepositoryProvider

data class CameraUiState(
    val barcodeValue: String = "",
    val barcodeFormat: String = "",
    val barcodeFormatInt: Int = 0,
    val isBarcodeScanned: Boolean = false,
    val showButtons: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CameraViewModel(private val context: Context) : ViewModel() {

    private val repository: BarcodeRepository = RepositoryProvider.provideBarcodeRepository(context)
    private val utils = Utils()
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "ua.com.programmer.barcodetest.preference",
        Context.MODE_PRIVATE
    )

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var flagSaved = false

    init {
        // Load saved state from SharedPreferences
        val savedBarcode = sharedPreferences.getString("BARCODE", "") ?: ""
        val savedFormat = sharedPreferences.getString("FORMAT", "") ?: ""
        
        if (savedBarcode.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                barcodeValue = savedBarcode,
                barcodeFormat = savedFormat,
                isBarcodeScanned = true,
                showButtons = true
            )
        }
    }

    fun onBarcodeFound(barcode: String?, format: Int) {
        if (barcode.isNullOrEmpty()) return
        
        val formatName = utils.nameOfBarcodeFormat(format)
        
        _uiState.value = _uiState.value.copy(
            barcodeValue = barcode,
            barcodeFormat = formatName,
            barcodeFormatInt = format,
            isBarcodeScanned = true,
            showButtons = true
        )
        
        saveState()
        sendBroadcast(barcode, formatName)
    }

    fun resetScanner() {
        utils.debug("resetting scanner")
        flagSaved = false
        _uiState.value = CameraUiState()
        saveState()
    }

    private fun saveState() {
        val state = _uiState.value
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("BARCODE", state.barcodeValue)
        editor.putString("FORMAT", state.barcodeFormat)
        editor.apply()

        if (!flagSaved && state.barcodeValue.isNotEmpty() && state.barcodeFormat.isNotEmpty()) {
            viewModelScope.launch {
                repository.saveBarcode(
                    state.barcodeValue,
                    state.barcodeFormat,
                    state.barcodeFormatInt
                ).onSuccess {
                    flagSaved = true
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
            }
        }
    }

    private fun sendBroadcast(barcodeValue: String, barcodeFormat: String) {
        val intent = Intent("ua.com.programmer.barcodetest.BARCODE_SCANNED")
        intent.putExtra("BARCODE_VALUE", barcodeValue)
        intent.putExtra("BARCODE_FORMAT", barcodeFormat)
        context.sendBroadcast(intent)
    }

    fun setShowButtons(show: Boolean) {
        _uiState.value = _uiState.value.copy(showButtons = show)
    }

    fun setError(error: String?) {
        _uiState.value = _uiState.value.copy(error = error)
    }
}

