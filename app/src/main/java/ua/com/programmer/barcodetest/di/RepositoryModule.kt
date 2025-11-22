package ua.com.programmer.qrscanner.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ua.com.programmer.qrscanner.data.datasource.BarcodeDataSource
import ua.com.programmer.qrscanner.data.repository.BarcodeRepository
import ua.com.programmer.qrscanner.data.repository.BarcodeRepositoryImpl
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

