package com.geotab.mobile.sdk.module.login

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.geotab.mobile.sdk.logging.InternalAppLogging
import androidx.work.Data
import java.util.concurrent.TimeUnit

class TokenRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
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
            InternalAppLogging.appLogger?.debug("TokenRefreshWorker", "scheduling token refresh")
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
                InternalAppLogging.appLogger?.error(
                    "TokenRefreshWorker",
                    "Username not provided in worker data"
                )
            }
        val authUtil: AuthUtil = try {
            AuthUtil.getInstance()
        } catch (e: IllegalStateException) {
            InternalAppLogging.appLogger?.error(
                "TokenRefreshWorker",
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
            InternalAppLogging.appLogger?.debug("TokenRefreshWorker", "After fetching token")

            if (token != null) {
                // token successfully obtained; proceed with scheduling next refresh
                InternalAppLogging.appLogger?.debug(
                    "TokenRefreshWorker",
                    "Token is valid or was refreshed successfully"
                )
                // continue the chain by enqueuing the next TokenRefreshWorker
                authUtil.scheduleNextRefreshToken(applicationContext, username)
                Result.success()
            } else {
                InternalAppLogging.appLogger?.error(
                    "TokenRefreshWorker",
                    "Failed to refresh token. A new login may be required."
                )
                Result.failure()
            }
        } catch (e: Exception) {
            InternalAppLogging.appLogger?.error(
                "TokenRefreshWorker",
                "Token refresh failed with an exception: ${e.message}"
            )
            Result.failure()
        }
    }
}
