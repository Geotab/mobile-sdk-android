package com.geotab.mobile.sdk.models.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.geotab.mobile.sdk.models.database.secureStorage.SecureStorage
import com.geotab.mobile.sdk.models.database.secureStorage.SecureStorageDao

@Database(entities = [SecureStorage::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun secureStorageDao(): SecureStorageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "native_storage"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
