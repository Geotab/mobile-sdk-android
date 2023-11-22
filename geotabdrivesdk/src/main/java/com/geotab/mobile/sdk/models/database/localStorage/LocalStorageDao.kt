package com.geotab.mobile.sdk.models.database.localStorage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalStorageDao {
    @Query("SELECT COUNT(localKey) FROM localStorage")
    fun length(): Int

    @Query("SELECT localKey FROM localStorage")
    fun getKeys(): List<String>

    @Query("SELECT localValue as value FROM localStorage Where localKey = :key")
    fun getValue(key: String): ByteArray?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(localStorage: LocalStorage)

    @Query("DELETE FROM localStorage Where localKey = :key")
    fun deleteKey(key: String): Int

    @Query("DELETE FROM localStorage")
    fun deleteAll(): Int
}
