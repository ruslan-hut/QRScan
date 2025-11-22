package ua.com.programmer.barcodetest.data

data class BarcodeHistoryItem(
    val id: Long,
    val date: String,
    val time: Long,
    val codeType: Int,
    val codeValue: String,
    val note: String? = null
)

