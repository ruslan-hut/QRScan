package ua.com.programmer.barcodetest.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ua.com.programmer.barcodetest.settings.SettingsPreferences
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppPreferences

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @AppPreferences
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences(
            "ua.com.programmer.barcodetest.preference",
            Context.MODE_PRIVATE
        )
    }

    @Provides
    @Singleton
    fun provideSettingsPreferences(
        @AppPreferences sharedPreferences: SharedPreferences
    ): SettingsPreferences {
        return SettingsPreferences(sharedPreferences)
    }
}

