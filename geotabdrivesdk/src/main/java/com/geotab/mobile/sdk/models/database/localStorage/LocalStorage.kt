package com.geotab.mobile.sdk.models.database.localStorage
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "localStorage")
data class LocalStorage(
    @PrimaryKey
    val localKey: String,
    val localValue: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalStorage

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
