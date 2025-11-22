package ua.com.programmer.barcodetest.data.repository

import ua.com.programmer.barcodetest.data.BarcodeHistoryItem
import ua.com.programmer.barcodetest.data.datasource.BarcodeDataSource

/**
 * Implementation of BarcodeRepository.
 * This class coordinates between different data sources and provides
 * a unified interface for the domain layer.
 */
class BarcodeRepositoryImpl(
    private val localDataSource: BarcodeDataSource
) : BarcodeRepository {

    override suspend fun saveBarcode(
        barcodeValue: String,
        barcodeFormat: String,
        codeType: Int
    ): Result<Unit> {
        return try {
            val success = localDataSource.saveBarcode(barcodeValue, barcodeFormat, codeType)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to save barcode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllHistoryItems(): Result<List<BarcodeHistoryItem>> {
        return try {
            val items = localDataSource.getAllHistoryItems()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHistoryItem(itemId: Long): Result<Unit> {
        return try {
            val success = localDataSource.deleteHistoryItem(itemId)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete item"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cleanOldHistory(): Result<Int> {
        return try {
            val deletedCount = localDataSource.cleanOldHistory()
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

