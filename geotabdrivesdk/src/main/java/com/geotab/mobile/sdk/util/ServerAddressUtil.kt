package com.geotab.mobile.sdk.util

/**
 * Utility class for parsing and manipulating server addresses.
 */
class ServerAddressUtil {
    companion object {
        /**
         * Extracts the hostname from a server address that may include a path.
         *
         * This function assumes the input is already validated by:
         * - UpdateLastServerFunction regex validation, or
         * - WebView URI parser (which extracts only the hostname)
         *
         * Examples:
         * - "my.geotab.com" → "my.geotab.com"
         * - "my.geotab.com/database" → "my.geotab.com"
         * - "my4.geotab.com/database/extra" → "my4.geotab.com"
         *
         * @param serverAddress The server address which may include a path component
         * @return The hostname without any path component
         */
        fun extractHostname(serverAddress: String): String {
            val trimmed = serverAddress.trim()
            val slashIndex = trimmed.indexOf('/')
            return if (slashIndex > 0) {
                trimmed.substring(0, slashIndex)
            } else {
                trimmed
            }
        }
    }
}
