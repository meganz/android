package mega.privacy.android.data.worker

import android.app.Notification
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
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.data.mapper.transfer.TransfersActionGroupFinishNotificationBuilder
import mega.privacy.android.data.mapper.transfer.TransfersActionGroupProgressNotificationBuilder
import mega.privacy.android.data.mapper.transfer.TransfersFinishNotificationSummaryBuilder
import mega.privacy.android.data.mapper.transfer.TransfersNotificationMapper
import mega.privacy.android.data.mapper.transfer.TransfersProgressNotificationSummaryBuilder
import mega.privacy.android.data.worker.AbstractTransfersWorker.Companion.NOTIFICATION_GROUP_MULTIPLAYER
import mega.privacy.android.data.worker.UploadsWorker.Companion.NOTIFICATION_UPLOAD_FINAL
import mega.privacy.android.data.worker.UploadsWorker.Companion.UPLOAD_NOTIFICATION_ID
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferProgressResult
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.usecase.transfers.MonitorActiveAndPendingTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.pending.StartAllPendingUploadsUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadsWorkerTest {

    private lateinit var underTest: UploadsWorker

    private val context = mock<Context>()
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase

    private val monitorActiveAndPendingTransfersUseCase =
        mock<MonitorActiveAndPendingTransfersUseCase>()
    private val areTransfersPausedUseCase = mock<AreTransfersPausedUseCase>()
    private val getActiveTransferTotalsUseCase = mock<GetActiveTransferTotalsUseCase>()
    private val transfersNotificationMapper = mock<TransfersNotificationMapper>()
    private val areNotificationsEnabledUseCase = mock<AreNotificationsEnabledUseCase>()
    private val correctActiveTransfersUseCase = mock<CorrectActiveTransfersUseCase>()
    private val overQuotaNotificationBuilder = mock<OverQuotaNotificationBuilder>()
    private val clearActiveTransfersIfFinishedUseCase =
        mock<ClearActiveTransfersIfFinishedUseCase>()
    private val workProgressUpdater = mock<ProgressUpdater>()
    private val setForeground = mock<ForegroundSetter>()
    private val crashReporter = mock<CrashReporter>()
    private val notificationManager = mock<NotificationManagerCompat>()
    private val startAllPendingUploadsUseCase = mock<StartAllPendingUploadsUseCase>()
    private val transfersProgressNotificationSummaryBuilder =
        mock<TransfersProgressNotificationSummaryBuilder>()
    private val transfersActionGroupProgressNotificationBuilder =
        mock<TransfersActionGroupProgressNotificationBuilder>()
    private val transfersFinishNotificationSummaryBuilder =
        mock<TransfersFinishNotificationSummaryBuilder>()
    private val transfersActionGroupFinishNotificationBuilder =
        mock<TransfersActionGroupFinishNotificationBuilder>()
    private val displayPathFromUriCache = mock<HashMap<String, String>>()

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
            monitorActiveAndPendingTransfersUseCase = monitorActiveAndPendingTransfersUseCase,
            getActiveTransferTotalsUseCase = getActiveTransferTotalsUseCase,
            transfersNotificationMapper = transfersNotificationMapper,
            overQuotaNotificationBuilder = overQuotaNotificationBuilder,
            areNotificationsEnabledUseCase = areNotificationsEnabledUseCase,
            notificationManager = notificationManager,
            correctActiveTransfersUseCase = correctActiveTransfersUseCase,
            clearActiveTransfersIfFinishedUseCase = clearActiveTransfersIfFinishedUseCase,
            crashReporter = crashReporter,
            foregroundSetter = setForeground,
            notificationSamplePeriod = 0L,
            startAllPendingUploadsUseCase = startAllPendingUploadsUseCase,
            loginMutex = mock(),
            transfersProgressNotificationSummaryBuilder = transfersProgressNotificationSummaryBuilder,
            transfersActionGroupProgressNotificationBuilder = transfersActionGroupProgressNotificationBuilder,
            transfersFinishNotificationSummaryBuilder = transfersFinishNotificationSummaryBuilder,
            transfersActionGroupFinishNotificationBuilder = transfersActionGroupFinishNotificationBuilder,
            displayPathFromUriCache = displayPathFromUriCache
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            context,
            workProgressUpdater,
            areTransfersPausedUseCase,
            getActiveTransferTotalsUseCase,
            transfersNotificationMapper,
            overQuotaNotificationBuilder,
            areNotificationsEnabledUseCase,
            notificationManager,
            correctActiveTransfersUseCase,
            clearActiveTransfersIfFinishedUseCase,
            crashReporter,
            setForeground,
            monitorActiveAndPendingTransfersUseCase,
            startAllPendingUploadsUseCase,
            transfersProgressNotificationSummaryBuilder,
            transfersActionGroupProgressNotificationBuilder,
            transfersFinishNotificationSummaryBuilder,
            transfersActionGroupFinishNotificationBuilder,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
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
            verify(monitorActiveAndPendingTransfersUseCase).invoke(TransferType.GENERAL_UPLOAD)
        }

    @Test
    fun `test that correctActiveTransfersUseCase is called before start monitoring ongoing transfers`() =
        runTest {
            commonStub()
            val inOrder =
                inOrder(
                    correctActiveTransfersUseCase,
                    monitorActiveAndPendingTransfersUseCase
                )
            underTest.doWork()
            inOrder.verify(correctActiveTransfersUseCase).invoke(TransferType.GENERAL_UPLOAD)
            inOrder.verify(monitorActiveAndPendingTransfersUseCase)
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
    fun `test that overQuotaNotificationBuilder is invoked when transfers finishes with incomplete transfers and over quota true`() =
        runTest {
            val transferTotal = mockActiveTransferTotals(false)
            commonStub(transferTotals = listOf(transferTotal), storageOverQuota = true)
            underTest.doWork()
            verify(overQuotaNotificationBuilder).invoke(true)
        }

    @Test
    fun `test that overQuotaNotificationBuilder is not invoked with incomplete transfers and over quota false`() =
        runTest {
            val transferTotal = mockActiveTransferTotals(false)
            commonStub(transferTotals = listOf(transferTotal))
            underTest.doWork()
            verifyNoInteractions(overQuotaNotificationBuilder)
        }

    @Test
    fun `test that overQuotaNotificationBuilder is not invoked when transfer finishes with completed transfers`() =
        runTest {
            val transferTotal = mockActiveTransferTotals(true)
            whenever(transferTotal.totalTransfers).thenReturn(1)
            commonStub(transferTotals = listOf(transferTotal))
            underTest.doWork()
            verifyNoInteractions(overQuotaNotificationBuilder)
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
    fun `test that overQuotaNotificationBuilder is not invoked when transfer finishes with no transfers`() =
        runTest {
            val transferTotal = mockActiveTransferTotals(true)
            whenever(transferTotal.totalTransfers).thenReturn(0)
            commonStub(transferTotals = listOf(transferTotal))
            underTest.doWork()
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
    fun `test that startAllPendingDownloadsUseCase is invoked when work is started`() = runTest {
        commonStub()
        underTest.doWork()
        verify(startAllPendingUploadsUseCase).invoke()
    }

    @Test
    fun `test that notification group is updated when transfer totals are updated`() = runTest {
        val groupId = 2135
        val transferTotals = (0..10).map {
            mockActiveTransferTotals(
                false,
                actionGroups = listOf(mockActionGroup(groupId, transferredBytes = it.toLong()))
            )
        }
        commonStub(
            initialTransferTotals = transferTotals.first(),
            transferTotals = transferTotals.drop(1),
        )
        val notifications = transferTotals.map {
            val notification = mock<Notification>()
            whenever(
                transfersActionGroupProgressNotificationBuilder(
                    it.actionGroups.first(),
                    TransferType.GENERAL_UPLOAD,
                    false
                )
            ) doReturn notification
            notification
        }

        underTest.doWork()

        verify(
            transfersProgressNotificationSummaryBuilder,
            atLeastOnce()
        )(TransferType.GENERAL_UPLOAD)
        notifications.forEach {
            val groupNotificationId =
                NOTIFICATION_GROUP_MULTIPLAYER * UPLOAD_NOTIFICATION_ID + groupId
            verify(notificationManager).notify(groupNotificationId, it)
        }
    }

    @Test
    fun `test that notification finish group is notified when transfer group finishes`() =
        runTest {
            val groupId = 675367
            val initial = mockActiveTransferTotals(
                false,
                actionGroups = listOf(mockActionGroup(groupId))
            )
            val final = mockActiveTransferTotals(
                true,
                actionGroups = listOf(mockActionGroup(groupId, finished = true))
            )
            commonStub(
                initialTransferTotals = initial,
                transferTotals = listOf(final),
            )
            val notification = mock<Notification>()
            whenever(
                transfersActionGroupFinishNotificationBuilder(
                    final.actionGroups.first(),
                    TransferType.GENERAL_UPLOAD,
                )
            ) doReturn notification

            underTest.doWork()

            verify(transfersFinishNotificationSummaryBuilder, atLeastOnce())
                .invoke(TransferType.GENERAL_UPLOAD)
            val groupNotificationId =
                NOTIFICATION_GROUP_MULTIPLAYER * NOTIFICATION_UPLOAD_FINAL + groupId
            verify(notificationManager).notify(groupNotificationId, notification)
        }

    private suspend fun commonStub(
        initialTransferTotals: ActiveTransferTotals = mockActiveTransferTotals(false),
        transferTotals: List<ActiveTransferTotals> = listOf(mockActiveTransferTotals(true)),
        storageOverQuota: Boolean = false,
        appData: List<TransferAppData>? = emptyList(),
    ): TransferEvent.TransferFinishEvent {
        val transfer: Transfer = mock {
            on { this.nodeHandle }.thenReturn(nodeId)
            on { this.localPath }.thenReturn(localPath)
            on { this.appData }.thenReturn(appData)
        }
        val transferEvent = TransferEvent.TransferFinishEvent(transfer, null)
        whenever(areTransfersPausedUseCase())
            .thenReturn(false)
        whenever(monitorActiveAndPendingTransfersUseCase(TransferType.GENERAL_UPLOAD))
            .thenReturn(flow {
                emit(
                    TransferProgressResult(
                        MonitorOngoingActiveTransfersResult(
                            activeTransferTotals = initialTransferTotals,
                            paused = false,
                            transfersOverQuota = false,
                            storageOverQuota = false
                        ),
                        pendingTransfers = false,
                        ongoingTransfers = !initialTransferTotals.hasCompleted(),
                    )
                )
                transferTotals.forEach {
                    delay(AbstractTransfersWorker.ON_TRANSFER_UPDATE_REFRESH_MILLIS) // events are sampled in the worker
                    emit(
                        TransferProgressResult(
                            MonitorOngoingActiveTransfersResult(
                                activeTransferTotals = it,
                                paused = false,
                                transfersOverQuota = false,
                                storageOverQuota = storageOverQuota
                            ),
                            pendingTransfers = false,
                            ongoingTransfers = !it.hasCompleted(),
                        )
                    )
                }
                delay(AbstractTransfersWorker.ON_TRANSFER_UPDATE_REFRESH_MILLIS + 10) //to be sure the last event is received
            })
        whenever(areNotificationsEnabledUseCase()).thenReturn(true)
        whenever(workProgressUpdater.updateProgress(any(), any(), any()))
            .thenReturn(SettableFuture.create<Void?>().also { it.set(null) })
        whenever(transfersNotificationMapper(any(), any())).thenReturn(mock())
        whenever(transfersProgressNotificationSummaryBuilder(TransferType.GENERAL_UPLOAD))
            .thenReturn(mock())
        whenever(transfersFinishNotificationSummaryBuilder(TransferType.GENERAL_UPLOAD))
            .thenReturn(mock())
        return transferEvent
    }

    private fun mockActiveTransferTotals(
        hasCompleted: Boolean,
        hasOngoing: Boolean = !hasCompleted,
        actionGroups: List<ActiveTransferTotals.ActionGroup> = emptyList(),
    ) = mock<ActiveTransferTotals> {
        on { hasCompleted() }.thenReturn(hasCompleted)
        on { hasOngoingTransfers() }.thenReturn(hasOngoing)
        on { this.actionGroups }.thenReturn(actionGroups)
    }

    private fun mockActionGroup(
        groupId: Int,
        totalBytes: Long = 4857L,
        transferredBytes: Long = 0L,
        finished: Boolean = false,
    ) = mock<ActiveTransferTotals.ActionGroup> {
        on { this.groupId } doReturn groupId
        on { this.totalBytes } doReturn totalBytes
        on { this.transferredBytes } doReturn transferredBytes
        on { this.finished() } doReturn finished
    }
}