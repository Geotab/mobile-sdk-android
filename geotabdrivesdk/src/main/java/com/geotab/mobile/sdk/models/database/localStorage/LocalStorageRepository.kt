package com.geotab.mobile.sdk.models.database.localStorage

class LocalStorageRepository(
    private val localStorageDao: LocalStorageDao
) {
    fun getValue(key: String): ByteArray? = localStorageDao.getValue(key)

    fun insertOrUpdate(localStorage: LocalStorage) = localStorageDao.insertOrUpdate(localStorage)
    fun delete(key: String) = localStorageDao.deleteKey(key)
    fun deleteAll() = localStorageDao.deleteAll()
    fun getKeys() = localStorageDao.getKeys()
    fun length() = localStorageDao.length()
}
