package test.mega.privacy.android.app.fcm


import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.SystemClock
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.WorkDatabase
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.WorkProgressUpdater
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.fcm.PushMessageWorker
import mega.privacy.android.app.notifications.ScheduledMeetingPushMessageNotification
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.data.mapper.pushmessage.PushMessageMapper
import mega.privacy.android.domain.entity.CallsMeetingReminders
import mega.privacy.android.domain.entity.pushes.PushMessage
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.exception.EmptyFolderException
import mega.privacy.android.domain.exception.SessionNotRetrievedException
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.RetryPendingConnectionsUseCase
import mega.privacy.android.domain.usecase.chat.IsChatNotifiableUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.login.InitialiseMegaChatUseCase
import mega.privacy.android.domain.usecase.notifications.GetChatMessageNotificationDataUseCase
import mega.privacy.android.domain.usecase.notifications.PushReceivedUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PushMessageWorkerTest {

    private lateinit var underTest: PushMessageWorker

    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase

    private val backgroundFastLoginUseCase = mock<BackgroundFastLoginUseCase>()
    private val pushReceivedUseCase = mock<PushReceivedUseCase>()
    private val retryPendingConnectionsUseCase = mock<RetryPendingConnectionsUseCase>()
    private val pushMessageMapper = mock<PushMessageMapper>()
    private val initialiseMegaChatUseCase = mock<InitialiseMegaChatUseCase>()
    private val scheduledMeetingPushMessageNotification =
        mock<ScheduledMeetingPushMessageNotification>()
    private val callsPreferencesGateway = mock<CallsPreferencesGateway>()
    private val notificationManager = mock<NotificationManagerCompat>()
    private val isChatNotifiableUseCase = mock<IsChatNotifiableUseCase>()
    private val getChatRoom = mock<GetChatRoom>()
    private val getChatMessageNotificationDataUseCase =
        mock<GetChatMessageNotificationDataUseCase>()
    private val fileDurationMapper = mock<FileDurationMapper>()
    private val ioDispatcher = UnconfinedTestDispatcher()


    @Before
    fun setUp() {
        Dispatchers.setMain(ioDispatcher)

        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase =
            WorkDatabase.create(context, workExecutor.serialTaskExecutor, SystemClock(), true)

        underTest = PushMessageWorker(
            context = context,
            workerParams = WorkerParameters(
                UUID.randomUUID(),
                workDataOf(),
                emptyList(),
                WorkerParameters.RuntimeExtras(),
                1,
                1,
                executor,
                workExecutor,
                WorkerFactory.getDefaultWorkerFactory(),
                WorkProgressUpdater(workDatabase, workExecutor),
                WorkForegroundUpdater(workDatabase,
                    { _, _ -> }, workExecutor
                )
            ),
            backgroundFastLoginUseCase = backgroundFastLoginUseCase,
            pushReceivedUseCase = pushReceivedUseCase,
            retryPendingConnectionsUseCase = retryPendingConnectionsUseCase,
            pushMessageMapper = pushMessageMapper,
            initialiseMegaChatUseCase = initialiseMegaChatUseCase,
            scheduledMeetingPushMessageNotification = scheduledMeetingPushMessageNotification,
            callsPreferencesGateway = callsPreferencesGateway,
            notificationManager = notificationManager,
            isChatNotifiableUseCase = isChatNotifiableUseCase,
            getChatRoom = getChatRoom,
            fileDurationMapper = fileDurationMapper,
            getChatMessageNotificationDataUseCase = getChatMessageNotificationDataUseCase,
            ioDispatcher = ioDispatcher,
            loginMutex = mock()
        )

        whenever(notificationManager.notify(any(), any())).then(mock())
        whenever(pushMessageMapper(any())).thenReturn(PushMessage.CallPushMessage)
        whenever(notificationManager.areNotificationsEnabled()).thenReturn(false)
        whenever(callsPreferencesGateway.getCallsMeetingRemindersPreference())
            .thenReturn(flowOf(CallsMeetingReminders.Disabled))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that doWork returns failure if fast login failed`() = runTest {
        whenever(backgroundFastLoginUseCase()).thenThrow(SessionNotRetrievedException::class.java)
        val result = underTest.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `test that retryPendingConnections is invoked if fast login success`() = runTest {
        whenever(backgroundFastLoginUseCase()).thenReturn("good_session")
        whenever(pushReceivedUseCase.invoke(any(), any())).thenReturn(Unit)

        underTest.doWork()
        verify(retryPendingConnectionsUseCase).invoke(false)
    }

    @Test
    fun `test that MegaChat is initialised if rootNode exists and retryPendingConnections raises ChatNotInitializedException`() =
        runTest {
            whenever(backgroundFastLoginUseCase()).thenReturn("good_session")
            whenever(retryPendingConnectionsUseCase(any())).thenThrow(ChatNotInitializedErrorStatus())

            underTest.doWork()
            verify(initialiseMegaChatUseCase).invoke("good_session")
        }

    @Test
    fun `test that initialiseMegaChat is not invoked if rootNode exists and retryPendingConnections raises exception other than ChatNotInitializedException`() =
        runTest {
            whenever(backgroundFastLoginUseCase()).thenReturn("good_session")
            whenever(retryPendingConnectionsUseCase(any())).thenThrow(
                EmptyFolderException()
            )

            underTest.doWork()
            verifyNoInteractions(initialiseMegaChatUseCase)
        }

    @Test
    fun `test that doWork returns failure if rootNode exists and retryPendingConnections raises ChatNotInitializedException and initialiseMegaChat fails`() =
        runTest {
            val sessionId = "good_session"
            whenever(backgroundFastLoginUseCase()).thenReturn(sessionId)
            whenever(retryPendingConnectionsUseCase(any())).thenThrow(ChatNotInitializedErrorStatus())
            whenever(initialiseMegaChatUseCase(sessionId)).thenThrow(ChatNotInitializedErrorStatus())

            val result = underTest.doWork()
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that ChatPushMessage is triggered as expected and getChatMessageNotificationDataUseCase is not called when chat is not notifiable`() {
        runTest {
            val push = PushMessage.ChatPushMessage(true, 1L, 2L)
            whenever(pushMessageMapper(any())).thenReturn(push)
            whenever(pushReceivedUseCase(push.shouldBeep, push.chatId)).thenReturn(Unit)
            whenever(isChatNotifiableUseCase(push.chatId)).thenReturn(false)
            val result = underTest.doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            verifyNoInteractions(getChatMessageNotificationDataUseCase)
        }
    }

    @Test
    fun `test that ScheduledMeetingPushMessage are triggered as expected`() {
        runTest {
            val pushMessage = PushMessage.ScheduledMeetingPushMessage(
                schedId = -1L,
                userHandle = -1L,
                chatRoomHandle = -1L,
                title = null,
                description = null,
                startTimestamp = 0L,
                endTimestamp = 0L,
                timezone = null,
                isStartReminder = false,
            )
            whenever(pushMessageMapper(any())).thenReturn(pushMessage)
            whenever(notificationManager.areNotificationsEnabled()).thenReturn(true)
            whenever(getChatRoom.invoke(any())).thenReturn(mock())
            whenever(callsPreferencesGateway.getCallsMeetingRemindersPreference())
                .thenReturn(flowOf(CallsMeetingReminders.Enabled))

            val result = underTest.doWork()

            verify(getChatRoom).invoke(-1L)
            verify(scheduledMeetingPushMessageNotification).show(context, pushMessage)
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }
    }
}
