package ua.com.programmer.barcodetest

import android.Manifest
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.Nonnull

class CameraFragment : Fragment() {

    private lateinit var cameraView: PreviewView
    private lateinit var textView: TextView
    private lateinit var buttons: LinearLayout
    private lateinit var sharedPreferences: SharedPreferences
    private var barcodeValue: String = ""
    private var barcodeFormat: String = ""
    private var barcodeFormatInt: Int = 0
    private var flagSaved = false
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var dbHelper: DBHelper
    private val utils = Utils()

    private val cameraProvider: ListenableFuture<ProcessCameraProvider> by lazy {
        ProcessCameraProvider.getInstance(requireContext())
    }
    private val cameraExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor()
    }

    private fun buttonsVisibilityTrigger(visible: Boolean) {
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

        dbHelper = DBHelper(requireContext())

        floatingActionButton = view.findViewById(R.id.fab)
        floatingActionButton.setOnClickListener {
            buttonsVisibilityTrigger(
                true
            )
        }

        buttons = view.findViewById(R.id.buttons)
        buttonsVisibilityTrigger(false)

        val btShare: TextView = view.findViewById(R.id.button_share)
        btShare.setOnClickListener {
            if (barcodeValue != "") {
                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_TEXT, barcodeValue)
                intent.setType("text/plain")
                startActivity(intent)
            }
        }

        val btSearch: TextView = view.findViewById(R.id.button_search)
        btSearch.setOnClickListener { v: View? ->
            if (barcodeValue != "") {
                try {
                    val intent = Intent(Intent.ACTION_WEB_SEARCH)
                    intent.putExtra(SearchManager.QUERY, barcodeValue)
                    startActivity(intent)
                } catch (noActivity: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), R.string.no_activity_error, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btReset: TextView = view.findViewById(R.id.button_reset)
        btReset.setOnClickListener { resetScanner() }

        sharedPreferences = requireContext().getSharedPreferences(
            "ua.com.programmer.barcodetest.preference",
            Context.MODE_PRIVATE
        )
        barcodeValue = sharedPreferences.getString("BARCODE", "") ?: ""
        barcodeFormat = sharedPreferences.getString("FORMAT", "") ?: ""

        textView = view.findViewById(R.id.txtContent)
        cameraView = view.findViewById(R.id.camera_view)

        if (requireContext().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            setupCamera()
        }

        return view
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
                                    barcodeValue = barCode ?: ""
                                    barcodeFormatInt = format
                                    barcodeFormat = Utils().nameOfBarcodeFormat(format)
                                    showBarcodeValue()
                                }

                                override fun onCodeNotFound(error: String?) {
                                    utils.debug("on code not found: $error")
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

    private fun showBarcodeValue() {
        stopCamera()

        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        tg.startTone(ToneGenerator.TONE_PROP_BEEP)

        val barcodeText = """
            $barcodeFormat
            $barcodeValue
            """.trimIndent()
        textView.text = barcodeText

        saveState()
        buttonsVisibilityTrigger(true)

        // Send broadcast intent with barcode data
        val intent = Intent("ua.com.programmer.barcodetest.BARCODE_SCANNED")
        intent.putExtra("BARCODE_VALUE", barcodeValue)
        intent.putExtra("BARCODE_FORMAT", barcodeFormat)
        requireContext().sendBroadcast(intent)
    }

    private fun saveState() {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("BARCODE", barcodeValue)
        editor.putString("FORMAT", barcodeFormat)
        editor.apply()

        if (!flagSaved) {
            if (barcodeValue != "" && barcodeFormat != "") {
                val currentDate = Date()
                val eventTime = String.format("%ts", currentDate).toInt().toLong()
                val eventDate = String.format(
                    Locale.getDefault(),
                    "%td-%tm-%tY",
                    currentDate,
                    currentDate,
                    currentDate
                )

                val db: SQLiteDatabase = dbHelper.writableDatabase
                val cv = ContentValues()
                cv.put("time", eventTime)
                cv.put("date", eventDate)
                cv.put("codeType", barcodeFormatInt)
                cv.put("codeValue", barcodeValue)
                db.insert("history", null, cv)
                flagSaved = true
            }
        }
    }

    private fun resetScanner() {
        utils.debug("resetting scanner")
        flagSaved = false
        barcodeValue = ""
        barcodeFormat = ""
        saveState()
        try {
            //cameraProvider.unbindAll()
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