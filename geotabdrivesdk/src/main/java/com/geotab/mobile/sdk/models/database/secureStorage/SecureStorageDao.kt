package com.geotab.mobile.sdk.models.database.secureStorage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SecureStorageDao {
    @Query("SELECT COUNT(localKey) FROM secureStorage")
    fun length(): Int

    @Query("SELECT localKey FROM secureStorage")
    fun getKeys(): List<String>

    @Query("SELECT localValue as value FROM secureStorage Where localKey = :key")
    fun getValue(key: String): ByteArray?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(secureStorage: SecureStorage)

    @Query("DELETE FROM secureStorage Where localKey = :key")
    fun deleteKey(key: String): Int

    @Query("DELETE FROM secureStorage")
    fun deleteAll(): Int
}
