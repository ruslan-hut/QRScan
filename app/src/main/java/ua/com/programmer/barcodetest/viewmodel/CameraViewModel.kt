package ua.com.programmer.barcodetest.viewmodel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.com.programmer.barcodetest.Utils
import ua.com.programmer.barcodetest.data.repository.BarcodeRepository
import ua.com.programmer.barcodetest.di.AppPreferences
import ua.com.programmer.barcodetest.error.AppError
import ua.com.programmer.barcodetest.error.ErrorMapper
import ua.com.programmer.barcodetest.error.getErrorMessage
import ua.com.programmer.barcodetest.settings.SettingsPreferences
import javax.inject.Inject

data class CameraUiState(
    val barcodeValue: String = "",
    val barcodeFormat: String = "",
    val barcodeFormatInt: Int = 0,
    val isBarcodeScanned: Boolean = false,
    val showButtons: Boolean = false,
    val isLoading: Boolean = false,
    val error: AppError? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: BarcodeRepository,
    @ApplicationContext private val context: Context,
    @AppPreferences private val sharedPreferences: SharedPreferences,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val utils = Utils()

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

        // Only save if auto-save is enabled
        if (!flagSaved && state.barcodeValue.isNotEmpty() && state.barcodeFormat.isNotEmpty() 
            && settingsPreferences.autoSave) {
            viewModelScope.launch {
                repository.saveBarcode(
                    state.barcodeValue,
                    state.barcodeFormat,
                    state.barcodeFormatInt
                ).onSuccess {
                    flagSaved = true
                }.onFailure { throwable ->
                    val appError = ErrorMapper.map(throwable)
                    _uiState.value = _uiState.value.copy(error = appError)
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

    fun setError(error: AppError?) {
        _uiState.value = _uiState.value.copy(error = error)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun setErrorFromString(errorMessage: String?) {
        errorMessage?.let {
            val appError = AppError.UnknownError(
                message = it,
                userMessage = it
            )
            setError(appError)
        }
    }
}

