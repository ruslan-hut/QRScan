package ua.com.programmer.barcodetest

import android.Manifest
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ua.com.programmer.barcodetest.error.ErrorDisplay
import ua.com.programmer.barcodetest.viewmodel.CameraViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.Nonnull

@AndroidEntryPoint
class CameraFragment : Fragment() {

    private val viewModel: CameraViewModel by viewModels()

    private lateinit var cameraView: PreviewView
    private lateinit var textView: TextView
    private lateinit var buttons: LinearLayout
    private lateinit var floatingActionButton: FloatingActionButton
    private val utils = Utils()

    private val cameraProvider: ListenableFuture<ProcessCameraProvider> by lazy {
        ProcessCameraProvider.getInstance(requireContext())
    }
    private val cameraExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor()
    }

    private fun buttonsVisibilityTrigger(visible: Boolean) {
        viewModel.setShowButtons(visible)
        if (visible) {
            stopCamera()
            floatingActionButton.visibility = View.GONE
            buttons.visibility = View.VISIBLE
        } else {
            floatingActionButton.visibility = View.VISIBLE
            buttons.visibility = View.GONE
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

        textView = view.findViewById(R.id.txtContent)
        cameraView = view.findViewById(R.id.camera_view)

        // Observe ViewModel state
        observeViewModelState()

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

    private fun setupCamera() {
                    cameraProvider.addListener({
                        val provider = cameraProvider.get()
                        val preview = Preview.Builder().build()
                        preview.surfaceProvider = cameraView.surfaceProvider

                        val imageAnalysis = ImageAnalysis.Builder().build()
                        imageAnalysis.setAnalyzer(
                            cameraExecutor,
                            BarcodeImageAnalyzer(object : BarcodeFoundListener {
                                override fun onBarcodeFound(barCode: String?, format: Int) {
                                    viewModel.onBarcodeFound(barCode, format)
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
                            provider.bindToLifecycle(
                                viewLifecycleOwner,
                                cameraSelector,
                                imageAnalysis,
                                preview
                            )
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

    private fun showBarcodeValue(format: String, value: String) {
        stopCamera()

        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        tg.startTone(ToneGenerator.TONE_PROP_BEEP)

        val barcodeText = """
            $format
            $value
            """.trimIndent()
        textView.text = barcodeText
    }

    private fun resetScanner() {
        utils.debug("resetting scanner")
        try {
            setupCamera()
        } catch (ex: Exception) {
            Toast.makeText(requireContext(), R.string.hint_try_to_reset, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Properly shutdown camera executor to prevent memory leaks
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }
}