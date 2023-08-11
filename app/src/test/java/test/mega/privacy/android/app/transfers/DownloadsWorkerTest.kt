package test.mega.privacy.android.app.transfers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.WorkDatabase
import androidx.work.impl.foreground.ForegroundProcessor
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.WorkProgressUpdater
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.workDataOf
import com.google.common.truth.Truth.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.mapper.transfer.DownloadNotificationMapper
import mega.privacy.android.data.worker.AreNotificationsEnabledUseCase
import mega.privacy.android.data.worker.DownloadsWorker
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.transfer.MonitorPausedTransfersUseCase
import mega.privacy.android.domain.usecase.transfer.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfer.activetransfers.AddOrUpdateActiveTransferUseCase
import mega.privacy.android.domain.usecase.transfer.activetransfers.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfer.activetransfers.MonitorActiveTransferTotalsUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class DownloadsWorkerTest {
    private lateinit var underTest: DownloadsWorker

    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase

    private val monitorTransferEventsUseCase = mock<MonitorTransferEventsUseCase>()
    private val addOrUpdateActiveTransferUseCase = mock<AddOrUpdateActiveTransferUseCase>()
    private val monitorActiveTransferTotalsUseCase = mock<MonitorActiveTransferTotalsUseCase>()
    private val monitorPausedTransfersUseCase = mock<MonitorPausedTransfersUseCase>()
    private val getActiveTransferTotalsUseCase = mock<GetActiveTransferTotalsUseCase>()
    private val downloadNotificationMapper = mock<DownloadNotificationMapper>()
    private val areNotificationsEnabledUseCase = mock<AreNotificationsEnabledUseCase>()

    @Before
    fun setup() {
        val ioDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(ioDispatcher)
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase = WorkDatabase.create(context, workExecutor.serialTaskExecutor, true)
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
                workExecutor,
                WorkerFactory.getDefaultWorkerFactory(),
                WorkProgressUpdater(workDatabase, workExecutor),
                WorkForegroundUpdater(workDatabase, object : ForegroundProcessor {
                    override fun startForeground(
                        workSpecId: String,
                        foregroundInfo: ForegroundInfo,
                    ) {
                    }

                    override fun stopForeground(workSpecId: String) {}
                    override fun isEnqueuedInForeground(workSpecId: String): Boolean = true
                }, workExecutor)
            ),
            ioDispatcher = ioDispatcher,
            monitorTransferEventsUseCase = monitorTransferEventsUseCase,
            addOrUpdateActiveTransferUseCase = addOrUpdateActiveTransferUseCase,
            monitorActiveTransferTotalsUseCase = monitorActiveTransferTotalsUseCase,
            monitorPausedTransfersUseCase = monitorPausedTransfersUseCase,
            getActiveTransferTotalsUseCase = getActiveTransferTotalsUseCase,
            downloadNotificationMapper = downloadNotificationMapper,
            areNotificationsEnabledUseCase = areNotificationsEnabledUseCase,
            notificationManager = mock()
        )
    }


    @After
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
    fun `test that monitorActiveTransferTotalsUseCase is invoked when the worker starts doing work`() =
        runTest {
            commonStub()
            underTest.doWork()
            verify(monitorActiveTransferTotalsUseCase).invoke(TransferType.TYPE_DOWNLOAD)
        }

    @Test
    fun `test that worker finishes with success if last transfer is completed`() = runTest {
        val transferTotal = mockActiveTransferTotals(true)
        commonStub(transferTotal = transferTotal)
        assertThat(underTest.doWork()).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `test that worker finishes with failure if last transfer is not completed`() = runTest {
        val transferTotal = mockActiveTransferTotals(false)
        commonStub(transferTotal)
        assertThat(underTest.doWork()).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `test that notification is created when worker starts`() = runTest {
        val transferTotal: ActiveTransferTotals = mockActiveTransferTotals(true)
        commonStub(transferTotal)
        underTest.doWork()
        verify(downloadNotificationMapper).invoke(transferTotal, false)
    }

    @Test
    fun `test that notification is updated when transfers are paused`() = runTest {
        val initial: ActiveTransferTotals = mockActiveTransferTotals(false)
        val transferTotal: ActiveTransferTotals = mockActiveTransferTotals(true)
        commonStub(
            monitorPauseFlow = flowOf(false, true),
            initialTransferTotals = initial,
            transferTotals = listOf(transferTotal)
        )
        underTest.doWork()
        verify(downloadNotificationMapper, atLeastOnce()).invoke(initial, false)
        verify(downloadNotificationMapper).invoke(transferTotal, true)
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
        verify(downloadNotificationMapper, atLeastOnce()).invoke(initial, false)
        transferTotals.forEach {
            verify(downloadNotificationMapper).invoke(it, false)
        }
    }

    private suspend fun commonStub(transferTotal: ActiveTransferTotals) = commonStub(
        transferTotals = listOf(transferTotal)
    )

    private suspend fun commonStub(
        monitorPauseFlow: Flow<Boolean> = flowOf(false),
        initialTransferTotals: ActiveTransferTotals = mockActiveTransferTotals(false),
        transferTotals: List<ActiveTransferTotals> = listOf(mockActiveTransferTotals(true)),
    ) = runTest {
        val transfer: Transfer = mock()
        val transferEvent = TransferEvent.TransferFinishEvent(transfer, null)
        whenever(getActiveTransferTotalsUseCase(TransferType.TYPE_DOWNLOAD))
            .thenReturn(initialTransferTotals)
        whenever(monitorPausedTransfersUseCase())
            .thenReturn(monitorPauseFlow)
        whenever(monitorTransferEventsUseCase())
            .thenReturn(flowOf(transferEvent))
        whenever(monitorActiveTransferTotalsUseCase(TransferType.TYPE_DOWNLOAD))
            .thenReturn(flow {
                delay(100)//to be sure that other events are received
                transferTotals.forEach {
                    emit(it)
                }
            })
        whenever(areNotificationsEnabledUseCase()).thenReturn(true)
    }

    private fun mockActiveTransferTotals(
        hasCompleted: Boolean,
        hasOngoing: Boolean = !hasCompleted,
    ) = mock<ActiveTransferTotals> {
        on { hasCompleted() }.thenReturn(hasCompleted)
        on { hasOngoingTransfers() }.thenReturn(hasOngoing)
    }
}