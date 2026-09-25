package mega.privacy.android.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.fcm.PushMessageWorker
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.RetryPendingConnectionsUseCase
import mega.privacy.android.domain.usecase.chat.message.SendTextMessageUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.login.InitialiseMegaChatUseCase
import mega.privacy.android.icon.pack.R as iconPackR
import nz.mega.sdk.MegaApiJava
import timber.log.Timber

/**
 * Sends a reply typed in a chat message notification.
 *
 * The reply may arrive after the app process was killed, so the worker performs the same
 * background fast login and MEGAChat initialisation as the push message handling before sending.
 * When the app is already logged in the fast login returns immediately with the current session.
 */
@HiltWorker
class SendChatReplyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase,
    private val retryPendingConnectionsUseCase: RetryPendingConnectionsUseCase,
    private val initialiseMegaChatUseCase: InitialiseMegaChatUseCase,
    private val sendTextMessageUseCase: SendTextMessageUseCase,
    private val notificationManager: NotificationManagerCompat,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        val chatId = inputData.getLong(CHAT_ID, MegaApiJava.INVALID_HANDLE)
        val message = inputData.getString(MESSAGE)

        if (chatId == MegaApiJava.INVALID_HANDLE || message.isNullOrBlank()) {
            Timber.w("Invalid notification reply data for chat $chatId")
            return@withContext Result.failure()
        }

        runCatching {
            val session = backgroundFastLoginUseCase()
            ensureChatInitialised(session)
            sendTextMessageUseCase(chatId, message)
        }.fold(
            onSuccess = {
                Timber.d("Notification reply sent to chat $chatId")
                Result.success()
            },
            onFailure = { error ->
                Timber.e(error, "Notification reply to chat $chatId could not be sent")
                Result.failure()
            }
        )
    }

    /**
     * Below Android 12 expedited work runs as a foreground service, which needs a notification.
     * The silent channel of [PushMessageWorker] keeps it out of sight.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                PushMessageWorker.RETRIEVING_NOTIFICATIONS_ID,
                PushMessageWorker.RETRIEVING_NOTIFICATIONS,
                NotificationManager.IMPORTANCE_NONE
            ).apply {
                enableVibration(false)
                setSound(null, null)
            }
        )
        val notification =
            NotificationCompat.Builder(applicationContext, PushMessageWorker.RETRIEVING_NOTIFICATIONS_ID)
                .setSmallIcon(iconPackR.drawable.ic_stat_notify)
                .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private suspend fun ensureChatInitialised(session: String) {
        runCatching { retryPendingConnectionsUseCase(disconnect = false) }
            .onFailure { error ->
                if (error is ChatNotInitializedErrorStatus) {
                    Timber.d("Chat engine not ready, initialising MEGAChat")
                    initialiseMegaChatUseCase(session)
                } else {
                    Timber.w(error)
                }
            }
    }

    companion object {
        /**
         * Input data key of the chat the reply is sent to
         */
        const val CHAT_ID = "CHAT_ID"

        /**
         * Input data key of the reply text
         */
        const val MESSAGE = "MESSAGE"

        /**
         * Id of the foreground notification while the reply is sent below Android 12
         */
        const val FOREGROUND_NOTIFICATION_ID = 1088
    }
}
