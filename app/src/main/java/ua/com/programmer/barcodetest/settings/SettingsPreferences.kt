package ua.com.programmer.qrscanner.settings

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user preferences/settings.
 * Provides type-safe access to app settings.
 */
@Singleton
class SettingsPreferences @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val KEY_AUTO_SAVE = "auto_save_barcodes"
        private const val KEY_HISTORY_RETENTION_DAYS = "history_retention_days"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_CAMERA_FLASH = "camera_flash_enabled"
        private const val KEY_BRIGHTNESS = "image_brightness"
        private const val KEY_CONTRAST = "image_contrast"
        
        // Default values
        private const val DEFAULT_AUTO_SAVE = true
        private const val DEFAULT_HISTORY_RETENTION_DAYS = 30
        private const val DEFAULT_SOUND_ENABLED = true
        private const val DEFAULT_VIBRATION_ENABLED = false
        private const val DEFAULT_CAMERA_FLASH = false
        private const val DEFAULT_BRIGHTNESS = 80 // Range: 0-150
        private const val DEFAULT_CONTRAST = 140 // Range: 50-200 (represents 0.5-2.0, default 1.4)
    }

    var autoSave: Boolean
        get() = sharedPreferences.getBoolean(KEY_AUTO_SAVE, DEFAULT_AUTO_SAVE)
        set(value) = sharedPreferences.edit().putBoolean(KEY_AUTO_SAVE, value).apply()

    var historyRetentionDays: Int
        get() = sharedPreferences.getInt(KEY_HISTORY_RETENTION_DAYS, DEFAULT_HISTORY_RETENTION_DAYS)
        set(value) = sharedPreferences.edit().putInt(KEY_HISTORY_RETENTION_DAYS, value).apply()

    var soundEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED)
        set(value) = sharedPreferences.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var vibrationEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_VIBRATION_ENABLED, DEFAULT_VIBRATION_ENABLED)
        set(value) = sharedPreferences.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

    var cameraFlashEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_CAMERA_FLASH, DEFAULT_CAMERA_FLASH)
        set(value) = sharedPreferences.edit().putBoolean(KEY_CAMERA_FLASH, value).apply()

    var imageBrightness: Int
        get() = sharedPreferences.getInt(KEY_BRIGHTNESS, DEFAULT_BRIGHTNESS)
        set(value) = sharedPreferences.edit().putInt(KEY_BRIGHTNESS, value.coerceIn(0, 150)).apply()

    var imageContrast: Int
        get() = sharedPreferences.getInt(KEY_CONTRAST, DEFAULT_CONTRAST)
        set(value) = sharedPreferences.edit().putInt(KEY_CONTRAST, value.coerceIn(50, 200)).apply()

    fun resetToDefaults() {
        sharedPreferences.edit()
            .putBoolean(KEY_AUTO_SAVE, DEFAULT_AUTO_SAVE)
            .putInt(KEY_HISTORY_RETENTION_DAYS, DEFAULT_HISTORY_RETENTION_DAYS)
            .putBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED)
            .putBoolean(KEY_VIBRATION_ENABLED, DEFAULT_VIBRATION_ENABLED)
            .putBoolean(KEY_CAMERA_FLASH, DEFAULT_CAMERA_FLASH)
            .putInt(KEY_BRIGHTNESS, DEFAULT_BRIGHTNESS)
            .putInt(KEY_CONTRAST, DEFAULT_CONTRAST)
            .apply()
    }
}

