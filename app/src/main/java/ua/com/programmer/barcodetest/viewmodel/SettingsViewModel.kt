package ua.com.programmer.qrscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.com.programmer.qrscanner.settings.SettingsModel
import ua.com.programmer.qrscanner.settings.SettingsPreferences
import javax.inject.Inject

data class SettingsUiState(
    val settings: SettingsModel = SettingsModel(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val showResetDialog: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val settings = SettingsModel(
                autoSave = settingsPreferences.autoSave,
                historyRetentionDays = settingsPreferences.historyRetentionDays,
                soundEnabled = settingsPreferences.soundEnabled,
                vibrationEnabled = settingsPreferences.vibrationEnabled,
                cameraFlashEnabled = settingsPreferences.cameraFlashEnabled
            )
            _uiState.value = _uiState.value.copy(
                settings = settings,
                isLoading = false
            )
        }
    }

    fun updateAutoSave(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.autoSave = enabled
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings.copy(autoSave = enabled)
            )
        }
    }

    fun updateHistoryRetentionDays(days: Int) {
        viewModelScope.launch {
            if (days in 1..365) {
                settingsPreferences.historyRetentionDays = days
                _uiState.value = _uiState.value.copy(
                    settings = _uiState.value.settings.copy(historyRetentionDays = days)
                )
            }
        }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.soundEnabled = enabled
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings.copy(soundEnabled = enabled)
            )
        }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.vibrationEnabled = enabled
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings.copy(vibrationEnabled = enabled)
            )
        }
    }

    fun updateCameraFlashEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.cameraFlashEnabled = enabled
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings.copy(cameraFlashEnabled = enabled)
            )
        }
    }

    fun showResetDialog() {
        _uiState.value = _uiState.value.copy(showResetDialog = true)
    }

    fun hideResetDialog() {
        _uiState.value = _uiState.value.copy(showResetDialog = false)
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            settingsPreferences.resetToDefaults()
            loadSettings()
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                showResetDialog = false,
                message = "Settings reset to defaults"
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

