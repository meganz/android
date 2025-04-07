package mega.privacy.android.data.worker

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.DefaultWorkerFactory
import androidx.work.ProgressUpdater
import androidx.work.SystemClock
import androidx.work.WorkerParameters
import androidx.work.impl.WorkDatabase
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.workDataOf
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import mega.privacy.android.data.mapper.transfer.ChatUploadNotificationMapper
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.ChatCompressionProgress
import mega.privacy.android.domain.entity.transfer.ChatCompressionState
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData.ChatUpload
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.usecase.chat.message.CheckFinishedChatUploadsUseCase
import mega.privacy.android.domain.usecase.chat.message.MonitorPendingMessagesByStateUseCase
import mega.privacy.android.domain.usecase.chat.message.pendingmessages.CompressPendingMessagesUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.ClearPendingMessagesCompressionProgressUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.PrepareAllPendingMessagesUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.StartUploadingAllPendingMessagesUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatUploadsWorkerTest {
    private lateinit var underTest: ChatUploadsWorker


    private val context = mock<Context>()
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase

    private val workProgressUpdater = mock<ProgressUpdater>()

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
    private val checkFinishedChatUploadsUseCase = mock<CheckFinishedChatUploadsUseCase>()
    private val setForeground = mock<ForegroundSetter>()
    private val crashReporter = mock<CrashReporter>()
    private val compressPendingMessagesUseCase =
        mock<CompressPendingMessagesUseCase>()
    private val clearPendingMessagesCompressionProgressUseCase =
        mock<ClearPendingMessagesCompressionProgressUseCase>()
    private val startUploadingAllPendingMessagesUseCase =
        mock<StartUploadingAllPendingMessagesUseCase>()
    private val monitorPendingMessagesByStateUseCase = mock<MonitorPendingMessagesByStateUseCase>()
    private val prepareAllPendingMessagesUseCase = mock<PrepareAllPendingMessagesUseCase>()

    @BeforeAll
    fun init() {
        val ioDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(ioDispatcher)
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
                Dispatchers.Unconfined,
                workExecutor,
                DefaultWorkerFactory,
                workProgressUpdater,
                WorkForegroundUpdater(
                    workDatabase,
                    { _, _ -> }, workExecutor
                )
            ),
            ioDispatcher = ioDispatcher,
            areTransfersPausedUseCase = areTransfersPausedUseCase,
            getActiveTransferTotalsUseCase = getActiveTransferTotalsUseCase,
            overQuotaNotificationBuilder = overQuotaNotificationBuilder,
            notificationManager = notificationManager,
            areNotificationsEnabledUseCase = areNotificationsEnabledUseCase,
            correctActiveTransfersUseCase = correctActiveTransfersUseCase,
            clearActiveTransfersIfFinishedUseCase = clearActiveTransfersIfFinishedUseCase,
            chatUploadNotificationMapper = chatUploadNotificationMapper,
            checkFinishedChatUploadsUseCase = checkFinishedChatUploadsUseCase,
            compressPendingMessagesUseCase = compressPendingMessagesUseCase,
            monitorOngoingActiveTransfersUseCase = monitorOngoingActiveTransfersUseCase,
            clearPendingMessagesCompressionProgressUseCase = clearPendingMessagesCompressionProgressUseCase,
            startUploadingAllPendingMessagesUseCase = startUploadingAllPendingMessagesUseCase,
            monitorPendingMessagesByStateUseCase = monitorPendingMessagesByStateUseCase,
            prepareAllPendingMessagesUseCase = prepareAllPendingMessagesUseCase,
            crashReporter = crashReporter,
            foregroundSetter = setForeground,
            notificationSamplePeriod = 0L,
            loginMutex = mock()
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            context,
            workProgressUpdater,
            areTransfersPausedUseCase,
            getActiveTransferTotalsUseCase,
            overQuotaNotificationBuilder,
            areNotificationsEnabledUseCase,
            notificationManager,
            correctActiveTransfersUseCase,
            clearActiveTransfersIfFinishedUseCase,
            crashReporter,
            setForeground,
            monitorOngoingActiveTransfersUseCase,
            chatUploadNotificationMapper,
            checkFinishedChatUploadsUseCase,
            compressPendingMessagesUseCase,
            clearPendingMessagesCompressionProgressUseCase,
            startUploadingAllPendingMessagesUseCase,
            monitorPendingMessagesByStateUseCase,
            prepareAllPendingMessagesUseCase,
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

    @Test
    fun `test that doWork starts compressing pending messages`() = runTest {
        commonStub()

        underTest.doWork()
        this.advanceUntilIdle()

        verify(compressPendingMessagesUseCase).invoke()
    }

    @Test
    fun `test that doWork starts preparing all pending messages`() = runTest {
        commonStub()

        underTest.doWork()
        this.advanceUntilIdle()

        verify(prepareAllPendingMessagesUseCase).invoke()
    }

    @Test
    fun `test that doWork starts uploading pending messages`() = runTest {
        commonStub()

        underTest.doWork()
        this.advanceUntilIdle()

        verify(startUploadingAllPendingMessagesUseCase).invoke()
    }

    @Test
    fun `test that monitorOngoingActiveTransfers does complete if there are no ongoing transfers or pending messages`() =
        runTest {
            commonStub()
            val monitorOngoingActiveTransfersUseFlow = monitorOngoingActiveTransfersFlow(false)
            whenever(monitorPendingMessagesByStateUseCase(anyVararg())) doReturn
                    flowOf(emptyList())
            whenever(monitorOngoingActiveTransfersUseCase(TransferType.CHAT_UPLOAD)) doReturn
                    monitorOngoingActiveTransfersUseFlow

            underTest.consumeProgress().test {
                awaitItem() //first value
                awaitComplete()
            }
        }

    @Test
    fun `test that monitorProgress does not complete if there are ongoing transfers`() =
        runTest {
            val monitorOngoingActiveTransfersUseFlow = monitorOngoingActiveTransfersFlow(true)
            whenever(monitorPendingMessagesByStateUseCase(anyVararg())) doReturn
                    flowOf(emptyList())
            whenever(monitorOngoingActiveTransfersUseCase(TransferType.CHAT_UPLOAD)) doReturn
                    monitorOngoingActiveTransfersUseFlow

            underTest.monitorProgress().test {
                awaitItem()
                expectNoEvents()
            }
        }

    @Test
    fun `test that monitorOngoingActiveTransfers does not complete if there are ongoing pending messages`() =
        runTest {
            commonStub()
            val monitorOngoingActiveTransfersUseFlow = monitorOngoingActiveTransfersFlow(false)
            whenever(
                monitorPendingMessagesByStateUseCase(
                    PendingMessageState.PREPARING,
                    PendingMessageState.COMPRESSING,
                    PendingMessageState.READY_TO_UPLOAD,
                    PendingMessageState.UPLOADING,
                    PendingMessageState.ATTACHING,
                )
            ) doReturn flowOf(listOf(mock()))
            whenever(monitorOngoingActiveTransfersUseCase(TransferType.CHAT_UPLOAD)) doReturn
                    monitorOngoingActiveTransfersUseFlow

            underTest.monitorProgress().test {
                awaitItem()
                expectNoEvents()
            }
        }

    @Test
    fun `test that pending messages compression progress is cleared when the work is finished`() =
        runTest {
            commonStub()
            val monitorOngoingActiveTransfersUseFlow = monitorOngoingActiveTransfersFlow(false)
            whenever(monitorPendingMessagesByStateUseCase(anyVararg())) doReturn
                    flowOf(emptyList())
            whenever(monitorOngoingActiveTransfersUseCase(TransferType.CHAT_UPLOAD)) doReturn
                    monitorOngoingActiveTransfersUseFlow

            underTest.consumeProgress().test {
                awaitItem() //first value
                awaitComplete()
                verify(clearPendingMessagesCompressionProgressUseCase).invoke()
            }
        }

    @Test
    fun `test that notification is updated when compression progress is updated`() = runTest {
        commonStub()
        whenever(areNotificationsEnabledUseCase()).thenReturn(true)
        val firstNotification = mock<Notification>()
        val secondNotification = mock<Notification>()
        val firstCompressionProgress = ChatCompressionProgress(0, 1, Progress(0f))
        val secondCompressionProgress = ChatCompressionProgress(0, 1, Progress(0.5f))
        val compressionFlow = MutableStateFlow<ChatCompressionState>(firstCompressionProgress)
        whenever(monitorPendingMessagesByStateUseCase(anyVararg())) doReturn
                flowOf(listOf(mock()))
        whenever(compressPendingMessagesUseCase()) doReturn compressionFlow
        whenever(
            chatUploadNotificationMapper(anyOrNull(), eq(firstCompressionProgress), any())
        ) doReturn firstNotification
        whenever(
            chatUploadNotificationMapper(anyOrNull(), eq(secondCompressionProgress), any())
        ) doReturn secondNotification
        val workerJob = launch {
            underTest.doWork()
        }
        yield() //to wait for the doWork to start
        verify(
            notificationManager,
        ).notify(any(), eq(firstNotification))
        verifyNoMoreInteractions(notificationManager)

        compressionFlow.value = secondCompressionProgress

        verify(notificationManager).notify(any(), eq(secondNotification))

        workerJob.cancel()
    }

    private fun monitorOngoingActiveTransfersFlow(hasOngoingTransfers: Boolean): Flow<MonitorOngoingActiveTransfersResult> {
        val activeTransferTotals = mock<ActiveTransferTotals> {
            on { this.hasOngoingTransfers() } doReturn hasOngoingTransfers
        }
        return flow {
            emit(
                MonitorOngoingActiveTransfersResult(
                    activeTransferTotals,
                    paused = false,
                    transfersOverQuota = false,
                    storageOverQuota = false
                )
            )
            awaitCancellation()
        }
    }

    private suspend fun commonStub(withError: Boolean = false): TransferEvent.TransferFinishEvent {
        val appData = ChatUpload(PENDING_MSG_ID)
        val transfer = mock<Transfer> {
            on { this.appData } doReturn listOf(appData)
            on { this.nodeHandle } doReturn NODE_ID
        }
        val error = if (withError) mock<MegaException>() else null
        val finishEvent = mock<TransferEvent.TransferFinishEvent> {
            on { this.transfer } doReturn transfer
            on { this.error } doReturn error
        }
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
        whenever(workProgressUpdater.updateProgress(any(), any(), any()))
            .thenReturn(SettableFuture.create<Void?>().also { it.set(null) })
        whenever(areNotificationsEnabledUseCase()).thenReturn(false)
        whenever(getActiveTransferTotalsUseCase(TransferType.CHAT_UPLOAD)).thenReturn(totals)
        whenever(compressPendingMessagesUseCase()).thenReturn(emptyFlow())
        whenever(startUploadingAllPendingMessagesUseCase()).thenReturn(emptyFlow())
        whenever(prepareAllPendingMessagesUseCase()).thenReturn(emptyFlow())
        return finishEvent
    }
}

private const val PENDING_MSG_ID = 16L
private const val NODE_ID = 1353L