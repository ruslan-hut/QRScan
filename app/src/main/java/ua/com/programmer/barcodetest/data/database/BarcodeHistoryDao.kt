package ua.com.programmer.qrscanner.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for barcode history operations.
 */
@Dao
interface BarcodeHistoryDao {

    @Query("SELECT * FROM history ORDER BY time DESC")
    fun getAllHistoryItems(): Flow<List<BarcodeHistoryEntity>>

    @Query("SELECT * FROM history ORDER BY time DESC")
    suspend fun getAllHistoryItemsList(): List<BarcodeHistoryEntity>

    @Query("SELECT * FROM history WHERE _id = :id")
    suspend fun getHistoryItemById(id: Long): BarcodeHistoryEntity?

    @Insert
    suspend fun insertHistoryItem(item: BarcodeHistoryEntity): Long

    @Insert
    suspend fun insertHistoryItems(items: List<BarcodeHistoryEntity>)

    @Update
    suspend fun updateHistoryItem(item: BarcodeHistoryEntity)

    @Delete
    suspend fun deleteHistoryItem(item: BarcodeHistoryEntity)

    @Query("DELETE FROM history WHERE _id = :id")
    suspend fun deleteHistoryItemById(id: Long): Int

    @Query("DELETE FROM history WHERE time < :cutoffTime")
    suspend fun deleteOldHistory(cutoffTime: Long): Int

    @Query("DELETE FROM history")
    suspend fun deleteAllHistory()
}

