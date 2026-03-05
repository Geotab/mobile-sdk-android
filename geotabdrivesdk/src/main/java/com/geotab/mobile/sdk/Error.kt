package com.geotab.mobile.sdk

import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import java.lang.RuntimeException

class Error(private var geotabDriveError: GeotabDriveError, errorMsg: String? = null) :
    RuntimeException(errorMsg) {
    private var geotabDriveMessage: String? = errorMsg

    fun getErrorCode(): GeotabDriveError {
        return geotabDriveError
    }

    fun getErrorMessage(): String {
        return geotabDriveMessage ?: ""
    }
}
