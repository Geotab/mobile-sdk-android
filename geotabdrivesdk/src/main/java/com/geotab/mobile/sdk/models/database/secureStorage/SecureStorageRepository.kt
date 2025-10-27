package com.geotab.mobile.sdk.models.database.secureStorage

class SecureStorageRepository(
    private val secureStorageDao: SecureStorageDao
) {
    fun getValue(key: String): ByteArray? = secureStorageDao.getValue(key)

    fun insertOrUpdate(secureStorage: SecureStorage) = secureStorageDao.insertOrUpdate(secureStorage)
    fun delete(key: String) = secureStorageDao.deleteKey(key)
    fun deleteAll() = secureStorageDao.deleteAll()
    fun getKeys() = secureStorageDao.getKeys()
    fun length() = secureStorageDao.length()
}
