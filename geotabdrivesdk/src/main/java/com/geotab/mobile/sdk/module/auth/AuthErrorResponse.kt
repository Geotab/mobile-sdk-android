package com.geotab.mobile.sdk.module.auth

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Structured JSON error response for authentication errors.
 * Provides web developers with programmatic access to error details.
 *
 * @property code Error code for programmatic handling (e.g., "TOKEN_REFRESH_FAILED")
 * @property message Human-readable error message
 * @property recoverable Whether this error is recoverable (can retry)
 * @property requiresReauthentication Whether this error requires user re-authentication (optional, only for token refresh)
 * @property username Username associated with the error (optional)
 * @property underlyingError Details about the underlying error cause (optional)
 */
@Keep
data class AuthErrorResponse(
    @SerializedName("code")
    val code: String,

    @SerializedName("message")
    val message: String,

    @SerializedName("recoverable")
    val recoverable: Boolean,

    @SerializedName("requiresReauthentication")
    val requiresReauthentication: Boolean? = null,

    @SerializedName("username")
    val username: String? = null,

    @SerializedName("underlyingError")
    val underlyingError: String? = null
) {
    companion object {
        /**
         * Creates an AuthErrorResponse from an AuthError.
         *
         * @param authError The AuthError to convert to JSON response
         * @return AuthErrorResponse with all relevant error details
         */
        fun fromAuthError(authError: AuthError): AuthErrorResponse {
            return when (authError) {
                is AuthError.TokenRefreshFailed -> AuthErrorResponse(
                    code = authError.errorCode,
                    message = authError.fallbackErrorMessage,
                    recoverable = authError.isRecoverable,
                    requiresReauthentication = authError.requiresReauthentication,
                    username = authError.username,
                    underlyingError = authError.underlyingError.localizedMessage
                )

                is AuthError.FailedToSaveAuthState -> AuthErrorResponse(
                    code = authError.errorCode,
                    message = authError.fallbackErrorMessage,
                    recoverable = authError.isRecoverable,
                    requiresReauthentication = null,
                    username = authError.username,
                    underlyingError = authError.underlyingError.localizedMessage
                )

                is AuthError.UsernameMismatch -> AuthErrorResponse(
                    code = authError.errorCode,
                    message = authError.fallbackErrorMessage,
                    recoverable = authError.isRecoverable,
                    requiresReauthentication = null,
                    username = authError.expected,
                    underlyingError = "Actual username: ${authError.actual}"
                )

                is AuthError.NoAccessTokenFoundError -> AuthErrorResponse(
                    code = authError.errorCode,
                    message = authError.fallbackErrorMessage,
                    recoverable = authError.isRecoverable,
                    requiresReauthentication = null,
                    username = authError.username,
                    underlyingError = null
                )

                is AuthError.InvalidRedirectScheme -> AuthErrorResponse(
                    code = authError.errorCode,
                    message = authError.fallbackErrorMessage,
                    recoverable = authError.isRecoverable,
                    requiresReauthentication = null,
                    username = null,
                    underlyingError = "Missing key: ${authError.schemeKey}"
                )

                is AuthError.NetworkError -> AuthErrorResponse(
                    code = authError.errorCode,
                    message = authError.fallbackErrorMessage,
                    recoverable = authError.isRecoverable,
                    requiresReauthentication = null,
                    username = null,
                    underlyingError = authError.underlyingError.localizedMessage
                )

                is AuthError.UnexpectedResponse -> AuthErrorResponse(
                    code = authError.errorCode,
                    message = authError.fallbackErrorMessage,
                    recoverable = authError.isRecoverable,
                    requiresReauthentication = null,
                    username = null,
                    underlyingError = "HTTP ${authError.statusCode}"
                )

                is AuthError.RevokeTokenFailed -> AuthErrorResponse(
                    code = authError.errorCode,
                    message = authError.fallbackErrorMessage,
                    recoverable = authError.isRecoverable,
                    requiresReauthentication = null,
                    username = null,
                    underlyingError = authError.underlyingError.localizedMessage
                )

                is AuthError.UnexpectedError -> AuthErrorResponse(
                    code = authError.errorCode,
                    message = authError.fallbackErrorMessage,
                    recoverable = authError.isRecoverable,
                    requiresReauthentication = null,
                    username = null,
                    underlyingError = authError.underlyingError?.localizedMessage
                )

                // All other error types don't have associated data
                else -> AuthErrorResponse(
                    code = authError.errorCode,
                    message = authError.fallbackErrorMessage,
                    recoverable = authError.isRecoverable,
                    requiresReauthentication = null,
                    username = null,
                    underlyingError = null
                )
            }
        }
    }
}
