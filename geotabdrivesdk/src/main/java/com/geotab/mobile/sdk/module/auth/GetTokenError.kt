package com.geotab.mobile.sdk.module.auth

import net.openid.appauth.AuthorizationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Get Token Error Types
 *
 * Sealed class representing all possible errors that can occur during token retrieval and refresh operations.
 * Each error type provides a descriptive message through the [errorDescription] property.
 */
sealed class GetTokenError : Exception() {
    /**
     * No auth token found for user.
     */
    data class NoAccessTokenFoundError(val username: String) : GetTokenError()

    /**
     * Failed to unarchive auth state from storage data.
     */
    object ParseFailedForAuthState : GetTokenError() {
        private fun readResolve(): Any = ParseFailedForAuthState
    }

    /**
     * Failed to save auth state to storage.
     */
    object FailedToSaveAuthState : GetTokenError() {
        private fun readResolve(): Any = FailedToSaveAuthState
    }

    /**
     * Failed to delete auth state from storage.
     */
    object FailedToDeleteAuthState : GetTokenError() {
        private fun readResolve(): Any = FailedToDeleteAuthState
    }

    /**
     * Token refresh failed for user.
     *
     * @param username The username for which token refresh failed
     * @param underlyingError The underlying error that caused the token refresh to fail
     * @param requiresReauthentication Whether the error requires user re-authentication
     */
    data class TokenRefreshFailed(
        val username: String,
        val underlyingError: Throwable,
        val requiresReauthentication: Boolean
    ) : GetTokenError()

    /**
     * Returns a human-readable error description.
     */
    val errorDescription: String
        get() = when (this) {
            is NoAccessTokenFoundError -> "No auth token found for user $username"
            is ParseFailedForAuthState -> "Failed to unarchive auth state from storage data."
            is FailedToSaveAuthState -> "Failed to save auth state to storage."
            is FailedToDeleteAuthState -> "Failed to delete auth state from storage."
            is TokenRefreshFailed -> {
                if (requiresReauthentication) {
                    "Token refresh failed for user $username. Re-authentication required: ${underlyingError.localizedMessage}"
                } else {
                    "Token refresh failed for user $username. Please try again: ${underlyingError.localizedMessage}"
                }
            }
        }

    override val message: String
        get() = errorDescription

    companion object {
        private val NETWORK_ERROR_KEYWORDS = listOf("network", "connection", "timeout", "unreachable")
        private val OAUTH_ERROR_KEYWORDS = listOf("invalid_grant", "invalid_client", "unauthorized", "invalid_token")
        private val HTTP_STATUS_REGEX = Regex("""HTTP\s+(\d{3})""")

        /**
         * Determines if an error from token refresh is recoverable (network issue) or requires re-authentication (auth server rejection).
         *
         * Recoverable errors include:
         * - Network connectivity issues (no internet, connection lost, timeout)
         * - DNS lookup failures
         * - Cannot connect to host
         * - HTTP 5xx server errors (temporary server issues)
         *
         * Non-recoverable errors (require re-authentication) include:
         * - OAuth errors (invalid_grant, invalid_client, etc.)
         * - HTTP 4xx client errors
         * - Unknown errors (safer to require re-auth)
         *
         * @param error The error to check for recoverability
         * @return true if the error is recoverable (transient network issue), false if it requires re-authentication
         */
        fun isRecoverableError(error: Throwable): Boolean {
            // Check if this is an AuthorizationException (from AppAuth library)
            // Matches iOS behavior for OIDGeneralErrorDomain and OIDOAuthTokenErrorDomain
            if (error is AuthorizationException) {
                // Check the cause chain for network-related exceptions (IOException, SocketTimeoutException, etc.)
                val cause = error.cause
                if (cause != null && isRecoverableError(cause)) {
                    return true
                }

                // OAuth protocol errors (invalid_grant, invalid_client, etc.) are NOT recoverable
                // Matches iOS: OIDOAuthTokenErrorDomain errors are not recoverable
                if (error.type == AuthorizationException.TYPE_OAUTH_TOKEN_ERROR) {
                    return false
                }

                // General errors (including NETWORK_ERROR, SERVER_ERROR) are recoverable by default
                // UNLESS they are OAuth-specific errors
                // Matches iOS: OIDGeneralErrorDomain errors are recoverable except specific OAuth codes
                if (error.type == AuthorizationException.TYPE_GENERAL_ERROR) {
                    val errorCode = error.code

                    // Check for OAuth error codes within general errors (iOS does this too)
                    val oauthErrorCodes = setOf(
                        AuthorizationException.AuthorizationRequestErrors.INVALID_REQUEST.code,
                        AuthorizationException.TokenRequestErrors.INVALID_CLIENT.code,
                        AuthorizationException.TokenRequestErrors.INVALID_GRANT.code,
                        AuthorizationException.TokenRequestErrors.UNAUTHORIZED_CLIENT.code,
                        AuthorizationException.TokenRequestErrors.UNSUPPORTED_GRANT_TYPE.code
                    )

                    // If it's an OAuth error code, not recoverable
                    if (errorCode in oauthErrorCodes) {
                        return false
                    }

                    // All other general errors (NETWORK_ERROR, SERVER_ERROR, etc.) are recoverable
                    return true
                }

                // For other authorization exception types, check message
                val errorMsg = error.message ?: ""
                if (errorMsg.containsNetworkErrorKeywords() || errorMsg.isServerError()) {
                    return true
                }

                // OAuth-specific errors in message are not recoverable
                if (errorMsg.containsOAuthError()) {
                    return false
                }

                // Unknown authorization error - not recoverable (safer)
                return false
            }

            return when (error) {
                is UnknownHostException,
                is SocketTimeoutException -> true

                is SSLException -> false // SSL/TLS errors usually indicate configuration issues

                is IOException -> error.message.containsNetworkErrorKeywords()

                else -> {
                    val errorMessage = error.message ?: ""
                    when {
                        errorMessage.isServerError() -> true
                        errorMessage.containsOAuthError() -> false
                        else -> false // Unknown errors are considered unrecoverable - safer to require re-auth
                    }
                }
            }
        }

        private fun String?.containsNetworkErrorKeywords(): Boolean {
            val message = this?.lowercase() ?: return false
            return NETWORK_ERROR_KEYWORDS.any { message.contains(it) }
        }

        private fun String.isServerError(): Boolean {
            if (!contains("HTTP") || !contains("5")) return false

            return HTTP_STATUS_REGEX.find(this)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
                ?.let { it in 500..599 }
                ?: false
        }

        private fun String.containsOAuthError(): Boolean {
            return OAUTH_ERROR_KEYWORDS.any { contains(it) }
        }
    }
}
