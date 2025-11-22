package ua.com.programmer.qrscanner.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ua.com.programmer.qrscanner.data.database.BarcodeDatabase
import ua.com.programmer.qrscanner.data.database.BarcodeHistoryDao
import ua.com.programmer.qrscanner.data.datasource.BarcodeDataSource
import ua.com.programmer.qrscanner.data.datasource.BarcodeLocalDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    @Singleton
    fun provideBarcodeDatabase(
        @ApplicationContext context: Context
    ): BarcodeDatabase {
        return BarcodeDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideBarcodeHistoryDao(
        database: BarcodeDatabase
    ): BarcodeHistoryDao {
        return database.barcodeHistoryDao()
    }

    @Provides
    @Singleton
    fun provideBarcodeDataSource(
        dao: BarcodeHistoryDao
    ): BarcodeDataSource {
        return BarcodeLocalDataSource(dao)
    }
}

