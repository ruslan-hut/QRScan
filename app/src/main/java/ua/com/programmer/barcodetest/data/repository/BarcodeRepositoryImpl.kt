package ua.com.programmer.qrscanner.data.repository

import ua.com.programmer.qrscanner.data.BarcodeHistoryItem
import ua.com.programmer.qrscanner.data.datasource.BarcodeDataSource
import ua.com.programmer.qrscanner.error.AppError
import ua.com.programmer.qrscanner.error.ErrorMapper

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
        codeType: Int,
        imagePath: String?
    ): Result<Unit> {
        return try {
            val success = localDataSource.saveBarcode(barcodeValue, barcodeFormat, codeType, imagePath)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(AppError.DatabaseError.SaveFailed)
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.mapDatabaseError(e, "save"))
        }
    }

    override suspend fun getAllHistoryItems(): Result<List<BarcodeHistoryItem>> {
        return try {
            val items = localDataSource.getAllHistoryItems()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(ErrorMapper.mapDatabaseError(e, "load"))
        }
    }

    override suspend fun deleteHistoryItem(itemId: Long): Result<Unit> {
        return try {
            val success = localDataSource.deleteHistoryItem(itemId)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(AppError.DatabaseError.DeleteFailed)
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.mapDatabaseError(e, "delete"))
        }
    }

    override suspend fun cleanOldHistory(retentionDays: Int): Result<Int> {
        return try {
            val deletedCount = localDataSource.cleanOldHistory(retentionDays)
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(ErrorMapper.mapDatabaseError(e, "cleanup"))
        }
    }
}

