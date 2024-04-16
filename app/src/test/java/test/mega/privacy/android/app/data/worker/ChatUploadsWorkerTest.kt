package test.mega.privacy.android.app.data.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ProgressUpdater
import androidx.work.SystemClock
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.WorkDatabase
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.mapper.transfer.ChatUploadNotificationMapper
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.data.worker.AreNotificationsEnabledUseCase
import mega.privacy.android.data.worker.ChatUploadsWorker
import mega.privacy.android.data.worker.ForegroundSetter
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.chat.message.AttachNodeWithPendingMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CheckFinishedChatUploadsUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class ChatUploadsWorkerTest {
    private lateinit var underTest: ChatUploadsWorker


    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase

    private val workProgressUpdater = mock<ProgressUpdater>()

    private val attachNodeWithPendingMessageUseCase = mock<AttachNodeWithPendingMessageUseCase>()
    private val monitorTransferEventsUseCase = mock<MonitorTransferEventsUseCase>()
    private val handleTransferEventUseCase = mock<HandleTransferEventUseCase>()
    private val monitorOngoingActiveTransfersUseCase = mock<MonitorOngoingActiveTransfersUseCase>()
    private val areTransfersPausedUseCase = mock<AreTransfersPausedUseCase>()
    private val getActiveTransferTotalsUseCase = mock<GetActiveTransferTotalsUseCase>()
    private val overQuotaNotificationBuilder = mock<OverQuotaNotificationBuilder>()
    private val notificationManager = mock<NotificationManagerCompat>()
    private val areNotificationsEnabledUseCase = mock<AreNotificationsEnabledUseCase>()
    private val correctActiveTransfersUseCase = mock<CorrectActiveTransfersUseCase>()
    private val clearActiveTransfersIfFinishedUseCase =
        mock<ClearActiveTransfersIfFinishedUseCase>()
    private val chatUploadNotificationMapper = mock<ChatUploadNotificationMapper>()
    private val chatMessageRepository = mock<ChatMessageRepository>()
    private val updatePendingMessageUseCase = mock<UpdatePendingMessageUseCase>()
    private val checkFinishedChatUploadsUseCase = mock<CheckFinishedChatUploadsUseCase>()
    private val setForeground = mock<ForegroundSetter>()
    private val crashReporter = mock<CrashReporter>()

    @Before
    fun init() {
        val ioDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(ioDispatcher)
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase =
            WorkDatabase.create(context, workExecutor.serialTaskExecutor, SystemClock(), true)
        underTest = ChatUploadsWorker(
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
                workProgressUpdater,
                WorkForegroundUpdater(
                    workDatabase,
                    { _, _ -> }, workExecutor
                )
            ),
            ioDispatcher = ioDispatcher,
            monitorTransferEventsUseCase,
            handleTransferEventUseCase,
            monitorOngoingActiveTransfersUseCase,
            areTransfersPausedUseCase,
            getActiveTransferTotalsUseCase,
            overQuotaNotificationBuilder,
            notificationManager,
            areNotificationsEnabledUseCase,
            correctActiveTransfersUseCase,
            clearActiveTransfersIfFinishedUseCase,
            chatUploadNotificationMapper,
            attachNodeWithPendingMessageUseCase,
            updatePendingMessageUseCase,
            checkFinishedChatUploadsUseCase,
            crashReporter,
            setForeground,
        )
    }

    @Test
    fun `test that crashReporter is invoked when the worker starts doing work`() =
        runTest {
            commonStub()
            underTest.doWork()
            verify(crashReporter, times(2)).log(any())
        }

    @Test
    fun `test that node is attached to chat once upload is finished`() = runTest {
        val finishEvent = commonStub()

        underTest.onTransferEventReceived(finishEvent)

        verify(attachNodeWithPendingMessageUseCase).invoke(PENDING_MSG_ID, NodeId(NODE_ID))
    }

    @Test
    fun `test that pending message is updated to error uploading if the upload fails`() = runTest {
        val finishEvent = commonStub(true)

        underTest.onTransferEventReceived(finishEvent)

        verify(updatePendingMessageUseCase).invoke(
            UpdatePendingMessageStateRequest(
                PENDING_MSG_ID,
                state = PendingMessageState.ERROR_UPLOADING
            )
        )
    }

    @Test
    fun `test that pending message is updated to error attaching if the attach fails`() = runTest {
        val finishEvent = commonStub()
        whenever(
            attachNodeWithPendingMessageUseCase(
                PENDING_MSG_ID,
                NodeId(NODE_ID),
            )
        ).thenThrow(RuntimeException::class.java)

        underTest.onTransferEventReceived(finishEvent)

        verify(updatePendingMessageUseCase).invoke(
            UpdatePendingMessageStateRequest(
                PENDING_MSG_ID,
                state = PendingMessageState.ERROR_ATTACHING
            )
        )
    }

    @Test
    fun `test that correctActiveTransfersUseCase is invoked when the worker starts doing work`() =
        runTest {
            commonStub()
            underTest.doWork()
            verify(correctActiveTransfersUseCase).invoke(TransferType.CHAT_UPLOAD)
        }

    @Test
    fun `test that checkFinishedChatUploadsUseCase is invoked when the worker starts doing work`() =
        runTest {
            commonStub()
            underTest.doWork()
            verify(checkFinishedChatUploadsUseCase).invoke()
        }

    private suspend fun commonStub(withError: Boolean = false): TransferEvent.TransferFinishEvent {
        val appData = TransferAppData.ChatUpload(PENDING_MSG_ID)
        val transfer = mock<Transfer> {
            on { this.appData } doReturn listOf(appData)
            on { this.nodeHandle } doReturn NODE_ID
        }
        val error = if (withError) mock<MegaException>() else null
        val finishEvent = mock<TransferEvent.TransferFinishEvent> {
            on { this.transfer } doReturn transfer
            on { this.error } doReturn error
        }
        val pendingMsg = mock<PendingMessage> {
            on { id } doReturn PENDING_MSG_ID
            on { this.chatId } doReturn CHAT_ID
        }
        whenever(chatMessageRepository.getPendingMessage(PENDING_MSG_ID)).thenReturn(pendingMsg)
        val totals = mock<ActiveTransferTotals> {
            on { transferredBytes }.thenReturn(100L)
            on { totalBytes }.thenReturn(200L)
            on { hasCompleted() }.thenReturn(false)
            on { hasOngoingTransfers() }.thenReturn(true)
        }
        whenever(monitorOngoingActiveTransfersUseCase(TransferType.CHAT_UPLOAD)) doReturn (flowOf(
            MonitorOngoingActiveTransfersResult(
                totals,
                paused = false,
                transfersOverQuota = false,
                storageOverQuota = false
            )
        ))
        whenever(monitorTransferEventsUseCase()) doReturn (emptyFlow())
        whenever(workProgressUpdater.updateProgress(any(), any(), any()))
            .thenReturn(SettableFuture.create<Void?>().also { it.set(null) })
        whenever(areNotificationsEnabledUseCase()).thenReturn(false)
        whenever(getActiveTransferTotalsUseCase(TransferType.CHAT_UPLOAD)).thenReturn(totals)
        return finishEvent
    }
}

private const val PENDING_MSG_ID = 16L
private const val CHAT_ID = 124L
private const val NODE_ID = 1353L