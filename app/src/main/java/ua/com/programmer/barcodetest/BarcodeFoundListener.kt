package ua.com.programmer.qrscanner

interface BarcodeFoundListener {
    fun onBarcodeFound(barCode: String?, format: Int)
    fun onCodeNotFound(error: String?)
}
