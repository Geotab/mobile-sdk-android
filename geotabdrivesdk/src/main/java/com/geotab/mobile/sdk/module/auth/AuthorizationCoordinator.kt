package com.geotab.mobile.sdk.module.auth

import com.geotab.mobile.sdk.logging.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * Coordinator to prevent concurrent authentication operations.
 *
 * This class ensures that:
 * - Only one login/reauth/logout operation per user runs at a time
 * - Duplicate requests share the same result instead of creating new operations
 * - UI operations (login, reauth, logout) are serialized to prevent UI conflicts
 *
 * Thread-safety is provided by Mutex for dictionary access and single-threaded
 * execution via AuthUtil's authScope.
 */
class AuthorizationCoordinator(
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS
) {
    private val mutex = Mutex()

    // Track in-flight operations per username
    private val inFlightLogins = mutableMapOf<String, CompletableDeferred<AuthToken>>()
    private val inFlightReauths = mutableMapOf<String, CompletableDeferred<AuthToken>>()
    private val inFlightTokenRefresh = mutableMapOf<String, CompletableDeferred<AuthToken?>>()
    private val inFlightLogouts = mutableMapOf<String, CompletableDeferred<Unit>>()

    // Track any UI-presenting operation to serialize them
    private var currentUIOperation: CompletableDeferred<*>? = null

    companion object {
        private const val TAG = "AuthorizationCoordinator"
        internal const val DEFAULT_TIMEOUT_MS = 120_000L // 2 minutes
    }

    /**
     * Perform login operation, deduplicating concurrent requests for the same user.
     *
     * If a login is already in progress for this user, returns the existing operation's result
     * instead of starting a new one.
     */
    suspend fun performLogin(
        username: String,
        operation: suspend () -> AuthToken
    ): AuthToken = executeUIOperation(inFlightLogins, username, "Login", operation)

    /**
     * Perform re-authentication operation, deduplicating concurrent requests for the same user.
     */
    suspend fun performReauth(
        username: String,
        operation: suspend () -> AuthToken
    ): AuthToken = executeUIOperation(inFlightReauths, username, "Re-authentication", operation)

    /**
     * Perform token refresh operation, deduplicating concurrent requests.
     *
     * Note: Token refresh does NOT wait for UI operations, allowing
     * background token refreshes while user is in a login flow.
     *
     * @param username The username to refresh token for
     * @param forceRefresh Whether this is a forced refresh
     * @param operation The refresh operation to perform
     */
    suspend fun performTokenRefresh(
        username: String,
        forceRefresh: Boolean,
        operation: suspend () -> AuthToken?
    ): AuthToken? {
        val key = "${username}_$forceRefresh"

        // Check for existing token refresh
        val existingRefresh = mutex.withLock { inFlightTokenRefresh[key] }
        if (existingRefresh != null) {
            Logger.shared.debug(TAG, "Token refresh already in progress for $username (force=$forceRefresh), sharing result")
            try {
                return withTimeout(timeoutMs) { existingRefresh.await() }
            } catch (e: TimeoutCancellationException) {
                Logger.shared.warn(TAG, "Timed out waiting for in-flight token refresh for $username, cleaning up stale operation")
                cleanupStaleOperation(existingRefresh, inFlightTokenRefresh, key)
            }
        }

        Logger.shared.debug(TAG, "Starting new token refresh for $username (force=$forceRefresh)")

        val deferred = CompletableDeferred<AuthToken?>()
        mutex.withLock {
            inFlightTokenRefresh[key] = deferred
        }

        return try {
            val result = operation()
            deferred.complete(result)
            result
        } catch (e: Exception) {
            deferred.completeExceptionally(e)
            throw e
        } finally {
            mutex.withLock {
                inFlightTokenRefresh.remove(key)
            }
        }
    }

    /**
     * Perform logout operation, deduplicating concurrent requests for the same user.
     */
    suspend fun performLogout(
        username: String,
        operation: suspend () -> Unit
    ) = executeUIOperation(inFlightLogouts, username, "Logout", operation)

    /**
     * Executes a UI operation with deduplication and serialization.
     *
     * - If an identical operation is already in-flight, awaits its result (with timeout).
     * - Waits for any other active UI operation to finish before starting.
     * - Registers itself as the current UI operation so others wait for it.
     */
    private suspend fun <T> executeUIOperation(
        operationMap: MutableMap<String, CompletableDeferred<T>>,
        key: String,
        operationName: String,
        operation: suspend () -> T
    ): T {
        val existing = mutex.withLock { operationMap[key] }
        if (existing != null) {
            Logger.shared.debug(TAG, "$operationName already in progress for $key, sharing result")
            try {
                return withTimeout(timeoutMs) { existing.await() }
            } catch (e: TimeoutCancellationException) {
                Logger.shared.warn(TAG, "Timed out waiting for in-flight $operationName for $key, cleaning up stale operation")
                cleanupStaleOperation(existing, operationMap, key)
            }
        }

        waitForUIOperation()

        Logger.shared.debug(TAG, "Starting new $operationName for $key")

        val deferred = CompletableDeferred<T>()
        mutex.withLock {
            operationMap[key] = deferred
            currentUIOperation = deferred
        }

        return try {
            val result = operation()
            deferred.complete(result)
            result
        } catch (e: Exception) {
            deferred.completeExceptionally(e)
            throw e
        } finally {
            mutex.withLock {
                operationMap.remove(key)
                if (currentUIOperation == deferred) {
                    currentUIOperation = null
                }
            }
        }
    }

    /**
     * Clean up a stale in-flight operation that has timed out.
     * Removes it from the tracking map and clears currentUIOperation if it matches.
     */
    private suspend fun <T> cleanupStaleOperation(
        staleOperation: CompletableDeferred<T>,
        operationMap: MutableMap<String, CompletableDeferred<T>>,
        key: String
    ) {
        mutex.withLock {
            if (operationMap[key] == staleOperation) {
                operationMap.remove(key)
            }
            if (currentUIOperation == staleOperation) {
                currentUIOperation = null
            }
        }
    }

    /**
     * Wait for any existing UI operation to complete before starting a new one.
     * This prevents multiple UI-presenting operations (login, reauth, logout) from
     * running simultaneously.
     *
     * Uses a loop to re-check after each wait, preventing a TOCTOU race where
     * multiple coroutines waiting on the same operation all proceed simultaneously
     * when it completes.
     */
    private suspend fun waitForUIOperation() {
        while (true) {
            val operation = mutex.withLock { currentUIOperation } ?: return
            Logger.shared.debug(TAG, "Waiting for existing UI operation to complete")
            try {
                withTimeout(timeoutMs) {
                    operation.await()
                }
            } catch (e: TimeoutCancellationException) {
                Logger.shared.warn(
                    TAG,
                    "Timed out waiting for UI operation after ${timeoutMs}ms, clearing stale operation"
                )
                mutex.withLock {
                    if (currentUIOperation == operation) {
                        currentUIOperation = null
                    }
                }
                return
            } catch (e: Exception) {
                // The awaited operation belongs to a different caller — their error
                // was already propagated to them. We only wait for completion so we
                // can safely start our own operation. Loop to re-check in case
                // another operation registered while we were waiting.
            }
        }
    }
}
