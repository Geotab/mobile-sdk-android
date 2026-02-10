package com.geotab.mobile.sdk.module.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.Keep
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.geotab.mobile.sdk.logging.Logger
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse

/**
 * Custom browser activity using Jetpack Compose for OAuth authentication flow.
 * This activity serves as a fallback when Chrome Custom Tabs don't support incognito mode.
 *
 * The activity hosts a WebView that handles the OAuth authorization flow and captures
 * the redirect URI with the authorization code.
 *
 * Following OAuth 2.0 best practices for native apps (RFC 8252), this implementation:
 * - Clears cookies and cache after use to mimic incognito behavior
 * - Handles authorization responses via deep links
 * - Returns results to the calling activity via Activity Result API
 *
 * Note: Using a custom WebView for OAuth is not recommended by OAuth best practices
 * but is necessary when the system doesn't support ephemeral browsing sessions.
 */
@Keep
class CustomBrowserActivity : ComponentActivity() {

    private var authorizationUrl: String? = null
    private var redirectUri: String? = null
    private var authorizationRequest: AuthorizationRequest? = null
    private var ephemeralSession: Boolean = false

    companion object {
        private const val TAG = "CustomBrowserActivity"
        private const val EXTRA_AUTHORIZATION_URL = "authorization_url"
        private const val EXTRA_REDIRECT_URI = "redirect_uri"
        private const val EXTRA_AUTHORIZATION_REQUEST = "authorizationRequest"
        private const val EXTRA_EPHEMERAL_SESSION = "ephemeral_session"
        const val EXTRA_AUTH_ERROR = "auth_error"

        /**
         * Creates an intent to launch the CustomBrowserActivity.
         *
         * @param context The context to use for creating the intent
         * @param authorizationRequest The AppAuth authorization request
         * @param ephemeralSession Whether to use ephemeral browsing (clear cookies)
         * @return Intent configured for launching CustomBrowserActivity
         */
        @Keep
        fun createIntent(
            context: Context,
            authorizationRequest: AuthorizationRequest,
            ephemeralSession: Boolean = false
        ): Intent {
            val authorizationUrl = authorizationRequest.toUri().toString()
            val redirectUri = authorizationRequest.redirectUri.toString()

            return Intent(context, CustomBrowserActivity::class.java).apply {
                putExtra(EXTRA_AUTHORIZATION_URL, authorizationUrl)
                putExtra(EXTRA_REDIRECT_URI, redirectUri)
                putExtra(EXTRA_AUTHORIZATION_REQUEST, authorizationRequest.jsonSerializeString())
                putExtra(EXTRA_EPHEMERAL_SESSION, ephemeralSession)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract parameters from intent
        authorizationUrl = intent.getStringExtra(EXTRA_AUTHORIZATION_URL)
        redirectUri = intent.getStringExtra(EXTRA_REDIRECT_URI)
        ephemeralSession = intent.getBooleanExtra(EXTRA_EPHEMERAL_SESSION, false)

        // Validate and extract authorization URL
        val validatedAuthUrl = validateParameter(
            authorizationUrl,
            "Missing authorization URL for custom browser"
        ) ?: return

        // Validate and extract redirect URI
        val validatedRedirectUri = validateParameter(
            redirectUri,
            "Missing redirect URI for custom browser"
        ) ?: return

        // Deserialize and validate the authorization request
        val authRequestJson = intent.getStringExtra(EXTRA_AUTHORIZATION_REQUEST)
        authorizationRequest = authRequestJson?.let {
            try {
                AuthorizationRequest.jsonDeserialize(it)
            } catch (e: Exception) {
                finishWithAuthError(
                    AuthError.UnexpectedError(
                        description = "Failed to deserialize authorization request in custom browser",
                        underlyingError = e
                    )
                )
                return
            }
        } ?: run {
            finishWithAuthError(
                AuthError.UnexpectedError(
                    description = "Missing authorization request for custom browser",
                    underlyingError = null
                )
            )
            return
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CustomBrowserScreen(
                        authorizationUrl = validatedAuthUrl,
                        redirectUri = validatedRedirectUri,
                        ephemeralSession = ephemeralSession,
                        onAuthorizationResponse = { responseUri ->
                            handleAuthorizationResponse(responseUri)
                        },
                        onError = { errorMessage ->
                            finishWithAuthError(
                                AuthError.UnexpectedError(
                                    description = "WebView error in custom browser: $errorMessage",
                                    underlyingError = null
                                )
                            )
                        },
                        onCancel = {
                            handleCancel()
                        }
                    )
                }
            }
        }
    }

    /**
     * Validates a parameter and finishes the activity with an error if validation fails.
     *
     * @param value The value to validate
     * @param errorDescription Description of the error if validation fails
     * @return The value if not null, null otherwise (activity will finish)
     */
    private fun <T> validateParameter(
        value: T?,
        errorDescription: String
    ): T? {
        return value ?: run {
            finishWithAuthError(
                AuthError.UnexpectedError(
                    description = errorDescription,
                    underlyingError = null
                )
            )
            null
        }
    }

    /**
     * Handles the authorization response when the redirect URI is intercepted.
     * Creates a properly formatted AppAuth response intent by manually building
     * the AuthorizationResponse from the redirect URI and original request.
     */
    private fun handleAuthorizationResponse(responseUri: Uri) {
        val authRequest = authorizationRequest
        if (authRequest == null) {
            finishWithAuthError(
                AuthError.UnexpectedError(
                    description = "Missing authorization request in custom browser",
                    underlyingError = null
                )
            )
            return
        }

        try {
            // Create AuthorizationResponse by combining the redirect URI with the original request
            val authResponse = AuthorizationResponse.Builder(authRequest)
                .fromUri(responseUri)
                .build()

            // Build result intent with AppAuth response
            val resultIntent = authResponse.toIntent()

            setResult(RESULT_OK, resultIntent)
            finish()
        } catch (e: Exception) {
            finishWithAuthError(
                AuthError.UnexpectedError(
                    description = "Failed to process authorization response in custom browser",
                    underlyingError = e
                )
            )
        }
    }

    /**
     * Handles errors during the authorization flow.
     * Logs the error locally for debugging and passes the AuthError directly to AuthUtil.
     * AuthUtil.handleAuthorizationResponse() will extract the AuthError and handle logging to Sentry.
     */
    private fun finishWithAuthError(authError: AuthError) {
        // Log locally for debugging the custom browser component
        Logger.shared.error("$TAG.finishWithAuthError", authError.fallbackErrorMessage)

        // Pass the AuthError directly to AuthUtil via intent extra
        // AuthError is Serializable (extends Exception), so it can be passed via Intent
        val resultIntent = Intent().apply {
            putExtra(EXTRA_AUTH_ERROR, authError as java.io.Serializable)
        }

        setResult(RESULT_CANCELED, resultIntent)
        finish()
    }

    /**
     * Handles user cancellation of the authorization flow.
     */
    private fun handleCancel() {
        // Use the exact same message from AuthError.UserCancelledFlow
        Logger.shared.debug("$TAG.handleCancel", AuthError.UserCancelledFlow.fallbackErrorMessage)
        setResult(RESULT_CANCELED)
        finish()
    }
}
