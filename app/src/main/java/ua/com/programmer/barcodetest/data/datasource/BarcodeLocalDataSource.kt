package ua.com.programmer.barcodetest.data.datasource

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.com.programmer.barcodetest.DBHelper
import ua.com.programmer.barcodetest.Utils
import ua.com.programmer.barcodetest.data.BarcodeHistoryItem
import java.util.Date
import java.util.Locale

/**
 * Local data source implementation using SQLite database.
 * This handles all direct database operations.
 */
class BarcodeLocalDataSource(private val context: Context) : BarcodeDataSource {

    private val dbHelper = DBHelper(context)
    private val utils = Utils()

    override suspend fun saveBarcode(
        barcodeValue: String,
        barcodeFormat: String,
        codeType: Int
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (barcodeValue.isNotEmpty() && barcodeFormat.isNotEmpty()) {
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
                    cv.put("codeType", codeType)
                    cv.put("codeValue", barcodeValue)
                    db.insert("history", null, cv)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun getAllHistoryItems(): List<BarcodeHistoryItem> {
        return withContext(Dispatchers.IO) {
            val items = mutableListOf<BarcodeHistoryItem>()
            val db = dbHelper.readableDatabase
            val cursor = db.query("history", null, null, null, null, null, "time DESC")

            try {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
                    val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))
                    val time = cursor.getLong(cursor.getColumnIndexOrThrow("time"))
                    val codeType = cursor.getInt(cursor.getColumnIndexOrThrow("codeType"))
                    val codeValue = cursor.getString(cursor.getColumnIndexOrThrow("codeValue"))
                    val noteIndex = cursor.getColumnIndex("note")
                    val note = if (noteIndex >= 0 && !cursor.isNull(noteIndex)) {
                        cursor.getString(noteIndex)
                    } else {
                        null
                    }

                    items.add(BarcodeHistoryItem(id, date, time, codeType, codeValue, note))
                }
            } finally {
                cursor.close()
            }

            items
        }
    }

    override suspend fun deleteHistoryItem(itemId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val db = dbHelper.writableDatabase
                val deletedRows = db.delete("history", "_id=?", arrayOf(itemId.toString()))
                deletedRows > 0
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun cleanOldHistory(): Int {
        return withContext(Dispatchers.IO) {
            try {
                val db = dbHelper.writableDatabase
                val cutoffTime = utils.dateBeginShiftDate()
                db.delete("history", "time<?", arrayOf(cutoffTime.toString()))
            } catch (e: Exception) {
                0
            }
        }
    }
}

