package ua.com.programmer.qrscanner

import android.util.Log
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.*

class Utils {

    fun dateBeginShiftDate(retentionDays: Int = 30): Long {
        val calendar: Calendar = GregorianCalendar()
        val currentYear = calendar[Calendar.YEAR]
        val currentMonth = calendar[Calendar.MONTH]
        val currentDay = calendar[Calendar.DATE]
        calendar[currentYear, currentMonth, currentDay, 0] = 0
        return calendar.timeInMillis / 1000 - 86400L * retentionDays
    }

    fun nameOfBarcodeFormat(format: Int): String {
        val name: String = when (format) {
            Barcode.FORMAT_QR_CODE -> "QR code"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            Barcode.FORMAT_EAN_13 -> "EAN13"
            Barcode.FORMAT_CODE_128 -> "Code128"
            Barcode.FORMAT_UPC_A -> "UPC A"
            Barcode.TYPE_CALENDAR_EVENT -> "Calendar event"
            Barcode.TYPE_URL -> "URL"
            else -> "?"
        }
        return name
    }

    fun debug(text: String?) {
        Log.d("XBUG", text!!)
    }
}