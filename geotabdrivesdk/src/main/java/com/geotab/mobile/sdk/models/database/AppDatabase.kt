package com.geotab.mobile.sdk.models.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.geotab.mobile.sdk.models.database.localStorage.LocalStorage
import com.geotab.mobile.sdk.models.database.localStorage.LocalStorageDao

@Database(entities = [LocalStorage::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localStorageDao(): LocalStorageDao

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
