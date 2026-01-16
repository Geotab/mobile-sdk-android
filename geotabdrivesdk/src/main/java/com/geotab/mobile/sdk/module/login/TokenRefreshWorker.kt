package com.geotab.mobile.sdk.module.login

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Data
import com.geotab.mobile.sdk.logging.Logger
import com.geotab.mobile.sdk.module.auth.AuthUtil
import com.geotab.mobile.sdk.module.auth.AuthError
import java.util.concurrent.TimeUnit

class TokenRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "TokenRefreshWorker"
        internal const val KEY_USERNAME = "username"
        const val UNIQUE_WORK_NAME_PREFIX = "token-refresh-work-"
        fun getUniqueWorkName(username: String): String = "$UNIQUE_WORK_NAME_PREFIX$username"

        fun cancelAllTokenRefreshWork(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(UNIQUE_WORK_NAME_PREFIX)
        }

        fun scheduleTokenRefreshWorker(
            context: Context,
            username: String,
            delayMillis: Long
        ) {
            Logger.shared.debug(TAG, "scheduling token refresh")
            val workManager = WorkManager.getInstance(context)
            val workName = getUniqueWorkName(username)
            val inputData = Data.Builder().putString(KEY_USERNAME, username).build()
            val request = OneTimeWorkRequestBuilder<TokenRefreshWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .addTag(UNIQUE_WORK_NAME_PREFIX)
                .setInputData(inputData)
                .build()

            workManager.enqueueUniqueWork(
                workName,
                androidx.work.ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val username = inputData.getString(KEY_USERNAME)
            ?: return Result.failure().also {
                Logger.shared.error(
                    TAG,
                    "Username not provided in worker data"
                )
            }
        val authUtil: AuthUtil = try {
            AuthUtil.getInstance()
        } catch (e: IllegalStateException) {
            Logger.shared.error(
                TAG,
                "AuthUtil has not been initialized: ${e.message}"
            )
            return Result.failure()
        }
        return try {
            val token = authUtil.getValidAccessToken(
                applicationContext,
                username,
                forceRefresh = true,
                startScheduler = false
            )
            Logger.shared.debug(TAG, "After fetching token")

            if (token != null) {
                // token successfully obtained; proceed with scheduling next refresh
                Logger.shared.debug(
                    TAG,
                    "Token is valid or was refreshed successfully"
                )
                // continue the chain by enqueuing the next TokenRefreshWorker
                authUtil.scheduleNextRefreshToken(applicationContext, username)
                Result.success()
            } else {
                Logger.shared.error(
                    TAG,
                    "Failed to refresh token. A new login may be required."
                )
                Result.failure()
            }
        } catch (e: Exception) {
            // Check if this is a recoverable error that should be retried
            when (e) {
                is AuthError.TokenRefreshFailed -> {
                    if (!e.requiresReauthentication) {
                        // Recoverable error (network issue) - schedule retry with exponential backoff
                        Logger.shared.debug(
                            TAG,
                            "Recoverable error detected, scheduling retry with backoff for $username"
                        )
                        authUtil.scheduleNextRefreshTokenWithBackoff(applicationContext, username)
                    } else {
                        // For non-recoverable errors (requiresReauthentication=true):
                        // - If app was in foreground: reauth already happened and reset was done in handleSuccessfulTokenExchange
                        // - If app was in background: reauth was deferred, reset here to avoid backoff accumulation
                        authUtil.resetRetryAttempts(username)
                    }
                }
            }
            // For other exceptions (reauth failed, etc.), no retry needed
            Result.failure()
        }
    }
}
