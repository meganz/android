package mega.privacy.android.data.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.DefaultWorkerFactory
import androidx.work.ListenableWorker
import androidx.work.ProgressUpdater
import androidx.work.SystemClock
import androidx.work.WorkerParameters
import androidx.work.impl.WorkDatabase
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.workDataOf
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.data.mapper.transfer.TransfersFinishedNotificationMapper
import mega.privacy.android.data.mapper.transfer.TransfersNotificationMapper
import mega.privacy.android.data.worker.AbstractTransfersWorker.Companion.ON_TRANSFER_UPDATE_REFRESH_MILLIS
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.usecase.qrcode.ScanMediaFileUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.pending.GetPendingTransfersByTypeUseCase
import mega.privacy.android.domain.usecase.transfers.pending.StartAllPendingDownloadsUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadsWorkerTest {
    private lateinit var underTest: DownloadsWorker

    private val context = mock<Context>()
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase

    private val monitorTransferEventsUseCase = mock<MonitorTransferEventsUseCase>()
    private val handleTransferEventUseCase = mock<HandleTransferEventUseCase>()
    private val areTransfersPausedUseCase = mock<AreTransfersPausedUseCase>()
    private val getActiveTransferTotalsUseCase = mock<GetActiveTransferTotalsUseCase>()
    private val transfersNotificationMapper = mock<TransfersNotificationMapper>()
    private val areNotificationsEnabledUseCase = mock<AreNotificationsEnabledUseCase>()
    private val correctActiveTransfersUseCase = mock<CorrectActiveTransfersUseCase>()
    private val overQuotaNotificationBuilder = mock<OverQuotaNotificationBuilder>()
    private val clearActiveTransfersIfFinishedUseCase =
        mock<ClearActiveTransfersIfFinishedUseCase>()
    private val transfersFinishedNotificationMapper = mock<TransfersFinishedNotificationMapper>()
    private val workProgressUpdater = mock<ProgressUpdater>()
    private val scanMediaFileUseCase = mock<ScanMediaFileUseCase>()
    private val setForeground = mock<ForegroundSetter>()
    private val crashReporter = mock<CrashReporter>()
    private val notificationManager = mock<NotificationManagerCompat>()
    private val getPendingTransfersByTypeUseCase = mock<GetPendingTransfersByTypeUseCase>()
    private val monitorOngoingActiveTransfersUseCase = mock<MonitorOngoingActiveTransfersUseCase>()
    private val startAllPendingDownloadsUseCase = mock<StartAllPendingDownloadsUseCase>()

    @BeforeAll
    fun setup() {
        val ioDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(ioDispatcher)
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase =
            WorkDatabase.create(context, workExecutor.serialTaskExecutor, SystemClock(), true)
        underTest = DownloadsWorker(
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
            monitorTransferEventsUseCase = monitorTransferEventsUseCase,
            handleTransferEventUseCase = handleTransferEventUseCase,
            areTransfersPausedUseCase = areTransfersPausedUseCase,
            getActiveTransferTotalsUseCase = getActiveTransferTotalsUseCase,
            transfersNotificationMapper = transfersNotificationMapper,
            overQuotaNotificationBuilder = overQuotaNotificationBuilder,
            areNotificationsEnabledUseCase = areNotificationsEnabledUseCase,
            notificationManager = notificationManager,
            correctActiveTransfersUseCase = correctActiveTransfersUseCase,
            clearActiveTransfersIfFinishedUseCase = clearActiveTransfersIfFinishedUseCase,
            transfersFinishedNotificationMapper = transfersFinishedNotificationMapper,
            scanMediaFileUseCase = scanMediaFileUseCase,
            crashReporter = crashReporter,
            foregroundSetter = setForeground,
            notificationSamplePeriod = 0L,
            getPendingTransfersByTypeUseCase = getPendingTransfersByTypeUseCase,
            monitorOngoingActiveTransfersUseCase = monitorOngoingActiveTransfersUseCase,
            startAllPendingDownloadsUseCase = startAllPendingDownloadsUseCase,
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
            crashReporter,
            setForeground,
            monitorTransferEventsUseCase,
            getPendingTransfersByTypeUseCase,
            monitorOngoingActiveTransfersUseCase,
            startAllPendingDownloadsUseCase,
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
            verify(monitorTransferEventsUseCase).invoke()
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
            verify(correctActiveTransfersUseCase).invoke(TransferType.DOWNLOAD)
        }

    @Test
    fun `test that monitorActiveTransferTotalsUseCase is invoked when the worker starts doing work`() =
        runTest {
            commonStub()
            underTest.doWork()
            verify(monitorOngoingActiveTransfersUseCase).invoke(TransferType.DOWNLOAD)
        }

    @Test
    fun `test that correctActiveTransfersUseCase is called before start monitoring ongoing transfers`() =
        runTest {
            commonStub()
            val inOrder =
                inOrder(
                    monitorTransferEventsUseCase,
                    correctActiveTransfersUseCase,
                    monitorOngoingActiveTransfersUseCase
                )
            underTest.doWork()
            inOrder.verify(correctActiveTransfersUseCase).invoke(TransferType.DOWNLOAD)
            inOrder.verify(monitorOngoingActiveTransfersUseCase)
                .invoke(TransferType.DOWNLOAD)
        }


    @Test
    fun `test that worker finishes with success if last transfer is completed`() = runTest {
        val transferTotal = mockActiveTransferTotals(true)
        commonStub(transferTotals = listOf(transferTotal))
        assertThat(underTest.doWork()).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `test that worker finishes with failure if last transfer is not completed`() = runTest {
        val transferTotal = mockActiveTransferTotals(false)
        commonStub(transferTotals = listOf(transferTotal))
        assertThat(underTest.doWork()).isEqualTo(ListenableWorker.Result.failure())
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
            commonStub(transferTotals = listOf(transferTotal), transferOverQuota = true)
            underTest.doWork()
            verify(overQuotaNotificationBuilder).invoke(false)
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
    fun `test that file is scanned by scanMediaFileUseCase once download is finished`() = runTest {
        val localPath = "localPath"
        val transfer = mock<Transfer> {
            on { this.localPath } doReturn localPath
        }
        val finishEvent = mock<TransferEvent.TransferFinishEvent> {
            on { this.transfer } doReturn transfer
        }

        underTest.onTransferEventReceived(finishEvent)
        verify(scanMediaFileUseCase).invoke(arrayOf(localPath), arrayOf(""))
    }

    @Test
    fun `test that startAllPendingDownloadsUseCase is invoked when work is started`() = runTest {
        commonStub()
        underTest.doWork()
        verify(startAllPendingDownloadsUseCase).invoke()
    }

    @Test
    fun `test that consumeProgress does complete if there are no ongoing transfers or pending transfers`() =
        runTest {
            commonStub()
            val monitorOngoingActiveTransfersUseFlow = monitorOngoingActiveTransfersFlow(false)
            whenever(getPendingTransfersByTypeUseCase(TransferType.DOWNLOAD)) doReturn
                    flowOf(emptyList())
            whenever(monitorOngoingActiveTransfersUseCase(TransferType.DOWNLOAD)) doReturn
                    monitorOngoingActiveTransfersUseFlow

            underTest.consumeProgress().test {
                awaitItem() //first value
                awaitComplete()
            }
        }

    @Test
    fun `test that consumeProgress only completes when there are no pending transfers for more than sample duration`() =
        runTest {
            commonStub()
            val monitorOngoingActiveTransfersUseFlow = monitorOngoingActiveTransfersFlow(false)
            whenever(getPendingTransfersByTypeUseCase(TransferType.DOWNLOAD)) doReturn
                    flow {
                        emit(emptyList())
                        delay(ON_TRANSFER_UPDATE_REFRESH_MILLIS / 2)
                        emit(listOf(mock())) //second value
                        emit(emptyList())
                        delay(ON_TRANSFER_UPDATE_REFRESH_MILLIS + 100)
                        emit(listOf(mock())) //third value
                    }
            whenever(monitorOngoingActiveTransfersUseCase(TransferType.DOWNLOAD)) doReturn
                    monitorOngoingActiveTransfersUseFlow

            underTest.consumeProgress().test {
                awaitItem() //first value
                awaitItem() //second value
                awaitComplete() //third value not received
            }
        }

    @Test
    fun `test that monitorProgress does not complete if there are ongoing transfers`() =
        runTest {
            commonStub()
            val monitorOngoingActiveTransfersUseFlow = monitorOngoingActiveTransfersFlow(true)
            whenever(getPendingTransfersByTypeUseCase(TransferType.DOWNLOAD)) doReturn
                    flowOf(emptyList())
            whenever(monitorOngoingActiveTransfersUseCase(TransferType.DOWNLOAD)) doReturn
                    monitorOngoingActiveTransfersUseFlow

            underTest.monitorProgress().test {
                awaitItem()
                expectNoEvents()
            }
        }

    @Test
    fun `test that monitorProgress does not complete if there are pending transfers`() =
        runTest {
            commonStub()
            val monitorOngoingActiveTransfersUseFlow = monitorOngoingActiveTransfersFlow(false)
            whenever(getPendingTransfersByTypeUseCase(TransferType.DOWNLOAD)) doReturn
                    flowOf(listOf(mock()))
            whenever(monitorOngoingActiveTransfersUseCase(TransferType.DOWNLOAD)) doReturn
                    monitorOngoingActiveTransfersUseFlow

            underTest.monitorProgress().test {
                awaitItem()
                expectNoEvents()
            }
        }


    private suspend fun commonStub(
        initialTransferTotals: ActiveTransferTotals = mockActiveTransferTotals(false),
        transferTotals: List<ActiveTransferTotals> = listOf(mockActiveTransferTotals(true)),
        transferOverQuota: Boolean = false,
    ) = runTest {
        val transfer: Transfer = mock()
        val transferEvent = TransferEvent.TransferFinishEvent(transfer, null)
        whenever(areTransfersPausedUseCase())
            .thenReturn(false)
        whenever(monitorTransferEventsUseCase())
            .thenReturn(flowOf(transferEvent))
        whenever(monitorOngoingActiveTransfersUseCase(TransferType.DOWNLOAD))
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
                    delay(ON_TRANSFER_UPDATE_REFRESH_MILLIS) // events are sampled in the worker
                    emit(
                        MonitorOngoingActiveTransfersResult(
                            activeTransferTotals = it,
                            paused = false,
                            transfersOverQuota = transferOverQuota,
                            storageOverQuota = false
                        )
                    )
                }
                delay(ON_TRANSFER_UPDATE_REFRESH_MILLIS + 10) //to be sure the last event is received
            })
        whenever(areNotificationsEnabledUseCase()).thenReturn(true)
        whenever(workProgressUpdater.updateProgress(any(), any(), any()))
            .thenReturn(SettableFuture.create<Void?>().also { it.set(null) })
        whenever(transfersNotificationMapper(any(), any())).thenReturn(mock())
        whenever(getPendingTransfersByTypeUseCase(TransferType.DOWNLOAD))
            .thenReturn(flowOf(emptyList()))
    }

    private fun mockActiveTransferTotals(
        hasCompleted: Boolean,
        hasOngoing: Boolean = !hasCompleted,
    ) = mock<ActiveTransferTotals> {
        on { hasCompleted() }.thenReturn(hasCompleted)
        on { hasOngoingTransfers() }.thenReturn(hasOngoing)
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
}
