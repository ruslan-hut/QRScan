package ua.com.programmer.barcodetest.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ua.com.programmer.barcodetest.data.datasource.BarcodeDataSource
import ua.com.programmer.barcodetest.data.datasource.BarcodeLocalDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    @Singleton
    fun provideBarcodeDataSource(
        @ApplicationContext context: Context
    ): BarcodeDataSource {
        return BarcodeLocalDataSource(context)
    }
}

