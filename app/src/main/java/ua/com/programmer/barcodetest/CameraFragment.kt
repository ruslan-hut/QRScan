package ua.com.programmer.qrscanner

import android.Manifest
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Bitmap
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ua.com.programmer.qrscanner.di.AppPreferences
import ua.com.programmer.qrscanner.error.ErrorDisplay
import ua.com.programmer.qrscanner.settings.SettingsPreferences
import ua.com.programmer.qrscanner.viewmodel.CameraViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.Nonnull
import javax.inject.Inject

@AndroidEntryPoint
class CameraFragment : Fragment() {

    private val viewModel: CameraViewModel by viewModels()
    
    @Inject
    lateinit var settingsPreferences: SettingsPreferences
    
    @Inject
    @AppPreferences
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var cameraView: PreviewView
    private lateinit var scannedImageView: ImageView
    private lateinit var textView: TextView
    private lateinit var buttons: LinearLayout
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var viewfinderOverlay: ViewfinderOverlay
    private val utils = Utils()
    private var currentProcessedBitmap: Bitmap? = null

    private val cameraProvider: ListenableFuture<ProcessCameraProvider> by lazy {
        ProcessCameraProvider.getInstance(requireContext())
    }
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private val settingsChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "camera_flash_enabled") {
            updateCameraFlash()
        }
    }

    private fun buttonsVisibilityTrigger(visible: Boolean) {
        viewModel.setShowButtons(visible)
        if (visible) {
            stopCamera()
            floatingActionButton.visibility = View.GONE
            buttons.visibility = View.VISIBLE
            viewfinderOverlay.visibility = View.GONE
        } else {
            floatingActionButton.visibility = View.VISIBLE
            buttons.visibility = View.GONE
            viewfinderOverlay.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_camera, container, false)

        floatingActionButton = view.findViewById(R.id.fab)
        floatingActionButton.setOnClickListener {
            buttonsVisibilityTrigger(true)
        }

        buttons = view.findViewById(R.id.buttons)
        textView = view.findViewById(R.id.txtContent)
        cameraView = view.findViewById(R.id.camera_view)
        scannedImageView = view.findViewById(R.id.scanned_image_view)
        viewfinderOverlay = view.findViewById(R.id.viewfinder_overlay)
        buttonsVisibilityTrigger(false)

        val btShare: TextView = view.findViewById(R.id.button_share)
        btShare.setOnClickListener {
            val state = viewModel.uiState.value
            if (state.barcodeValue.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_TEXT, state.barcodeValue)
                intent.setType("text/plain")
                startActivity(intent)
            }
        }

        val btSearch: TextView = view.findViewById(R.id.button_search)
        btSearch.setOnClickListener {
            val state = viewModel.uiState.value
            if (state.barcodeValue.isNotEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_WEB_SEARCH)
                    intent.putExtra(SearchManager.QUERY, state.barcodeValue)
                    startActivity(intent)
                } catch (noActivity: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), R.string.no_activity_error, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btReset: TextView = view.findViewById(R.id.button_reset)
        btReset.setOnClickListener { 
            viewModel.resetScanner()
            resetScanner()
        }

        // Initialize camera executor
        if (!::cameraExecutor.isInitialized) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }

        // Observe ViewModel state
        observeViewModelState()
        
        // Observe settings changes for camera flash
        observeSettingsChanges()

        if (requireContext().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            setupCamera()
        }

        return view
    }

    private fun observeViewModelState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Update UI based on state
                if (state.isBarcodeScanned) {
                    // Show barcode text (image is already displayed by showProcessedImage)
                    showBarcodeValue(state.barcodeFormat, state.barcodeValue)
                }
                
                if (state.showButtons) {
                    buttonsVisibilityTrigger(true)
                } else {
                    buttonsVisibilityTrigger(false)
                }
                
                state.error?.let { error ->
                    ErrorDisplay.showError(requireContext(), error)
                    // Clear error after displaying
                    viewModel.clearError()
                }
            }
        }
    }
    
    private fun observeSettingsChanges() {
        // Register listener for settings changes
        sharedPreferences.registerOnSharedPreferenceChangeListener(settingsChangeListener)
    }

    private fun setupCamera() {
                    // Ensure camera executor is initialized
                    if (!::cameraExecutor.isInitialized) {
                        cameraExecutor = Executors.newSingleThreadExecutor()
                    }
                    cameraProvider.addListener({
                        val provider = cameraProvider.get()
                        val preview = Preview.Builder().build()
                        preview.surfaceProvider = cameraView.surfaceProvider

                        val imageAnalysis = ImageAnalysis.Builder().build()
                        imageAnalysis.setAnalyzer(
                            cameraExecutor,
                            BarcodeImageAnalyzer(object : BarcodeFoundListener {
                                override fun onBarcodeFound(barcode: Barcode, imageProxy: ImageProxy) {
                                    // Process image on background thread
                                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                        try {
                                            // Get brightness and contrast settings
                                            val brightness = settingsPreferences.imageBrightness
                                            val contrast = settingsPreferences.imageContrast
                                            
                                            // Process image with bounding box
                                            val processedBitmap = BarcodeImageProcessor.processImageWithBarcode(
                                                imageProxy,
                                                barcode,
                                                brightness,
                                                contrast
                                            )
                                            
                                            // Close ImageProxy first
                                            imageProxy.close()
                                            
                                            // Update UI on main thread immediately with processed image
                                            withContext(Dispatchers.Main) {
                                                // Display processed image immediately
                                                processedBitmap?.let {
                                                    showProcessedImage(it)
                                                    // Store bitmap temporarily (will be recycled on reset)
                                                    currentProcessedBitmap = it
                                                }
                                                
                                                // Save image to storage in background
                                                launch(Dispatchers.IO) {
                                                    val imagePath = processedBitmap?.let {
                                                        ImageStorageHelper.saveImage(it, requireContext())
                                                    }
                                                    
                                                    // Update ViewModel with image path
                                                    withContext(Dispatchers.Main) {
                                                        viewModel.onBarcodeFound(
                                                            barcode.rawValue,
                                                            barcode.format,
                                                            imagePath
                                                        )
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            utils.debug("Error processing image: ${e.message}")
                                            imageProxy.close()
                                            withContext(Dispatchers.Main) {
                                                // Still notify about barcode even if image processing fails
                                                viewModel.onBarcodeFound(
                                                    barcode.rawValue,
                                                    barcode.format,
                                                    null
                                                )
                                            }
                                        }
                                    }
                                }

                                override fun onCodeNotFound(error: String?) {
                                    utils.debug("on code not found: $error")
                                    viewModel.setErrorFromString(error)
                                }
                            })
                        )

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            provider.unbindAll()
                            camera = provider.bindToLifecycle(
                                viewLifecycleOwner,
                                cameraSelector,
                                imageAnalysis,
                                preview
                            )
                            // Update flash state after camera is bound
                            updateCameraFlash()
                        } catch (e: Exception) {
                            utils.debug("bind provider error; " + e.message)
                        }
                    }, ContextCompat.getMainExecutor(requireContext()))
                }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        @Nonnull permissions: Array<String>,
        @Nonnull grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            resetScanner()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun stopCamera() {
        val handler = Handler(requireContext().mainLooper)
        handler.post {
            try {
                cameraProvider.get().unbindAll()
            } catch (e: Exception) {
                utils.debug("stopCamera: $e")
            }
        }
    }

    private fun showProcessedImage(bitmap: Bitmap) {
        stopCamera()
        
        // Hide camera view and show processed image
        cameraView.visibility = View.GONE
        viewfinderOverlay.visibility = View.GONE
        scannedImageView.visibility = View.VISIBLE
        scannedImageView.setImageBitmap(bitmap)

        // Play sound if enabled
        if (settingsPreferences.soundEnabled) {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP)
        }

        // Vibrate if enabled
        if (settingsPreferences.vibrationEnabled) {
            vibrate()
        }
    }

    private fun showBarcodeValue(format: String, value: String) {
        val barcodeText = """
            $format
            $value
            """.trimIndent()
        textView.text = barcodeText
    }

    private fun vibrate() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        } catch (e: Exception) {
            // Vibration not available or permission denied
            utils.debug("Vibration error: ${e.message}")
        }
    }

    private fun updateCameraFlash() {
        try {
            val cameraControl = camera?.cameraControl
            val flashEnabled = settingsPreferences.cameraFlashEnabled
            
            if (cameraControl != null) {
                cameraControl.enableTorch(flashEnabled)
            }
        } catch (e: Exception) {
            utils.debug("Camera flash error: ${e.message}")
        }
    }

    private fun resetScanner() {
        utils.debug("resetting scanner")
        
        // Clear processed image
        currentProcessedBitmap?.recycle()
        currentProcessedBitmap = null
        scannedImageView.setImageBitmap(null)
        scannedImageView.visibility = View.GONE
        
        // Show camera view again
        cameraView.visibility = View.VISIBLE
        viewfinderOverlay.visibility = View.VISIBLE
        
        try {
            setupCamera()
        } catch (ex: Exception) {
            Toast.makeText(requireContext(), R.string.hint_try_to_reset, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Unregister settings change listener
        if (::sharedPreferences.isInitialized) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(settingsChangeListener)
        }
        // Recycle processed bitmap if still held
        currentProcessedBitmap?.recycle()
        currentProcessedBitmap = null
        // Properly shutdown camera executor to prevent memory leaks
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }
}