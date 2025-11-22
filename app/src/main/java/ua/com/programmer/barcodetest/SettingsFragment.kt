package ua.com.programmer.barcodetest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ua.com.programmer.barcodetest.viewmodel.SettingsViewModel

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var autoSaveSwitch: SwitchCompat
    private lateinit var historyRetentionSeekBar: SeekBar
    private lateinit var historyRetentionValue: TextView
    private lateinit var soundSwitch: SwitchCompat
    private lateinit var vibrationSwitch: SwitchCompat
    private lateinit var cameraFlashSwitch: SwitchCompat
    private lateinit var resetButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        initializeViews(view)
        setupObservers()
        setupListeners()

        return view
    }

    private fun initializeViews(view: View) {
        autoSaveSwitch = view.findViewById(R.id.settings_auto_save_switch)
        historyRetentionSeekBar = view.findViewById(R.id.settings_history_retention_seekbar)
        historyRetentionValue = view.findViewById(R.id.settings_history_retention_value)
        soundSwitch = view.findViewById(R.id.settings_sound_switch)
        vibrationSwitch = view.findViewById(R.id.settings_vibration_switch)
        cameraFlashSwitch = view.findViewById(R.id.settings_camera_flash_switch)
        resetButton = view.findViewById(R.id.settings_reset_button)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Update UI based on state
                autoSaveSwitch.isChecked = state.settings.autoSave
                historyRetentionSeekBar.progress = state.settings.historyRetentionDays
                updateHistoryRetentionText(state.settings.historyRetentionDays)
                soundSwitch.isChecked = state.settings.soundEnabled
                vibrationSwitch.isChecked = state.settings.vibrationEnabled
                cameraFlashSwitch.isChecked = state.settings.cameraFlashEnabled

                // Show reset dialog
                if (state.showResetDialog) {
                    showResetDialog()
                }

                // Show message
                state.message?.let { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    viewModel.clearMessage()
                }
            }
        }
    }

    private fun setupListeners() {
        autoSaveSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateAutoSave(isChecked)
        }

        historyRetentionSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val days = progress.coerceAtLeast(1)
                    updateHistoryRetentionText(days)
                    viewModel.updateHistoryRetentionDays(days)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateSoundEnabled(isChecked)
        }

        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateVibrationEnabled(isChecked)
        }

        cameraFlashSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateCameraFlashEnabled(isChecked)
        }

        resetButton.setOnClickListener {
            viewModel.showResetDialog()
        }
    }

    private fun updateHistoryRetentionText(days: Int) {
        historyRetentionValue.text = "$days ${if (days == 1) "day" else "days"}"
    }

    private fun showResetDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_reset_dialog_title)
            .setMessage(R.string.settings_reset_dialog_message)
            .setPositiveButton(R.string.settings_reset_confirm) { _, _ ->
                viewModel.resetToDefaults()
            }
            .setNegativeButton(R.string.dialog_cancel) { _, _ ->
                viewModel.hideResetDialog()
            }
            .setOnDismissListener {
                viewModel.hideResetDialog()
            }
            .show()
    }
}

