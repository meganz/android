package mega.privacy.android.app.notifications

import android.app.NotificationChannel
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Data
import androidx.work.DefaultWorkerFactory
import androidx.work.ListenableWorker
import androidx.work.SystemClock
import androidx.work.WorkerParameters
import androidx.work.impl.WorkDatabase
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.WorkProgressUpdater
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.fcm.PushMessageWorker
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.usecase.RetryPendingConnectionsUseCase
import mega.privacy.android.domain.usecase.chat.message.SendTextMessageUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.login.InitialiseMegaChatUseCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SendChatReplyWorkerTest {

    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase

    private val backgroundFastLoginUseCase = mock<BackgroundFastLoginUseCase>()
    private val retryPendingConnectionsUseCase = mock<RetryPendingConnectionsUseCase>()
    private val initialiseMegaChatUseCase = mock<InitialiseMegaChatUseCase>()
    private val sendTextMessageUseCase = mock<SendTextMessageUseCase>()
    private val notificationManager = mock<NotificationManagerCompat>()
    private val ioDispatcher = UnconfinedTestDispatcher()

    private val validInputData = workDataOf(
        SendChatReplyWorker.CHAT_ID to CHAT_ID,
        SendChatReplyWorker.MESSAGE to REPLY,
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase =
            WorkDatabase.create(context, workExecutor.serialTaskExecutor, SystemClock(), true)
    }

    @Test
    fun `test that doWork returns failure without logging in when the chat id is missing`() =
        runTest {
            val result = createWorker(workDataOf(SendChatReplyWorker.MESSAGE to REPLY)).doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
            verifyNoInteractions(backgroundFastLoginUseCase, sendTextMessageUseCase)
        }

    @Test
    fun `test that doWork returns failure without logging in when the message is blank`() =
        runTest {
            val inputData = workDataOf(
                SendChatReplyWorker.CHAT_ID to CHAT_ID,
                SendChatReplyWorker.MESSAGE to "   ",
            )

            val result = createWorker(inputData).doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
            verifyNoInteractions(backgroundFastLoginUseCase, sendTextMessageUseCase)
        }

    @Test
    fun `test that doWork sends the reply after the fast login when the data is valid`() =
        runTest {
            whenever(backgroundFastLoginUseCase()).thenReturn(SESSION)

            val result = createWorker(validInputData).doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            verify(retryPendingConnectionsUseCase).invoke(disconnect = false)
            verify(initialiseMegaChatUseCase, never()).invoke(any())
            verify(sendTextMessageUseCase).invoke(CHAT_ID, REPLY)
        }

    @Test
    fun `test that doWork initialises MEGAChat before sending when the chat engine is not initialised`() =
        runTest {
            whenever(backgroundFastLoginUseCase()).thenReturn(SESSION)
            whenever(retryPendingConnectionsUseCase(disconnect = false))
                .thenThrow(ChatNotInitializedErrorStatus())

            val result = createWorker(validInputData).doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            verify(initialiseMegaChatUseCase).invoke(SESSION)
            verify(sendTextMessageUseCase).invoke(CHAT_ID, REPLY)
        }

    @Test
    fun `test that doWork still sends the reply when retrying pending connections fails for another reason`() =
        runTest {
            whenever(backgroundFastLoginUseCase()).thenReturn(SESSION)
            whenever(retryPendingConnectionsUseCase(disconnect = false))
                .thenThrow(RuntimeException("offline"))

            val result = createWorker(validInputData).doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            verify(initialiseMegaChatUseCase, never()).invoke(any())
            verify(sendTextMessageUseCase).invoke(CHAT_ID, REPLY)
        }

    @Test
    fun `test that doWork returns failure without sending when the fast login fails`() = runTest {
        whenever(backgroundFastLoginUseCase()).thenThrow(RuntimeException("no session"))

        val result = createWorker(validInputData).doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        verifyNoInteractions(sendTextMessageUseCase)
    }

    @Test
    fun `test that doWork returns failure when sending fails`() = runTest {
        whenever(backgroundFastLoginUseCase()).thenReturn(SESSION)
        whenever(sendTextMessageUseCase(CHAT_ID, REPLY)).thenThrow(RuntimeException("send failed"))

        val result = createWorker(validInputData).doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `test that getForegroundInfo posts a silent notification for expedited work below Android 12`() =
        runTest {
            val foregroundInfo = createWorker(validInputData).getForegroundInfo()

            assertThat(foregroundInfo.notificationId)
                .isEqualTo(SendChatReplyWorker.FOREGROUND_NOTIFICATION_ID)
            assertThat(foregroundInfo.notification.channelId)
                .isEqualTo(PushMessageWorker.RETRIEVING_NOTIFICATIONS_ID)
            verify(notificationManager).createNotificationChannel(any<NotificationChannel>())
        }

    private fun createWorker(inputData: Data) = SendChatReplyWorker(
        context = context,
        workerParams = WorkerParameters(
            UUID.randomUUID(),
            inputData,
            emptyList(),
            WorkerParameters.RuntimeExtras(),
            1,
            1,
            executor,
            Dispatchers.Unconfined,
            workExecutor,
            DefaultWorkerFactory,
            WorkProgressUpdater(workDatabase, workExecutor),
            WorkForegroundUpdater(workDatabase, { _, _ -> }, workExecutor),
        ),
        backgroundFastLoginUseCase = backgroundFastLoginUseCase,
        retryPendingConnectionsUseCase = retryPendingConnectionsUseCase,
        initialiseMegaChatUseCase = initialiseMegaChatUseCase,
        sendTextMessageUseCase = sendTextMessageUseCase,
        notificationManager = notificationManager,
        ioDispatcher = ioDispatcher,
    )

    private companion object {
        const val CHAT_ID = 123L
        const val REPLY = "Reply from notification"
        const val SESSION = "session"
    }
}
