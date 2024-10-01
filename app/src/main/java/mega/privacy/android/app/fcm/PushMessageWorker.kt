package mega.privacy.android.app.fcm

import mega.privacy.android.icon.pack.R as iconPackR
import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.notifications.ChatMessageNotificationManager
import mega.privacy.android.app.notifications.PromoPushNotificationManager
import mega.privacy.android.app.notifications.ScheduledMeetingPushMessageNotificationManager
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.data.mapper.pushmessage.PushMessageMapper
import mega.privacy.android.domain.entity.CallsMeetingReminders
import mega.privacy.android.domain.entity.pushes.PushMessage
import mega.privacy.android.domain.entity.pushes.PushMessage.CallPushMessage
import mega.privacy.android.domain.entity.pushes.PushMessage.ChatPushMessage
import mega.privacy.android.domain.entity.pushes.PushMessage.PromoPushMessage
import mega.privacy.android.domain.entity.pushes.PushMessage.ScheduledMeetingPushMessage
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.RetryPendingConnectionsUseCase
import mega.privacy.android.domain.usecase.chat.IsChatNotifiableUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.login.InitialiseMegaChatUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.notifications.GetChatMessageNotificationDataUseCase
import mega.privacy.android.domain.usecase.notifications.PushReceivedUseCase
import timber.log.Timber

/**
 * Worker class to manage push notifications.
 *
 *  @property backgroundFastLoginUseCase  Use case to login to the server
 *  @property pushReceivedUseCase        Use case to manage push notifications
 *  @property retryPendingConnectionsUseCase Use case to retry pending connections
 *  @property pushMessageMapper         Mapper to convert worker data to PushMessage
 *  @property initialiseMegaChatUseCase Use case to initialise MegaChat
 *  @property scheduledMeetingPushMessageNotificationManager Use case to show a device Notification given a [ScheduledMeetingPushMessage]
 *  @property promoPushNotificationManager     Use case to show a device Notification given a [PromoPushMessage]
 *  @property callsPreferencesGateway  Gateway to manage calls preferences
 *  @property notificationManager      Notification manager
 *  @property isChatNotifiableUseCase  Use case to check if a chat is notifiable
 *  @property getChatRoomUseCase       Use case to get a chat room
 *  @property monitorChatCallUpdatesUseCase   Use case to monitor call updates
 *  @property fileDurationMapper      Mapper to convert file duration
 *  @property getChatMessageNotificationDataUseCase Use case to get chat message notification data
 *  @property chatMessageNotificationManager Use case to show a device Notification given a [ChatMessageNotificationData]
 *  @property ioDispatcher            Dispatcher to perform work in background
 *  @property loginMutex               Mutex to avoid multiple logins at the same time
 */
@HiltWorker
class PushMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase,
    private val pushReceivedUseCase: PushReceivedUseCase,
    private val retryPendingConnectionsUseCase: RetryPendingConnectionsUseCase,
    private val pushMessageMapper: PushMessageMapper,
    private val initialiseMegaChatUseCase: InitialiseMegaChatUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val scheduledMeetingPushMessageNotificationManager: ScheduledMeetingPushMessageNotificationManager,
    private val promoPushNotificationManager: PromoPushNotificationManager,
    private val callsPreferencesGateway: CallsPreferencesGateway,
    private val notificationManager: NotificationManagerCompat,
    private val isChatNotifiableUseCase: IsChatNotifiableUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val fileDurationMapper: FileDurationMapper,
    private val getChatMessageNotificationDataUseCase: GetChatMessageNotificationDataUseCase,
    private val chatMessageNotificationManager: ChatMessageNotificationManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @LoginMutex private val loginMutex: Mutex,
) : CoroutineWorker(context, workerParams) {

    /**
     * Job to monitor chat call updates flow
     */
    private var monitorChatCallUpdatesJob: Job? = null

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        // legacy support, other places need to know logging in happen
        if (loginMutex.isLocked) {
            Timber.w("Logging already running.")
            return@withContext Result.failure()
        }

        getPushMessageFromWorkerData(inputData)?.let {
            if (it is CallPushMessage) {
                runCatching {
                    monitorChatCallUpdatesJob = monitorChatCallUpdates(it.chatId)
                }
            }
        }

        val loginResult = runCatching {
            backgroundFastLoginUseCase()
        }

        if (loginResult.isSuccess) {
            Timber.d("Fast login success.")
            runCatching { retryPendingConnectionsUseCase(disconnect = false) }
                .recoverCatching { error ->
                    if (error is ChatNotInitializedErrorStatus) {
                        Timber.d("chat engine not ready. try to initialise megachat.")
                        initialiseMegaChatUseCase(loginResult.getOrDefault(""))
                    } else {
                        Timber.w(error)
                    }
                }.onFailure { error ->
                    Timber.e("Initialise MEGAChat failed: $error")
                    cancelJobs()
                    return@withContext Result.failure()
                }
        } else {
            Timber.e("Fast login error: ${loginResult.exceptionOrNull()}")
            cancelJobs()
            return@withContext Result.failure()
        }

        when (val pushMessage = getPushMessageFromWorkerData(inputData)) {
            is ChatPushMessage -> {
                with(pushMessage) {
                    Timber.d("Should beep: $shouldBeep, Chat: $chatId, message: $msgId")

                    if (chatId == -1L || msgId == -1L) {
                        Timber.d("Message should be managed in onChatNotification")
                        cancelJobs()
                        return@withContext Result.success()
                    }

                    runCatching {
                        pushReceivedUseCase(shouldBeep)
                    }.onSuccess {
                        Timber.d("Push received success chatID: $chatId msgId:$msgId")
                        if (!isChatNotifiableUseCase(chatId) || !areNotificationsEnabled()) {
                            cancelJobs()
                            return@with

                        }

                        val data = getChatMessageNotificationDataUseCase(
                            shouldBeep,
                            chatId,
                            msgId,
                            runCatching { DEFAULT_NOTIFICATION_URI.toString() }.getOrNull()
                        ) ?: return@withContext Result.failure()


                        chatMessageNotificationManager.show(
                            applicationContext,
                            data,
                            fileDurationMapper
                        )
                    }.onFailure { error ->
                        Timber.e(error)
                        cancelJobs()
                        return@withContext Result.failure()
                    }
                }
            }

            is ScheduledMeetingPushMessage -> {
                if (areNotificationsEnabled() && areMeetingRemindersEnabled()) {
                    runCatching {
                        scheduledMeetingPushMessageNotificationManager.show(
                            applicationContext,
                            pushMessage.updateTitle()
                        )
                    }.onFailure { error ->
                        Timber.e(error)
                        cancelJobs()
                        return@withContext Result.failure()
                    }
                }
            }

            is PromoPushMessage -> {
                runCatching { promoPushNotificationManager.show(applicationContext, pushMessage) }
                    .onFailure { error ->
                        Timber.e(error)
                        cancelJobs()
                        return@withContext Result.failure()
                    }
            }

            else -> {
                Timber.w("Unsupported Push Message type")
            }
        }

        cancelJobs()
        return@withContext Result.success()
    }

    /**
     * Get push message from worker input data
     *
     * @param data
     * @return          Push Message
     */
    private fun getPushMessageFromWorkerData(data: Data): PushMessage? =
        runCatching { pushMessageMapper(data) }
            .onFailure(Timber.Forest::e)
            .getOrNull()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = when (pushMessageMapper(inputData)) {
            is CallPushMessage -> getNotification(R.drawable.ic_call_started)
            is ChatPushMessage -> getNotification(
                iconPackR.drawable.ic_stat_notify,
                R.string.notification_chat_undefined_content
            )

            else -> getNotification(iconPackR.drawable.ic_stat_notify)
        }

        return ForegroundInfo(NOTIFICATION_CHANNEL_ID, notification)
    }

    private fun getNotification(iconId: Int, titleId: Int? = null): Notification {
        val notificationChannel = NotificationChannel(
            RETRIEVING_NOTIFICATIONS_ID,
            RETRIEVING_NOTIFICATIONS,
            NotificationManager.IMPORTANCE_NONE
        ).apply {
            enableVibration(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(notificationChannel)

        return NotificationCompat.Builder(applicationContext, RETRIEVING_NOTIFICATIONS_ID)
            .setSmallIcon(iconId)
            .apply {
                titleId?.let { setContentText(applicationContext.getString(titleId)) }
            }.build()
    }

    /**
     * Check if notifications are enabled and required permissions are granted
     *
     * @return  True if are enabled, false otherwise
     */
    private fun areNotificationsEnabled(): Boolean =
        notificationManager.areNotificationsEnabled() &&
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

    /**
     * Check if meeting reminders are enabled
     *
     * @return  True if are enabled, false otherwise
     */
    private suspend fun areMeetingRemindersEnabled(): Boolean =
        callsPreferencesGateway.getCallsMeetingRemindersPreference().firstOrNull() ==
                CallsMeetingReminders.Enabled

    private suspend fun ScheduledMeetingPushMessage.updateTitle(): ScheduledMeetingPushMessage =
        runCatching { getChatRoomUseCase(chatRoomHandle)?.title }.getOrNull()
            ?.let { chatRoomTitle ->
                copy(title = chatRoomTitle)
            } ?: this

    /**
     * Monitors and processes only the Camera Uploads Transfers
     */
    private fun CoroutineScope.monitorChatCallUpdates(chatIdPushMessage: Long) = launch {
        monitorChatCallUpdatesUseCase()
            .filter { it.chatId == chatIdPushMessage }
            .distinctUntilChanged().collectLatest {
                Timber.d("Call updated")
            }
    }

    private suspend fun cancelJobs() {
        monitorChatCallUpdatesJob?.cancelAndJoin()
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = 1086
        const val RETRIEVING_NOTIFICATIONS_ID = "RETRIEVING_NOTIFICATIONS_ID"
        const val RETRIEVING_NOTIFICATIONS = "RETRIEVING_NOTIFICATIONS"
    }
}
