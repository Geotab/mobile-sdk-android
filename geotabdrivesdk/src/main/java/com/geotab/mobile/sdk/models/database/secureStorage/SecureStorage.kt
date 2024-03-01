package com.geotab.mobile.sdk.models.database.secureStorage
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secureStorage")
data class SecureStorage(
    @PrimaryKey
    val localKey: String,
    val localValue: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SecureStorage

        if (localKey != other.localKey) return false
        if (!localValue.contentEquals(other.localValue)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = localKey.hashCode()
        result = 31 * result + localValue.contentHashCode()
        return result
    }
}
