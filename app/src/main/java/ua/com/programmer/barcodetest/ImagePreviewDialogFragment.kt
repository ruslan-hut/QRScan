package ua.com.programmer.qrscanner

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import coil.load
import java.io.File

/**
 * Dialog fragment to display full-size barcode image preview
 */
class ImagePreviewDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_IMAGE_PATH = "image_path"

        fun newInstance(imagePath: String): ImagePreviewDialogFragment {
            return ImagePreviewDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE_PATH, imagePath)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_image_preview, container, false)
        val imageView = view.findViewById<ImageView>(R.id.preview_image)
        val closeButton = view.findViewById<ImageView>(R.id.close_button)

        val imagePath = arguments?.getString(ARG_IMAGE_PATH)
        
        if (!imagePath.isNullOrEmpty() && ImageStorageHelper.imageExists(imagePath)) {
            imageView.load(File(imagePath)) {
                crossfade(true)
                error(R.drawable.product_48)
            }
        } else {
            imageView.setImageResource(R.drawable.product_48)
        }

        closeButton.setOnClickListener {
            dismiss()
        }

        imageView.setOnClickListener {
            dismiss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}

