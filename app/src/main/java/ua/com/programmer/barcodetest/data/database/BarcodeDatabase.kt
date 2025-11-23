package ua.com.programmer.qrscanner.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database for barcode history.
 * Version 2 matches the original SQLiteOpenHelper version.
 */
@Database(
    entities = [BarcodeHistoryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class BarcodeDatabase : RoomDatabase() {

    abstract fun barcodeHistoryDao(): BarcodeHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: BarcodeDatabase? = null

        private const val DATABASE_NAME = "qrData"

        /**
         * Migration from version 1 to 2 (adding time column)
         * This matches the original SQLiteOpenHelper migration.
         * Note: If migrating from SQLiteOpenHelper, the database might already have version 2.
         * Room will handle this automatically if the schema matches.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Check if column already exists before adding
                val cursor = database.query("PRAGMA table_info(history)")
                var hasTimeColumn = false
                try {
                    while (cursor.moveToNext()) {
                        val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                        if (columnName == "time") {
                            hasTimeColumn = true
                            break
                        }
                    }
                } finally {
                    cursor.close()
                }

                if (!hasTimeColumn) {
                    database.execSQL("ALTER TABLE history ADD COLUMN time INTEGER")
                }
            }
        }

        /**
         * Migration from version 2 to 3 (adding imagePath column)
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Check if column already exists before adding
                val cursor = database.query("PRAGMA table_info(history)")
                var hasImagePathColumn = false
                try {
                    while (cursor.moveToNext()) {
                        val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                        if (columnName == "imagePath") {
                            hasImagePathColumn = true
                            break
                        }
                    }
                } finally {
                    cursor.close()
                }

                if (!hasImagePathColumn) {
                    database.execSQL("ALTER TABLE history ADD COLUMN imagePath TEXT")
                }
            }
        }

        fun getDatabase(context: Context): BarcodeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BarcodeDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    // Allow Room to work with existing SQLite database
                    // Room will validate the schema and apply migrations if needed
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

