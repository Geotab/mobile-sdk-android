package com.geotab.mobile.sdk.util
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.logging.InternalAppLogging
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.login.GeotabAuthState
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

class AuthUtil {
    private var loginCallback: ((Result<Success<String>, Failure>) -> Unit)? = null
    var authService: AuthorizationService? = null

    fun activityResultLauncherFunction(
        fragmentForResult: Fragment,
        tag: String
    ): ActivityResultLauncher<Intent> {
        return fragmentForResult.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            authService?.let {
                exchangeToken(
                    data = result.data,
                    tag = tag
                )
            }

            authService?.dispose()
        }
    }

    fun login(
        clientId: String,
        discoveryUri: Uri,
        loginHint: String,
        redirectScheme: Uri,
        loginCallback: ((Result<Success<String>, Failure>) -> Unit)?,
        tag: String,
        authResultLauncher: ActivityResultLauncher<Intent>
    ) {
        this.loginCallback = loginCallback
        AuthorizationServiceConfiguration.fetchFromUrl(discoveryUri) { serviceConfiguration, fetchException ->
            if (fetchException != null) {
                sendErrorMessage(
                    errorMessage = fetchException.message ?: "Failed to fetch configuration",
                    callback = loginCallback,
                    tag = tag
                )
                return@fetchFromUrl
            }

            serviceConfiguration?.let { sConfiguration ->
                val authRequest = AuthorizationRequest.Builder(
                    sConfiguration,
                    clientId,
                    ResponseTypeValues.CODE,
                    redirectScheme
                )
                    .setScope("openid profile email")

                if (loginHint.isNotEmpty()) {
                    authRequest.setLoginHint(loginHint)
                }

                val authIntent = authService?.getAuthorizationRequestIntent(authRequest.build())

                authResultLauncher.launch(authIntent)
            }
        }
    }

    private fun exchangeToken(
        data: Intent?,
        tag: String
    ) {
        if (data != null) {
            val response = AuthorizationResponse.fromIntent(data)
            val exception = AuthorizationException.fromIntent(data)

            if (response != null) {
                val tokenRequest = response.createTokenExchangeRequest()

                authService?.performTokenRequest(tokenRequest) { tokenResponse, tokenException ->
                    if (tokenResponse != null) {
                        val authState = AuthState(response, tokenResponse, null)
                        authState.update(tokenResponse, null)

                        val geotabAuthState = GeotabAuthState(
                            accessToken = tokenResponse.accessToken ?: "",
                            idToken = tokenResponse.idToken ?: "",
                            refreshToken = tokenResponse.refreshToken ?: ""
                        )

                        val geotabAuthStateJson = JsonUtil.toJson(geotabAuthState)
                        loginCallback?.let {
                            it(Success(geotabAuthStateJson))
                        }
                    } else {
                        sendErrorMessage(
                            errorMessage = tokenException?.message ?: "Token exchange failed",
                            callback = loginCallback,
                            tag = tag
                        )
                    }
                }
            } else if (exception != null) {
                sendErrorMessage(
                    errorMessage = exception.message ?: "Authorization failed",
                    callback = loginCallback,
                    tag = tag
                )
            } else {
                sendErrorMessage(
                    errorMessage = "No data returned from authorization flow",
                    callback = loginCallback,
                    tag = tag
                )
            }
        } else {
            sendErrorMessage(
                errorMessage = "Activity result was null",
                callback = loginCallback,
                tag = tag
            )
        }
    }

    private fun sendErrorMessage(
        errorMessage: String,
        callback: ((Result<Success<String>, Failure>) -> Unit)?,
        tag: String
    ) {
        InternalAppLogging.appLogger?.error(
            tag,
            errorMessage
        )
        callback?.let {
            it(Failure(Error(GeotabDriveError.LOGIN_FAILED_ERROR, errorMessage)))
        }
    }
}
