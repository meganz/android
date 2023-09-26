package mega.privacy.android.app.main.managerSections

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.GetFailedOrCanceledTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.GetInProgressTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorFailedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferBeforeByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferToFirstByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferToLastByTagUseCase
import mega.privacy.android.domain.usecase.transfers.completed.DeleteCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.completed.GetAllCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.completed.MonitorCompletedTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransferByTagUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigInteger

@ExperimentalCoroutinesApi
internal class TransfersViewModelTest {
    private lateinit var underTest: TransfersViewModel
    private val transfersManagement: TransfersManagement = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val monitorFailedTransferUseCase: MonitorFailedTransferUseCase = mock()
    private val moveTransferBeforeByTagUseCase: MoveTransferBeforeByTagUseCase = mock()
    private val moveTransferToFirstByTagUseCase: MoveTransferToFirstByTagUseCase = mock()
    private val moveTransferToLastByTagUseCase: MoveTransferToLastByTagUseCase = mock()
    private val getTransferByTagUseCase: GetTransferByTagUseCase = mock()
    private val getInProgressTransfersUseCase: GetInProgressTransfersUseCase = mock()
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase = mock()
    private val monitorCompletedTransferEventUseCase: MonitorCompletedTransferEventUseCase = mock()
    private val getAllCompletedTransfersUseCase: GetAllCompletedTransfersUseCase = mock()
    private val getFailedOrCanceledTransfersUseCase: GetFailedOrCanceledTransfersUseCase = mock()
    private val deleteCompletedTransferUseCase: DeleteCompletedTransferUseCase = mock()
    private val pauseTransferByTagUseCase: PauseTransferByTagUseCase = mock()
    private val cancelTransferByTagUseCase: CancelTransferByTagUseCase = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        initViewModel()
    }

    private fun initViewModel() {
        underTest = TransfersViewModel(
            transfersManagement = transfersManagement,
            ioDispatcher = ioDispatcher,
            monitorFailedTransferUseCase = monitorFailedTransferUseCase,
            moveTransferBeforeByTagUseCase = moveTransferBeforeByTagUseCase,
            moveTransferToFirstByTagUseCase = moveTransferToFirstByTagUseCase,
            moveTransferToLastByTagUseCase = moveTransferToLastByTagUseCase,
            getTransferByTagUseCase = getTransferByTagUseCase,
            getInProgressTransfersUseCase = getInProgressTransfersUseCase,
            getAllCompletedTransfersUseCase = getAllCompletedTransfersUseCase,
            monitorTransferEventsUseCase = monitorTransferEventsUseCase,
            monitorCompletedTransferEventUseCase = monitorCompletedTransferEventUseCase,
            getFailedOrCanceledTransfersUseCase = getFailedOrCanceledTransfersUseCase,
            deleteCompletedTransferUseCase = deleteCompletedTransferUseCase,
            pauseTransferByTagUseCase = pauseTransferByTagUseCase,
            cancelTransferByTagUseCase = cancelTransferByTagUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that moveTransfer invoke moveTransferToFirstByTagUseCase success when pass newPosition as 0`() =
        runTest {
            val transferTag = 1
            val transfer = mock<Transfer> {
                on { tag }.thenReturn(transferTag)
            }
            whenever(getInProgressTransfersUseCase.invoke()).thenReturn(emptyList())
            whenever(moveTransferToFirstByTagUseCase.invoke(transferTag)).thenReturn(Unit)
            underTest.getAllActiveTransfers()
            underTest.moveTransfer(transfer, 0)
            advanceUntilIdle()
            verify(getTransferByTagUseCase, times(1)).invoke(transferTag)
        }

    @Test
    fun `test that moveTransfer invoke moveTransferToLastByTagUseCase success when pass newPosition as 0`() =
        runTest {
            val transfers = mutableListOf<Transfer>()
            for (i in 1..5) {
                val transfer = mock<Transfer> {
                    on { tag }.thenReturn(i)
                    on { isStreamingTransfer }.thenReturn(false)
                    on { appData }.thenReturn(emptyList())
                    on { priority }.thenReturn(BigInteger.valueOf(i.toLong()))
                    on { state }.thenReturn(TransferState.STATE_COMPLETED)
                }
                transfers.add(transfer)
                whenever(getTransferByTagUseCase(i)).thenReturn(transfer)
            }
            whenever(getInProgressTransfersUseCase.invoke()).thenReturn(transfers)
            whenever(moveTransferToLastByTagUseCase.invoke(any())).thenReturn(Unit)
            underTest.getAllActiveTransfers()
            underTest.moveTransfer(transfers.first(), transfers.lastIndex)
            advanceUntilIdle()
            verify(moveTransferToLastByTagUseCase, times(1)).invoke(any())
            verify(getTransferByTagUseCase, times(1)).invoke(transfers.first().tag)
            underTest.activeState.test {
                assertThat(awaitItem())
                    .isInstanceOf(ActiveTransfersState.TransferMovementFinishedUpdated::class.java)
            }
        }

    @Test
    fun `test that moveTransfer invoke moveTransferBeforeByTagUseCase success when pass newPosition in the middle of the list`() =
        runTest {
            val transfers = mutableListOf<Transfer>()
            for (i in 1..5) {
                val transfer = mock<Transfer> {
                    on { tag }.thenReturn(i)
                    on { isStreamingTransfer }.thenReturn(false)
                    on { appData }.thenReturn(emptyList())
                    on { priority }.thenReturn(BigInteger.valueOf(i.toLong()))
                    on { state }.thenReturn(TransferState.STATE_COMPLETED)
                }
                transfers.add(transfer)
                whenever(getTransferByTagUseCase(i)).thenReturn(transfer)
            }
            whenever(getInProgressTransfersUseCase.invoke()).thenReturn(transfers)
            whenever(moveTransferBeforeByTagUseCase.invoke(any(), any())).thenReturn(Unit)
            underTest.getAllActiveTransfers()
            underTest.moveTransfer(transfers.first(), 2)
            advanceUntilIdle()
            verify(moveTransferBeforeByTagUseCase, times(1)).invoke(any(), any())
            verify(getTransferByTagUseCase, times(1)).invoke(transfers.first().tag)
            underTest.activeState.test {
                assertThat(awaitItem())
                    .isInstanceOf(ActiveTransfersState.TransferMovementFinishedUpdated::class.java)
            }
        }

    @Test
    fun `test that completedTransfers update correctly when getAllCompletedTransfersUseCase returns value`() =
        runTest {
            val completedTransfer = mock<CompletedTransfer>()
            val completedTransfers = listOf(completedTransfer)
            whenever(getAllCompletedTransfersUseCase(DatabaseHandler.MAX_TRANSFERS)).thenReturn(
                flowOf(completedTransfers)
            )
            initViewModel()
            advanceUntilIdle()
            underTest.completedTransfers.test {
                assertThat(awaitItem()).isEqualTo(completedTransfers)
            }
        }

    @Test
    fun `test that deleteCompletedTransferUseCase invoke success when deleteCompletedTransfer is called`() =
        runTest {
            val transfer = mock<CompletedTransfer>()
            underTest.completedTransferRemoved(transfer, false)
            advanceUntilIdle()
            verify(deleteCompletedTransferUseCase).invoke(transfer, false)
        }

    @Test
    fun `test that pauseOrResumeTransferResult update correctly when call pauseTransferByTagUseCase success`() =
        runTest {
            val transfer = mock<Transfer> {
                on { tag }.thenReturn(1)
            }
            whenever(
                pauseTransferByTagUseCase.invoke(
                    transfer.tag,
                    true
                )
            ).thenReturn(true)
            underTest.pauseOrResumeTransfer(transfer)
            advanceUntilIdle()
            underTest.uiState.test {
                val newItem = awaitItem()
                assertThat(newItem.pauseOrResumeTransferResult?.isSuccess).isTrue()
                assertThat(newItem.pauseOrResumeTransferResult?.getOrThrow()).isEqualTo(transfer)
            }
        }

    @Test
    fun `test that pauseOrResumeTransferResult update correctly when call pauseTransferByTagUseCase failed`() =
        runTest {
            val transfer = mock<Transfer> {
                on { tag }.thenReturn(1)
            }
            whenever(
                pauseTransferByTagUseCase.invoke(
                    transfer.tag,
                    false
                )
            ).thenThrow(RuntimeException::class.java)
            underTest.pauseOrResumeTransfer(transfer)
            advanceUntilIdle()
            underTest.uiState.test {
                val newItem = awaitItem()
                assertThat(newItem.pauseOrResumeTransferResult?.isFailure).isTrue()
            }
        }

    @Test
    fun `test that pauseOrResumeTransferResult update correctly when call markHandledPauseOrResumeTransferResult`() =
        runTest {
            underTest.markHandledPauseOrResumeTransferResult()
            underTest.uiState.test {
                val newItem = awaitItem()
                assertThat(newItem.pauseOrResumeTransferResult).isNull()
            }
        }

    @Test
    fun `test that cancelTransferResult update correctly when call cancelTransfersByTag success`() =
        runTest {
            val tags = listOf(1, 2, 3, 4)
            whenever(cancelTransferByTagUseCase(any())).thenReturn(Unit)
            underTest.cancelTransfersByTag(tags)
            advanceUntilIdle()
            underTest.uiState.test {
                val newItem = awaitItem()
                assertThat(newItem.cancelTransfersResult?.isSuccess).isTrue()
            }
        }

    @Test
    fun `test that cancelTransferResult update correctly when call cancelTransfersByTag failed`() =
        runTest {
            val tags = listOf(1, 2, 3, 4)
            tags.take(3).forEach { tag ->
                whenever(cancelTransferByTagUseCase(tag)).thenReturn(Unit)
            }
            whenever(cancelTransferByTagUseCase(4)).thenThrow(RuntimeException::class.java)
            underTest.cancelTransfersByTag(tags)
            advanceUntilIdle()
            underTest.uiState.test {
                val newItem = awaitItem()
                assertThat(newItem.cancelTransfersResult?.isFailure).isTrue()
            }
        }

    @Test
    fun `test that cancelTransferResult update correctly when call markHandledCancelTransfersResult`() =
        runTest {
            underTest.markHandledCancelTransfersResult()
            underTest.uiState.test {
                val newItem = awaitItem()
                assertThat(newItem.cancelTransfersResult).isNull()
            }
        }
}
