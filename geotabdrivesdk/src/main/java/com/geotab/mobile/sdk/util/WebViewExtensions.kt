package com.geotab.mobile.sdk.util

import android.webkit.WebView
import org.json.JSONObject

/**
 * Safely sets the WebView's location hash by navigating to the given path.
 * The path is properly escaped to prevent JavaScript injection attacks.
 *
 * @param path The path to navigate to (e.g., "hosLogs", "messages", "a/b/c")
 */
fun WebView.setLocationHash(path: String) {
    // JSONObject.quote() returns a properly escaped JavaScript string literal with surrounding quotes
    // Example: hosLogs -> "hosLogs"
    // Example: ";alert(1);// -> "\";alert(1);//"
    val escapedPath = JSONObject.quote(path)
    evaluateJavascript("window.location.hash=$escapedPath;", null)
}
