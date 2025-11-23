package ua.com.programmer.qrscanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class ViewfinderOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x80FFFFFF.toInt() // Half-transparent white (50% opacity)
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x80000000.toInt() // Half-transparent black for overlay
    }

    private val cornerRadius = 24f // Rounded corner radius in pixels
    private val viewfinderSizeRatio = 0.7f // Viewfinder will be 70% of the smaller dimension

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        
        // Calculate viewfinder size (square, 70% of smaller dimension)
        val size = minOf(width, height) * viewfinderSizeRatio
        
        // Center the viewfinder
        val left = (width - size) / 2
        val top = (height - size) / 2
        val right = left + size
        val bottom = top + size
        
        val viewfinderRect = RectF(left, top, right, bottom)

        // Create a path that covers the entire screen but excludes the viewfinder area
        val overlayPath = Path().apply {
            // Add outer rectangle (entire screen)
            addRect(0f, 0f, width, height, Path.Direction.CW)
            // Subtract the viewfinder rectangle with rounded corners
            addRoundRect(viewfinderRect, cornerRadius, cornerRadius, Path.Direction.CCW)
            fillType = Path.FillType.EVEN_ODD
        }

        // Draw the dark overlay (everything except the viewfinder)
        canvas.drawPath(overlayPath, overlayPaint)

        // Draw the border around the viewfinder
        canvas.drawRoundRect(viewfinderRect, cornerRadius, cornerRadius, borderPaint)
    }
}

