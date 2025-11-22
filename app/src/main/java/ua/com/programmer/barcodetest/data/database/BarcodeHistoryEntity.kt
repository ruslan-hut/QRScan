package ua.com.programmer.qrscanner.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for barcode history items.
 * Maps to the "history" table in the database.
 * Column names match the original SQLiteOpenHelper schema for compatibility.
 */
@Entity(tableName = "history")
data class BarcodeHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Long = 0,
    val date: String,
    val time: Long,
    @ColumnInfo(name = "codeType")
    val codeType: Int,
    @ColumnInfo(name = "codeValue")
    val codeValue: String,
    val note: String? = null
)

