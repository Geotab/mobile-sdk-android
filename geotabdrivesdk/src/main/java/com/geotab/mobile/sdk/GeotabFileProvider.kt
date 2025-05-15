package com.geotab.mobile.sdk

import androidx.core.content.FileProvider

/**
 * Provides a secure way to share files from the Geotab application with other apps.
 *
 * This class extends [FileProvider] and is configured in the application's manifest
 * to specify the directories and file types that can be shared.
 *
 * Using [FileProvider] is the recommended way to share files securely in Android
 * by creating content URIs instead of directly exposing file system paths.
 */
class GeotabFileProvider : FileProvider()
