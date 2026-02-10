package com.geotab.mobile.sdk.module.auth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.geotab.mobile.sdk.BuildConfig
import com.geotab.mobile.sdk.R
import com.geotab.mobile.sdk.logging.Logger

private const val TAG = "CustomBrowserScreen"

/**
 * Composable screen that hosts a WebView for OAuth authentication.
 * Implements Jetpack Compose best practices:
 * - Uses AndroidView for platform view interop
 * - Manages lifecycle with DisposableEffect
 * - Properly handles state with remember and derived state
 * - Cleans up resources on disposal
 *
 * The WebView can be configured for ephemeral browsing:
 * - When ephemeralSession is true: Clears cookies before and after use
 * - Always clears cache and history on disposal
 * - Disables storage APIs
 *
 * @param authorizationUrl The OAuth authorization URL to load
 * @param redirectUri The redirect URI to watch for callback
 * @param ephemeralSession Whether to clear cookies for ephemeral browsing
 * @param onAuthorizationResponse Callback when authorization response is received
 * @param onError Callback when an error occurs
 * @param onCancel Callback when user cancels the flow
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBrowserScreen(
    authorizationUrl: String,
    redirectUri: String,
    ephemeralSession: Boolean = false,
    onAuthorizationResponse: (Uri) -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableFloatStateOf(0f) }

    // Clean cookies before starting (to ensure clean state) - only if ephemeral session
    DisposableEffect(Unit) {
        if (ephemeralSession) {
            clearCookies()
        }
        onDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Authentication") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.custombrowser_close_description)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress indicator
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { loadProgress },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // WebView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                CustomWebView(
                    url = authorizationUrl,
                    redirectUri = redirectUri,
                    ephemeralSession = ephemeralSession,
                    onPageStarted = {
                        isLoading = true
                    },
                    onPageFinished = {
                        isLoading = false
                        loadProgress = 1f
                    },
                    onProgressChanged = { progress ->
                        loadProgress = progress / 100f
                    },
                    onRedirectDetected = { responseUri ->
                        onAuthorizationResponse(responseUri)
                    },
                    onError = { errorMessage ->
                        // Pass error to activity - CustomBrowserActivity.finishWithAuthError() will log it
                        onError(errorMessage)
                    }
                )
            }
        }
    }
}

/**
 * Composable that wraps a WebView with proper lifecycle management.
 * Follows Compose best practices for AndroidView integration.
 *
 * @param url The URL to load
 * @param redirectUri The redirect URI to intercept
 * @param ephemeralSession Whether to clear cookies on cleanup
 * @param onPageStarted Callback when page starts loading
 * @param onPageFinished Callback when page finishes loading
 * @param onProgressChanged Callback for loading progress updates
 * @param onRedirectDetected Callback when redirect URI is detected
 * @param onError Callback when an error occurs
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CustomWebView(
    url: String,
    redirectUri: String,
    ephemeralSession: Boolean,
    onPageStarted: () -> Unit,
    onPageFinished: () -> Unit,
    onProgressChanged: (Int) -> Unit,
    onRedirectDetected: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    val redirectHost = remember(redirectUri) {
        redirectUri.toUri().host
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                configureWebView(this)

                webViewClient = createWebViewClient(
                    redirectHost = redirectHost,
                    onPageStarted = onPageStarted,
                    onPageFinished = onPageFinished,
                    onRedirectDetected = onRedirectDetected,
                    onError = onError
                )

                webChromeClient = createWebChromeClient(onProgressChanged)

                // Load the authorization URL
                loadUrl(url)
            }
        },
        update = {
            // Update is called when composable recomposes
            // We don't need to reload the URL on recomposition
        },
        onRelease = { webView ->
            // Clean up WebView resources following best practices
            cleanupWebView(webView, ephemeralSession)
        }
    )
}

/**
 * Configures WebView settings to mimic incognito browsing.
 * Disables storage and cache to prevent data persistence.
 */
@SuppressLint("SetJavaScriptEnabled")
private fun configureWebView(webView: WebView) {
    if (BuildConfig.DEBUG) {
        WebView.setWebContentsDebuggingEnabled(true)
    }

    webView.settings.apply {
        // Enable JavaScript (required for OAuth flows)
        javaScriptEnabled = true

        // Disable storage to mimic incognito mode
        domStorageEnabled = false

        // Set cache mode to no cache
        cacheMode = WebSettings.LOAD_NO_CACHE

        // Disable saving passwords and form data
        @Suppress("DEPRECATION")
        savePassword = false
        @Suppress("DEPRECATION")
        saveFormData = false

        // Don't support multiple windows (security)
        setSupportMultipleWindows(false)

        // Enable safe browsing (API 26+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            safeBrowsingEnabled = true
        }

        // Set user agent (standard browser UA, not WebView)
        userAgentString = userAgentString?.replace("; wv", "") ?: userAgentString
    }
}

/**
 * Creates a WebViewClient that intercepts navigation and detects the OAuth redirect.
 */
private fun createWebViewClient(
    redirectHost: String?,
    onPageStarted: () -> Unit,
    onPageFinished: () -> Unit,
    onRedirectDetected: (Uri) -> Unit,
    onError: (String) -> Unit
): WebViewClient {
    return object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            onPageStarted()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onPageFinished()
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url

            // Check if this is the redirect URI
            if (url != null && url.host == redirectHost) {
                onRedirectDetected(url)
                return true // Prevent loading the redirect URI in WebView
            }

            return false // Allow normal navigation
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)

            // Only report errors for the main frame
            if (request?.isForMainFrame == true) {
                val errorMessage = error?.description?.toString()
                    ?: "Unknown error occurred"
                onError(errorMessage)
            }
        }
    }
}

/**
 * Creates a WebChromeClient to track loading progress.
 */
private fun createWebChromeClient(
    onProgressChanged: (Int) -> Unit
): WebChromeClient {
    return object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            onProgressChanged(newProgress)
        }
    }
}

/**
 * Cleans up WebView resources and clears data based on session type.
 * For ephemeral sessions, ensures no OAuth data persists after the flow completes.
 *
 * Uses try-catch to handle potential race conditions during rapid activity transitions.
 *
 * @param webView The WebView to clean up
 * @param ephemeralSession Whether to clear cookies (for ephemeral browsing)
 */
private fun cleanupWebView(webView: WebView, ephemeralSession: Boolean) {
    try {
        // Stop loading any pending requests
        webView.stopLoading()

        // Clear WebView data
        webView.clearHistory()
        webView.clearCache(true)
        webView.clearFormData()

        // Clear all cookies only if ephemeral session
        if (ephemeralSession) {
            clearCookies()
        }

        // Remove all views and destroy
        webView.removeAllViews()
        webView.destroy()
    } catch (e: Exception) {
        // Log but don't crash - cleanup is best-effort
        // Errors can occur during rapid activity transitions or WebView state changes
        Logger.shared.error("$TAG.cleanupWebView", "Error during WebView cleanup: ${e.message}", e)
    }
}

/**
 * Clears all cookies from the CookieManager.
 * This helps maintain privacy and mimics incognito browsing.
 */
private fun clearCookies() {
    val cookieManager = CookieManager.getInstance()
    cookieManager.removeAllCookies(null)
    cookieManager.flush()
}
