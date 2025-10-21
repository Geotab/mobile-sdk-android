package com.geotab.mobile.sdk.models.database.secureStorage

import com.geotab.mobile.sdk.BuildConfig
import com.geotab.mobile.sdk.util.decryptTextToChars
import com.geotab.mobile.sdk.util.encryptText

class SecureStorageRepository(
    packageName: String,
    private val secureStorageDao: SecureStorageDao
) {
    val keyAlias = packageName + "." + BuildConfig.KEYSTORE_ALIAS

    suspend fun getValueChars(key: String): CharArray? {
        val byteResult = secureStorageDao.getValue(key)
        return if (byteResult == null) {
            null
        } else {
            decryptTextToChars(keyAlias, byteResult)
        }
    }

    suspend fun insertOrUpdate(key: String, value: CharArray) {
        try {
            val secureStorage = SecureStorage(
                key,
                encryptText(keyAlias, String(value))
            )
            secureStorageDao.insertOrUpdate(secureStorage)
        } finally {
            value.fill('\u0000')
        }
    }

    suspend fun delete(key: String) = secureStorageDao.deleteKey(key)
    suspend fun deleteAll() = secureStorageDao.deleteAll()
    suspend fun getKeys() = secureStorageDao.getKeys()
    suspend fun length() = secureStorageDao.length()
}
