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
import mega.privacy.android.app.R
import mega.privacy.android.app.data.mapper.PushMessageMapper
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.domain.exception.ChatNotInitializedException
import mega.privacy.android.domain.exception.LoginAlreadyRunningException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.CompleteFastLogin
import mega.privacy.android.domain.usecase.GetSession
import mega.privacy.android.domain.usecase.InitialiseMegaChat
import mega.privacy.android.domain.usecase.PushReceived
import mega.privacy.android.domain.usecase.RetryPendingConnections
import mega.privacy.android.domain.usecase.RootNodeExists
import timber.log.Timber

/**
 * Worker class to manage push notifications.
 *
 * @property getSession              Required for checking credentials.
 * @property rootNodeExists          Required for checking if it is logged in.
 * @property completeFastLogin       Required for performing a complete login process with an existing session.
 * @property pushReceived            Required for notifying received pushes.
 * @property retryPendingConnections Required for retrying pending connections.
 * @property pushMessageMapper       [PushMessageMapper].
 */
@HiltWorker
class PushMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getSession: GetSession,
    private val rootNodeExists: RootNodeExists,
    private val completeFastLogin: CompleteFastLogin,
    private val pushReceived: PushReceived,
    private val retryPendingConnections: RetryPendingConnections,
    private val pushMessageMapper: PushMessageMapper,
    private val initialiseMegaChat: InitialiseMegaChat,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result =
        withContext(ioDispatcher) {
            val session = getSession() ?: return@withContext Result.failure().also {
                Timber.e("No user credentials, process terminates!")
            }

            val pushMessage = pushMessageMapper(inputData)

            if (!rootNodeExists()) {
                Timber.d("Needs fast login")

                kotlin.runCatching { completeFastLogin(session) }
                    .fold(
                        { Timber.d("Fast login success.") },
                        { error ->
                            if (error is LoginAlreadyRunningException) {
                                Timber.d(error, "No more actions required.")
                                return@withContext Result.success()
                            } else {
                                Timber.e("Fast login error: $error")
                                return@withContext Result.failure()
                            }
                        }
                    )
            } else {
                kotlin.runCatching { retryPendingConnections(disconnect = false) }
                    .onFailure { error ->
                        if (error is ChatNotInitializedException) {
                            Timber.d("chat engine not ready. try to initialise megachat.")
                            kotlin.runCatching { initialiseMegaChat(session) }
                                .onFailure { exception ->
                                    Timber.e("Initialise MEGAChat failed: $exception")
                                    return@withContext Result.failure()
                                }
                        } else {
                            Timber.w(error)
                        }
                    }
            }

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
            TYPE_CHAT -> getNotification(R.drawable.ic_stat_notify,
                R.string.notification_chat_undefined_content)
            else -> getNotification(R.drawable.ic_stat_notify)
        }

        return ForegroundInfo(NOTIFICATION_CHANNEL_ID, notification)
    }

    private fun getNotification(iconId: Int, titleId: Int? = null): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                RETRIEVING_NOTIFICATIONS_ID,
                RETRIEVING_NOTIFICATIONS,
                NotificationManager.IMPORTANCE_NONE).apply {
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