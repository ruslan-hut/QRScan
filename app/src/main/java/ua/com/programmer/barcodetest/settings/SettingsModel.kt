package ua.com.programmer.qrscanner.settings

/**
 * Data class representing app settings
 */
data class SettingsModel(
    val autoSave: Boolean = true,
    val historyRetentionDays: Int = 30,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = false,
    val cameraFlashEnabled: Boolean = false,
    val imageBrightness: Int = 80,
    val imageContrast: Int = 140
)

