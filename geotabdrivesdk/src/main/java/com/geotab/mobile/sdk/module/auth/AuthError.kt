package com.geotab.mobile.sdk.module.auth

import com.geotab.mobile.sdk.util.JsonUtil
import net.openid.appauth.AuthorizationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Auth Module Error Types
 *
 * Sealed class representing all possible errors that can occur during authentication operations.
 * Each error type provides:
 * - [errorDescription]: Structured JSON error with code, message, recoverable flag, and metadata
 * - [fallbackErrorMessage]: Human-readable error message (fallback if JSON serialization fails)
 * - [errorCode]: Programmatic error code for web developers (e.g., "TOKEN_REFRESH_FAILED")
 * - [isRecoverable]: Whether this error is recoverable (can retry)
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
     * Failed parsing Json or auth state.
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
     * Missing required authentication data.
     */
    object MissingAuthData : AuthError() {
        private fun readResolve(): Any = MissingAuthData
    }

    /**
     * No external user agent available.
     */
    object NoExternalUserAgent : AuthError() {
        private fun readResolve(): Any = NoExternalUserAgent
    }

    /**
     * User cancelled the authentication flow.
     */
    object UserCancelledFlow : AuthError() {
        private fun readResolve(): Any = UserCancelledFlow
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
     * Username mismatch between expected and actual username in access token.
     */
    data class UsernameMismatch(
        val expected: String,
        val actual: String
    ) : AuthError()

    /**
     * No auth token found for user.
     */
    data class NoAccessTokenFoundError(val username: String) : AuthError()

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
    ) : AuthError()

    /**
     * Token revocation failed.
     */
    data class RevokeTokenFailed(val underlyingError: Throwable) : AuthError()

    /**
     * Network error occurred during authentication.
     */
    data class NetworkError(val underlyingError: Throwable) : AuthError()

    /**
     * Unexpected response from server.
     */
    data class UnexpectedResponse(val statusCode: Int) : AuthError()

    /**
     * Unexpected error occurred.
     * Used for exceptions that don't fit into other AuthError categories.
     *
     * @param description Human-readable description of what operation failed
     * @param underlyingError The underlying exception that caused the error
     */
    data class UnexpectedError(
        val description: String,
        val underlyingError: Throwable?
    ) : AuthError()

    /**
     * Returns the error code for programmatic error handling.
     * Examples: "TOKEN_REFRESH_FAILED", "USERNAME_MISMATCH", "USER_CANCELLED"
     */
    val errorCode: String
        get() = when (this) {
            is SessionParseFailedError -> "SESSION_PARSE_FAILED"
            is SessionRetrieveFailedError -> "SESSION_RETRIEVE_FAILED"
            is ParseFailedError -> "PARSE_FAILED_FOR_AUTH_STATE"
            is NoDataFoundError -> "NO_DATA_FOUND"
            is InvalidURL -> "INVALID_URL"
            is MissingAuthData -> "MISSING_AUTH_DATA"
            is NoExternalUserAgent -> "NO_EXTERNAL_USER_AGENT"
            is UserCancelledFlow -> "USER_CANCELLED"
            is InvalidRedirectScheme -> "INVALID_REDIRECT_SCHEME"
            is FailedToSaveAuthState -> "FAILED_TO_SAVE_AUTH_STATE"
            is UsernameMismatch -> "USERNAME_MISMATCH"
            is NoAccessTokenFoundError -> "NO_ACCESS_TOKEN_FOUND"
            is TokenRefreshFailed -> {
                if (requiresReauthentication) "TOKEN_REFRESH_REAUTH_REQUIRED" else "TOKEN_REFRESH_FAILED"
            }
            is RevokeTokenFailed -> "REVOKE_TOKEN_FAILED"
            is NetworkError -> "NETWORK_ERROR"
            is UnexpectedResponse -> "UNEXPECTED_RESPONSE"
            is UnexpectedError -> "UNEXPECTED_ERROR"
        }

    /**
     * Returns whether this error is recoverable (can retry).
     * Recoverable errors include network issues, server errors, and user cancellations.
     * Non-recoverable errors require user intervention (re-authentication, configuration fixes, etc.)
     */
    val isRecoverable: Boolean
        get() = when (this) {
            is TokenRefreshFailed -> isRecoverableError(underlyingError)
            is NetworkError -> isRecoverableError(underlyingError)
            is UnexpectedResponse -> statusCode in 500..599
            is UserCancelledFlow -> true
            is UnexpectedError -> underlyingError?.let { isRecoverableError(it) } ?: false
            else -> false
        }

    /**
     * Returns a human-readable error message (fallback if JSON serialization fails).
     * This is the plain text error message without JSON structure.
     */
    val fallbackErrorMessage: String
        get() = when (this) {
            is SessionParseFailedError -> "Failed parsing session."
            is SessionRetrieveFailedError -> "Failed retrieving session."
            is ParseFailedError -> "Failed parsing Json or auth state."
            is NoDataFoundError -> "No data returned from authorization flow."
            is InvalidURL -> "Insecure Discovery URI. HTTPS is required."
            is MissingAuthData -> "Missing required authentication data."
            is NoExternalUserAgent -> "No external user agent available."
            is UserCancelledFlow -> "User cancelled the authentication flow."
            is InvalidRedirectScheme -> "Login redirect scheme key $schemeKey not found in AndroidManifest.xml."
            is FailedToSaveAuthState -> "Failed to save auth state for user $username: ${underlyingError.localizedMessage}"
            is UsernameMismatch -> "Username mismatch: expected '$expected' but access token contains '$actual'"
            is NoAccessTokenFoundError -> "No auth token found for user $username"
            is TokenRefreshFailed -> {
                if (requiresReauthentication) {
                    "Token refresh failed for user $username. Re-authentication required: ${underlyingError.localizedMessage}"
                } else {
                    "Token refresh failed for user $username. Please try again: ${underlyingError.localizedMessage}"
                }
            }
            is RevokeTokenFailed -> "Token revocation failed: ${underlyingError.localizedMessage}"
            is NetworkError -> "Network error: ${underlyingError.localizedMessage}"
            is UnexpectedResponse -> "Unexpected response from server: HTTP $statusCode"
            is UnexpectedError -> {
                if (underlyingError != null) {
                    "An unexpected authentication error occurred: $description. ${underlyingError.localizedMessage}"
                } else {
                    "An unexpected authentication error occurred: $description"
                }
            }
        }

    /**
     * Returns a structured JSON error description for web developers.
     * Falls back to [fallbackErrorMessage] if JSON serialization fails.
     *
     * The JSON object contains:
     * - code: Error code for programmatic handling
     * - message: Human-readable error message
     * - recoverable: Whether error is recoverable (can retry)
     * - requiresReauthentication: Whether re-authentication is needed (optional)
     * - username: Associated username (optional)
     * - underlyingError: Details about underlying cause (optional)
     */
    val errorDescription: String
        get() {
            return try {
                val response = AuthErrorResponse.fromAuthError(this)
                JsonUtil.toJson(response)
            } catch (e: Exception) {
                // Fallback to plain text error if JSON serialization fails
                fallbackErrorMessage
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
            if (error is AuthorizationException) {
                // Check the cause chain for network-related exceptions (IOException, SocketTimeoutException, etc.)
                val cause = error.cause
                if (cause != null && isRecoverableError(cause)) {
                    return true
                }

                // OAuth protocol errors (invalid_grant, invalid_client, etc.) are NOT recoverable
                // EXCEPT for server errors like temporarily_unavailable
                if (error.type == AuthorizationException.TYPE_OAUTH_TOKEN_ERROR) {
                    val errorCode = error.code

                    // Code 2007 is temporarily_unavailable - this is a server error, not auth rejection
                    // Should be treated as recoverable (server will hopefully recover)
                    if (errorCode == 2007) {
                        return true
                    }

                    // All other OAuth token errors are not recoverable
                    return false
                }

                // General errors (including NETWORK_ERROR, SERVER_ERROR) are recoverable by default
                // UNLESS they are OAuth-specific errors
                if (error.type == AuthorizationException.TYPE_GENERAL_ERROR) {
                    val errorCode = error.code

                    // Check for OAuth error codes within general errors
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

                    // Code 5 is JSON deserialization error - treat as recoverable (likely server error)
                    // This happens when server returns non-JSON response (e.g., plain text 503)
                    if (errorCode == 5) {
                        return true
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
