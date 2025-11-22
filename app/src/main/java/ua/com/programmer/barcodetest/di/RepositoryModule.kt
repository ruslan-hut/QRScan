package ua.com.programmer.barcodetest.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ua.com.programmer.barcodetest.data.datasource.BarcodeDataSource
import ua.com.programmer.barcodetest.data.repository.BarcodeRepository
import ua.com.programmer.barcodetest.data.repository.BarcodeRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideBarcodeRepository(
        localDataSource: BarcodeDataSource
    ): BarcodeRepository {
        return BarcodeRepositoryImpl(localDataSource)
    }
}

