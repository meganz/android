package mega.privacy.android.data.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.ListenableWorker
import androidx.work.ProgressUpdater
import androidx.work.SystemClock
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.WorkDatabase
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.workDataOf
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.data.mapper.transfer.TransfersFinishedNotificationMapper
import mega.privacy.android.data.mapper.transfer.TransfersNotificationMapper
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUntilFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.SetNodeAttributesAfterUploadUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadsWorkerTest {

    private lateinit var underTest: UploadsWorker

    private val context = mock<Context>()
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase

    private val monitorTransferEventsUseCase = mock<MonitorTransferEventsUseCase>()
    private val handleTransferEventUseCase = mock<HandleTransferEventUseCase>()
    private val monitorOngoingActiveTransfersUntilFinishedUseCase =
        mock<MonitorOngoingActiveTransfersUntilFinishedUseCase>()
    private val areTransfersPausedUseCase = mock<AreTransfersPausedUseCase>()
    private val getActiveTransferTotalsUseCase = mock<GetActiveTransferTotalsUseCase>()
    private val transfersNotificationMapper = mock<TransfersNotificationMapper>()
    private val areNotificationsEnabledUseCase = mock<AreNotificationsEnabledUseCase>()
    private val correctActiveTransfersUseCase = mock<CorrectActiveTransfersUseCase>()
    private val overQuotaNotificationBuilder = mock<OverQuotaNotificationBuilder>()
    private val clearActiveTransfersIfFinishedUseCase =
        mock<ClearActiveTransfersIfFinishedUseCase>()
    private val transfersFinishedNotificationMapper = mock<TransfersFinishedNotificationMapper>()
    private val setNodeAttributesAfterUploadUseCase = mock<SetNodeAttributesAfterUploadUseCase>()
    private val workProgressUpdater = mock<ProgressUpdater>()
    private val setForeground = mock<ForegroundSetter>()
    private val crashReporter = mock<CrashReporter>()
    private val notificationManager = mock<NotificationManagerCompat>()

    private val nodeId = 1L
    private val localPath = "localPath"

    @BeforeAll
    fun setup() {
        val ioDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(ioDispatcher)
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase =
            WorkDatabase.create(context, workExecutor.serialTaskExecutor, SystemClock(), true)
        underTest = UploadsWorker(
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
            monitorTransferEventsUseCase = monitorTransferEventsUseCase,
            handleTransferEventUseCase = handleTransferEventUseCase,
            areTransfersPausedUseCase = areTransfersPausedUseCase,
            monitorOngoingActiveTransfersUntilFinishedUseCase = monitorOngoingActiveTransfersUntilFinishedUseCase,
            getActiveTransferTotalsUseCase = getActiveTransferTotalsUseCase,
            transfersNotificationMapper = transfersNotificationMapper,
            overQuotaNotificationBuilder = overQuotaNotificationBuilder,
            areNotificationsEnabledUseCase = areNotificationsEnabledUseCase,
            notificationManager = notificationManager,
            correctActiveTransfersUseCase = correctActiveTransfersUseCase,
            clearActiveTransfersIfFinishedUseCase = clearActiveTransfersIfFinishedUseCase,
            transfersFinishedNotificationMapper = transfersFinishedNotificationMapper,
            setNodeAttributesAfterUploadUseCase = setNodeAttributesAfterUploadUseCase,
            crashReporter = crashReporter,
            foregroundSetter = setForeground,
            notificationSamplePeriod = 0L,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            context,
            workProgressUpdater,
            handleTransferEventUseCase,
            areTransfersPausedUseCase,
            getActiveTransferTotalsUseCase,
            transfersNotificationMapper,
            overQuotaNotificationBuilder,
            areNotificationsEnabledUseCase,
            notificationManager,
            correctActiveTransfersUseCase,
            clearActiveTransfersIfFinishedUseCase,
            transfersFinishedNotificationMapper,
            setNodeAttributesAfterUploadUseCase,
            crashReporter,
            setForeground,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that monitorTransferEventsUseCase is invoked when the worker starts doing work`() =
        runTest {
            commonStub()
            underTest.doWork()
            verify(monitorTransferEventsUseCase, atLeastOnce()).invoke()
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
            verify(correctActiveTransfersUseCase).invoke(TransferType.GENERAL_UPLOAD)
        }

    @Test
    fun `test that monitorActiveTransferTotalsUseCase is invoked when the worker starts doing work`() =
        runTest {
            commonStub()
            underTest.doWork()
            verify(monitorOngoingActiveTransfersUntilFinishedUseCase).invoke(TransferType.GENERAL_UPLOAD)
        }

    @Test
    fun `test that correctActiveTransfersUseCase is called before start monitoring ongoing transfers`() =
        runTest {
            commonStub()
            val inOrder =
                inOrder(
                    monitorTransferEventsUseCase,
                    correctActiveTransfersUseCase,
                    monitorOngoingActiveTransfersUntilFinishedUseCase
                )
            underTest.doWork()
            inOrder.verify(correctActiveTransfersUseCase).invoke(TransferType.GENERAL_UPLOAD)
            inOrder.verify(monitorOngoingActiveTransfersUntilFinishedUseCase)
                .invoke(TransferType.GENERAL_UPLOAD)
        }


    @Test
    fun `test that worker finishes with success if last transfer is completed`() = runTest {
        val transferTotal = mockActiveTransferTotals(true)
        commonStub(transferTotals = listOf(transferTotal))
        Truth.assertThat(underTest.doWork()).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `test that worker finishes with failure if last transfer is not completed`() = runTest {
        val transferTotal = mockActiveTransferTotals(false)
        commonStub(transferTotals = listOf(transferTotal))
        Truth.assertThat(underTest.doWork()).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `test that notification is created when worker starts`() = runTest {
        val transferTotal: ActiveTransferTotals = mockActiveTransferTotals(true)
        commonStub(initialTransferTotals = transferTotal)
        underTest.doWork()
        verify(transfersNotificationMapper).invoke(transferTotal, false)
    }

    @Test
    fun `test that notification is updated when transfer totals are updated`() = runTest {
        val initial: ActiveTransferTotals = mockActiveTransferTotals(false)
        val transferTotals = (0..10).map {
            mockActiveTransferTotals(false)
        }.plus(mockActiveTransferTotals(true))
        commonStub(
            initialTransferTotals = initial,
            transferTotals = transferTotals,
        )
        underTest.doWork()
        verify(transfersNotificationMapper, atLeastOnce()).invoke(initial, false)
        transferTotals.forEach {
            verify(transfersNotificationMapper).invoke(it, false)
        }
    }

    @Test
    fun `test that overQuotaNotificationBuilder is invoked when transfers finishes with incomplete transfers and over quota true`() =
        runTest {
            val transferTotal = mockActiveTransferTotals(false)
            commonStub(transferTotals = listOf(transferTotal), storageOverQuota = true)
            underTest.doWork()
            verify(overQuotaNotificationBuilder).invoke(true)
            verifyNoInteractions(transfersFinishedNotificationMapper)
        }

    @Test
    fun `test that transfersFinishedNotificationMapper is invoked when transfers finishes with incomplete transfers and over quota false`() =
        runTest {
            val transferTotal = mockActiveTransferTotals(false)
            commonStub(transferTotals = listOf(transferTotal))
            underTest.doWork()
            verify(transfersFinishedNotificationMapper).invoke(transferTotal)
            verifyNoInteractions(overQuotaNotificationBuilder)
        }

    @Test
    fun `test that transfersFinishedNotificationMapper is invoked when transfer finishes with completed transfers`() =
        runTest {
            val transferTotal = mockActiveTransferTotals(true)
            whenever(transferTotal.totalTransfers).thenReturn(1)
            commonStub(transferTotals = listOf(transferTotal))
            underTest.doWork()
            verify(transfersFinishedNotificationMapper).invoke(transferTotal)
            verifyNoInteractions(overQuotaNotificationBuilder)
        }

    @Test
    fun `test that transfersFinishedNotificationMapper is invoked when worker starts with completed transfers`() =
        runTest {
            val initial: ActiveTransferTotals = mockActiveTransferTotals(true)
            whenever(initial.totalTransfers).thenReturn(1)
            commonStub(initialTransferTotals = initial, transferTotals = emptyList())
            underTest.doWork()
            verify(transfersFinishedNotificationMapper).invoke(initial)
        }

    @Test
    fun `test that setForeground is not invoked when worker starts with completed transfers`() =
        runTest {
            val initial: ActiveTransferTotals = mockActiveTransferTotals(true)
            commonStub(initialTransferTotals = initial)
            whenever(initial.totalTransfers).thenReturn(1)
            underTest.doWork()
            verifyNoInteractions(setForeground)
        }

    @Test
    fun `test that transfersFinishedNotificationMapper is not invoked when transfer finishes with no transfers`() =
        runTest {
            val transferTotal = mockActiveTransferTotals(true)
            whenever(transferTotal.totalTransfers).thenReturn(0)
            commonStub(transferTotals = listOf(transferTotal))
            underTest.doWork()
            verifyNoInteractions(transfersFinishedNotificationMapper)
            verifyNoInteractions(overQuotaNotificationBuilder)
        }

    @Test
    fun `test that clearActiveTransfersIfFinishedUseCase is invoked when transfers finishes`() =
        runTest {
            val transferTotal = mockActiveTransferTotals(true)
            commonStub(transferTotals = listOf(transferTotal))
            underTest.doWork()
            verify(clearActiveTransfersIfFinishedUseCase).invoke()
        }

    @Test
    fun `test that progress is set as work progress`() = runTest {
        val transferTotal = mockActiveTransferTotals(true)
        val expectedProgress = Progress(1f)
        whenever(transferTotal.transferProgress).thenReturn(expectedProgress)
        commonStub(initialTransferTotals = transferTotal)
        underTest.doWork()
        val expectedData =
            workDataOf(AbstractTransfersWorker.PROGRESS to expectedProgress.floatValue)
        verify(workProgressUpdater).updateProgress(eq(context), any(), eq(expectedData))
    }

    @Test
    fun `test that node attributes are set once upload is finished`() = runTest {
        val finishEvent = commonStub()

        underTest.onTransferEventReceived(finishEvent)

        verify(setNodeAttributesAfterUploadUseCase).invoke(nodeId, File(localPath))
    }

    @Test
    fun `test that node attributes are not set once upload is finished with error`() = runTest {
        val transfer: Transfer = mock {
            on { this.nodeHandle }.thenReturn(nodeId)
            on { this.localPath }.thenReturn(localPath)
            on { this.state }.thenReturn(TransferState.STATE_FAILED)
        }
        val transferEvent = TransferEvent.TransferFinishEvent(transfer, null)
        commonStub()

        underTest.onTransferEventReceived(transferEvent)

        verify(setNodeAttributesAfterUploadUseCase).invoke(nodeId, File(localPath))
    }

    private suspend fun commonStub(
        initialTransferTotals: ActiveTransferTotals = mockActiveTransferTotals(false),
        transferTotals: List<ActiveTransferTotals> = listOf(mockActiveTransferTotals(true)),
        storageOverQuota: Boolean = false,
    ): TransferEvent.TransferFinishEvent {
        val transfer: Transfer = mock {
            on { this.nodeHandle }.thenReturn(nodeId)
            on { this.localPath }.thenReturn(localPath)
        }
        val transferEvent = TransferEvent.TransferFinishEvent(transfer, null)
        whenever(areTransfersPausedUseCase())
            .thenReturn(false)
        whenever(monitorTransferEventsUseCase())
            .thenReturn(flowOf(transferEvent))
        whenever(monitorOngoingActiveTransfersUntilFinishedUseCase(TransferType.GENERAL_UPLOAD))
            .thenReturn(flow {
                emit(
                    MonitorOngoingActiveTransfersResult(
                        activeTransferTotals = initialTransferTotals,
                        paused = false,
                        transfersOverQuota = false,
                        storageOverQuota = false
                    )
                )
                transferTotals.forEach {
                    delay(AbstractTransfersWorker.ON_TRANSFER_UPDATE_REFRESH_MILLIS) // events are sampled in the worker
                    emit(
                        MonitorOngoingActiveTransfersResult(
                            activeTransferTotals = it,
                            paused = false,
                            transfersOverQuota = false,
                            storageOverQuota = storageOverQuota
                        )
                    )
                }
                delay(AbstractTransfersWorker.ON_TRANSFER_UPDATE_REFRESH_MILLIS + 10) //to be sure the last event is received
            })
        whenever(areNotificationsEnabledUseCase()).thenReturn(true)
        whenever(workProgressUpdater.updateProgress(any(), any(), any()))
            .thenReturn(SettableFuture.create<Void?>().also { it.set(null) })
        whenever(transfersNotificationMapper(any(), any())).thenReturn(mock())
        return transferEvent
    }

    private fun mockActiveTransferTotals(
        hasCompleted: Boolean,
        hasOngoing: Boolean = !hasCompleted,
    ) = mock<ActiveTransferTotals> {
        on { hasCompleted() }.thenReturn(hasCompleted)
        on { hasOngoingTransfers() }.thenReturn(hasOngoing)
    }
}