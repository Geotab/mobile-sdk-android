package com.geotab.mobile.sdk.module.auth

import com.geotab.mobile.sdk.logging.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
class AuthorizationCoordinator {
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
    ): AuthToken {
        // Check for existing login first, before waiting for UI operations
        mutex.withLock {
            inFlightLogins[username]?.let { existingOperation ->
                Logger.shared.debug(TAG, "Login already in progress for $username, sharing result")
                return existingOperation.await()
            }
        }

        // Wait for any existing UI operation to complete
        waitForUIOperation()

        Logger.shared.debug(TAG, "Starting new login for $username")

        val deferred = CompletableDeferred<AuthToken>()
        mutex.withLock {
            inFlightLogins[username] = deferred
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
                inFlightLogins.remove(username)
                if (currentUIOperation == deferred) {
                    currentUIOperation = null
                }
            }
        }
    }

    /**
     * Perform re-authentication operation, deduplicating concurrent requests for the same user.
     */
    suspend fun performReauth(
        username: String,
        operation: suspend () -> AuthToken
    ): AuthToken {
        // Check for existing reauth first
        mutex.withLock {
            inFlightReauths[username]?.let { existingOperation ->
                Logger.shared.debug(TAG, "Re-authentication already in progress for $username, sharing result")
                return existingOperation.await()
            }
        }

        // Wait for any existing UI operation to complete
        waitForUIOperation()

        Logger.shared.debug(TAG, "Starting new re-authentication for $username")

        val deferred = CompletableDeferred<AuthToken>()
        mutex.withLock {
            inFlightReauths[username] = deferred
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
                inFlightReauths.remove(username)
                if (currentUIOperation == deferred) {
                    currentUIOperation = null
                }
            }
        }
    }

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
        mutex.withLock {
            inFlightTokenRefresh[key]?.let { existingOperation ->
                Logger.shared.debug(TAG, "Token refresh already in progress for $username (force=$forceRefresh), sharing result")
                return existingOperation.await()
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
    ) {
        // Check for existing logout first
        mutex.withLock {
            inFlightLogouts[username]?.let { existingOperation ->
                Logger.shared.debug(TAG, "Logout already in progress for $username, waiting for completion")
                return existingOperation.await()
            }
        }

        // Wait for any existing UI operation to complete
        waitForUIOperation()

        Logger.shared.debug(TAG, "Starting new logout for $username")

        val deferred = CompletableDeferred<Unit>()
        mutex.withLock {
            inFlightLogouts[username] = deferred
            currentUIOperation = deferred
        }

        try {
            operation()
            deferred.complete(Unit)
        } catch (e: Exception) {
            deferred.completeExceptionally(e)
            throw e
        } finally {
            mutex.withLock {
                inFlightLogouts.remove(username)
                if (currentUIOperation == deferred) {
                    currentUIOperation = null
                }
            }
        }
    }

    /**
     * Wait for any existing UI operation to complete before starting a new one.
     * This prevents multiple UI-presenting operations (login, reauth, logout) from
     * running simultaneously.
     */
    private suspend fun waitForUIOperation() {
        val operation = mutex.withLock { currentUIOperation }
        if (operation != null) {
            Logger.shared.debug(TAG, "Waiting for existing UI operation to complete")
            try {
                operation.await()
            } catch (e: Exception) {
                // Ignore errors from previous operation
            }
        }
    }
}
