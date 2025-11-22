package ua.com.programmer.barcodetest.data.repository

import android.content.Context
import ua.com.programmer.barcodetest.data.datasource.BarcodeDataSource
import ua.com.programmer.barcodetest.data.datasource.BarcodeLocalDataSource

/**
 * Provider for creating repository instances.
 * This centralizes repository creation and allows for easy dependency injection.
 */
object RepositoryProvider {

    /**
     * Creates a BarcodeRepository instance with default data sources
     */
    fun provideBarcodeRepository(context: Context): BarcodeRepository {
        val localDataSource: BarcodeDataSource = BarcodeLocalDataSource(context)
        return BarcodeRepositoryImpl(localDataSource)
    }
}

