package com.geotab.mobile.sdk.module.auth

/**
 * Auth Module Error Types
 *
 * Sealed class representing all possible errors that can occur during authentication operations.
 * Each error type provides a descriptive message through the [errorDescription] property.
 */
sealed class AuthError : Exception() {
    /**
     * Failed parsing session.
     */
    object SessionParseFailedError : AuthError() {
        private fun readResolve(): Any = SessionParseFailedError
    }

    /**
     * Failed retrieving session.
     */
    object SessionRetrieveFailedError : AuthError() {
        private fun readResolve(): Any = SessionRetrieveFailedError
    }

    /**
     * Failed parsing Json.
     */
    object ParseFailedError : AuthError() {
        private fun readResolve(): Any = ParseFailedError
    }

    /**
     * No data returned from authorization flow.
     */
    object NoDataFoundError : AuthError() {
        private fun readResolve(): Any = NoDataFoundError
    }

    /**
     * Insecure Discovery URI. HTTPS is required.
     */
    object InvalidURL : AuthError() {
        private fun readResolve(): Any = InvalidURL
    }

    /**
     * Login redirect scheme key not found in AndroidManifest.xml.
     */
    data class InvalidRedirectScheme(val schemeKey: String) : AuthError()

    /**
     * Failed to save auth state for user.
     */
    data class FailedToSaveAuthState(
        val username: String,
        val underlyingError: Throwable
    ) : AuthError()

    /**
     * Returns a human-readable error description.
     */
    val errorDescription: String
        get() = when (this) {
            is SessionParseFailedError -> "Failed parsing session."
            is SessionRetrieveFailedError -> "Failed retrieving session."
            is ParseFailedError -> "Failed parsing Json."
            is NoDataFoundError -> "No data returned from authorization flow."
            is InvalidURL -> "Insecure Discovery URI. HTTPS is required."
            is InvalidRedirectScheme -> "Login redirect scheme key $schemeKey not found in AndroidManifest.xml."
            is FailedToSaveAuthState -> "Failed to save auth state for user $username: ${underlyingError.localizedMessage}"
        }

    override val message: String
        get() = errorDescription
}
