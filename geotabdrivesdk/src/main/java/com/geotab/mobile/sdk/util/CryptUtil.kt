package com.geotab.mobile.sdk.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.Charset
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

const val KEY_TYPE = "AndroidKeyStore"
const val GCM_TAG_BIT_LENGTH = 128
const val IV_LENGTH = 12
internal fun getCipher(): Cipher {
    return Cipher.getInstance(
        KeyProperties.KEY_ALGORITHM_AES + "/" +
            KeyProperties.BLOCK_MODE_GCM + "/" +
            KeyProperties.ENCRYPTION_PADDING_NONE
    )
}

internal fun getSecretKey(alias: String): SecretKey {
    val keyStore = KeyStore.getInstance(KEY_TYPE)
    keyStore.load(null)
    keyStore.getKey(alias, null)?.let { return it as SecretKey }
    return createSecretKey(alias)
}

internal fun createSecretKey(alias: String): SecretKey {
    val paramsBuilder = KeyGenParameterSpec.Builder(
        alias,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )

    paramsBuilder.apply {
        setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    }
    val keyGenParameterSpec = paramsBuilder.build()
    val keyGenerator = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES, KEY_TYPE
    )
    keyGenerator.init(keyGenParameterSpec)
    return keyGenerator.generateKey()
}

internal fun encryptText(alias: String, value: String): ByteArray {
    val cipher = getCipher()
    cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(alias))
    val iv = cipher.iv
    val encryptedText = cipher.doFinal(value.toByteArray(Charset.defaultCharset()))
    return iv + encryptedText
}

internal fun decryptText(alias: String, encryptedText: ByteArray): String {
    val params = GCMParameterSpec(GCM_TAG_BIT_LENGTH, encryptedText, 0, IV_LENGTH)
    val cipher = getCipher()
    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(alias), params)
    val result = cipher.doFinal(encryptedText, IV_LENGTH, encryptedText.size - IV_LENGTH)
    return String(result, Charset.defaultCharset())
}
