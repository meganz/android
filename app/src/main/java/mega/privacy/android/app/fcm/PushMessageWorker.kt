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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.data.mapper.PushMessageMapper
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.PushReceived
import mega.privacy.android.domain.usecase.RetryPendingConnections
import mega.privacy.android.domain.usecase.login.BackgroundFastLogin
import mega.privacy.android.domain.usecase.login.InitialiseMegaChat
import timber.log.Timber

/**
 * Worker class to manage push notifications.
 *
 * @property backgroundFastLogin       Required for performing a complete login process with an existing session.
 * @property pushReceived            Required for notifying received pushes.
 * @property retryPendingConnections Required for retrying pending connections.
 * @property pushMessageMapper       [PushMessageMapper].
 */
@HiltWorker
class PushMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val backgroundFastLogin: BackgroundFastLogin,
    private val pushReceived: PushReceived,
    private val retryPendingConnections: RetryPendingConnections,
    private val pushMessageMapper: PushMessageMapper,
    private val initialiseMegaChat: InitialiseMegaChat,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result =
        withContext(ioDispatcher) {

            // legacy support, other places need to know logging in happen
            if (MegaApplication.isLoggingIn) {
                Timber.w("Logging already running.")
                return@withContext Result.failure()
            }

            MegaApplication.isLoggingIn = true
            val result = runCatching { backgroundFastLogin() }
            MegaApplication.isLoggingIn = false

            if (result.isSuccess) {
                Timber.d("Fast login success.")
                runCatching { retryPendingConnections(disconnect = false) }
                    .recoverCatching { error ->
                        if (error is ChatNotInitializedErrorStatus) {
                            Timber.d("chat engine not ready. try to initialise megachat.")
                            initialiseMegaChat(result.getOrDefault(""))
                        } else {
                            Timber.w(error)
                        }
                    }.onFailure { error ->
                        Timber.e("Initialise MEGAChat failed: $error")
                        return@withContext Result.failure()
                    }
            } else {
                Timber.e("Fast login error: ${result.exceptionOrNull()}")
                return@withContext Result.failure()
            }

            val pushMessage = pushMessageMapper(inputData)
            Timber.d("PushMessage.type: ${pushMessage.type}")

            if (pushMessage.type == TYPE_CHAT) {
                kotlin.runCatching { pushReceived(pushMessage.shouldBeep()) }
                    .fold(
                        { request ->
                            ChatAdvancedNotificationBuilder.newInstance(applicationContext)
                                .generateChatNotification(request)
                        },
                        { error ->
                            Timber.e("Push received error: ${error.message}")
                            return@withContext Result.failure()
                        }
                    )
            }

            Result.success()
        }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = when (pushMessageMapper(inputData).type) {
            TYPE_CALL -> getNotification(R.drawable.ic_call_started)
            TYPE_CHAT -> getNotification(
                R.drawable.ic_stat_notify,
                R.string.notification_chat_undefined_content
            )
            else -> getNotification(R.drawable.ic_stat_notify)
        }

        return ForegroundInfo(NOTIFICATION_CHANNEL_ID, notification)
    }

    private fun getNotification(iconId: Int, titleId: Int? = null): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                RETRIEVING_NOTIFICATIONS_ID,
                RETRIEVING_NOTIFICATIONS,
                NotificationManager.IMPORTANCE_NONE
            ).apply {
                enableVibration(false)
                setSound(null, null)
            }
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(applicationContext, RETRIEVING_NOTIFICATIONS_ID)
            .apply {
                setSmallIcon(iconId)

                if (titleId != null) {
                    setContentText(getString(titleId))
                }
            }

        return builder.build()
    }

    companion object {
        private const val TYPE_SHARE_FOLDER = "1"
        private const val TYPE_CHAT = "2"
        private const val TYPE_CONTACT_REQUEST = "3"
        private const val TYPE_CALL = "4"
        private const val TYPE_ACCEPTANCE = "5"

        const val NOTIFICATION_CHANNEL_ID = 1086
        const val RETRIEVING_NOTIFICATIONS_ID = "RETRIEVING_NOTIFICATIONS_ID"
        const val RETRIEVING_NOTIFICATIONS = "RETRIEVING_NOTIFICATIONS"
    }
}