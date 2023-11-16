package mega.privacy.android.app.fcm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.domain.usecase.GetPushToken
import mega.privacy.android.domain.usecase.pushnotifications.RegisterPushNotificationsUseCase
import mega.privacy.android.domain.usecase.pushnotifications.SetPushTokenUseCase
import timber.log.Timber

/**
 * Worker class to manage device token updates.
 *
 * @property getPushToken               Required for getting push token.
 * @property registerPushNotificationsUseCase  Required for registering push notifications.
 * @property setPushTokenUseCase               Required for setting push token.
 */
@HiltWorker
class NewTokenWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getPushToken: GetPushToken,
    private val registerPushNotificationsUseCase: RegisterPushNotificationsUseCase,
    private val setPushTokenUseCase: SetPushTokenUseCase,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val newToken = inputData.getString(NEW_TOKEN)

            if (newToken.isNullOrEmpty() || getPushToken() == newToken) {
                Timber.d("No need to register new token.")
            } else {
                Timber.d("Push service's new token: $newToken")
                runCatching {
                    registerPushNotificationsUseCase(
                        inputData.getInt(
                            DEVICE_TYPE,
                            INVALID_VALUE
                        ), newToken
                    )
                }.onSuccess { token ->
                    runCatching { setPushTokenUseCase(token) }
                        .onFailure { Timber.w("Exception setting push token: $it") }
                }.onFailure { Timber.w("Exception registering push notifications: $it") }
            }

            Result.success()
        }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(NOTIFICATION_CHANNEL_ID, getNotification())
    }

    private fun getNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                RETRIEVING_NEW_TOKEN_ID,
                RETRIEVING_NEW_TOKEN,
                NotificationManager.IMPORTANCE_NONE
            ).apply {
                enableVibration(false)
                setSound(null, null)
            }
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(
            applicationContext,
            RETRIEVING_NEW_TOKEN_ID
        ).apply {
            setSmallIcon(iconPackR.drawable.ic_stat_notify)
        }

        return builder.build()
    }

    companion object {
        const val NEW_TOKEN = "NEW_TOKEN"
        const val DEVICE_TYPE = "DEVICE_TYPE"
        const val WORK_NAME = "NewTokenWorker"

        const val NOTIFICATION_CHANNEL_ID = 1087
        const val RETRIEVING_NEW_TOKEN_ID = "RETRIEVING_NEW_TOKEN_ID"
        const val RETRIEVING_NEW_TOKEN = "RETRIEVING_NEW_TOKEN"
    }
}