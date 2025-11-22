package ua.com.programmer.qrscanner

import android.content.ContentValues
import android.database.Cursor
import android.util.Log

class CursorHelper internal constructor(cursor: Cursor) {
    private val values: ContentValues

    init {
        var i: Int
        val columns = cursor.columnNames
        values = ContentValues()
        try {
            for (column in columns) {
                i = cursor.getColumnIndex(column)
                if (column == "_id") values.put("raw_id", cursor.getLong(i))
                else when (cursor.getType(i)) {
                    Cursor.FIELD_TYPE_FLOAT -> values.put(column, cursor.getDouble(i))
                    Cursor.FIELD_TYPE_INTEGER -> values.put(column, cursor.getLong(i))
                    else -> values.put(column, cursor.getString(i))
                }
            }
        } catch (e: Exception) {
            Log.e("XBUG", "Cursor helper init: $e")
        }
    }

    fun getInt(column: String?): Int {
        var value = 0
        if (column != null) {
            if (values.containsKey(column) && values[column] != null) {
                if (values.getAsString(column).isNotEmpty()) value = values.getAsInteger(column)
            }
        }
        return value
    }

    fun getLong(column: String?): Long {
        var value: Long = 0
        if (column != null) {
            if (values.containsKey(column) && values[column] != null) {
                if (values.getAsString(column).isNotEmpty()) value = values.getAsLong(column)
            }
        }
        return value
    }

    fun getString(column: String?): String {
        var value = ""
        if (column != null) {
            if (values.containsKey(column) && values[column] != null) {
                value = values.getAsString(column)
            }
        }
        return value
    }
}
