package mega.privacy.android.app.main.managerSections

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.LegacyDatabaseHandler
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.usecase.transfer.GetInProgressTransfersUseCase
import mega.privacy.android.domain.usecase.transfer.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfer.MonitorCompletedTransferEventUseCase
import mega.privacy.android.domain.usecase.transfer.MonitorFailedTransfer
import mega.privacy.android.domain.usecase.transfer.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfer.MoveTransferBeforeByTagUseCase
import mega.privacy.android.domain.usecase.transfer.MoveTransferToFirstByTagUseCase
import mega.privacy.android.domain.usecase.transfer.MoveTransferToLastByTagUseCase
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
    private val dbH: LegacyDatabaseHandler = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val monitorFailedTransfer: MonitorFailedTransfer = mock()
    private val moveTransferBeforeByTagUseCase: MoveTransferBeforeByTagUseCase = mock()
    private val moveTransferToFirstByTagUseCase: MoveTransferToFirstByTagUseCase = mock()
    private val moveTransferToLastByTagUseCase: MoveTransferToLastByTagUseCase = mock()
    private val getTransferByTagUseCase: GetTransferByTagUseCase = mock()
    private val getInProgressTransfersUseCase: GetInProgressTransfersUseCase = mock()
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase = mock()
    private val monitorCompletedTransferEventUseCase: MonitorCompletedTransferEventUseCase = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        initViewModel()
    }

    private fun initViewModel() {
        underTest = TransfersViewModel(
            transfersManagement = transfersManagement,
            dbH = dbH,
            ioDispatcher = ioDispatcher,
            monitorFailedTransfer = monitorFailedTransfer,
            moveTransferBeforeByTagUseCase = moveTransferBeforeByTagUseCase,
            moveTransferToFirstByTagUseCase = moveTransferToFirstByTagUseCase,
            moveTransferToLastByTagUseCase = moveTransferToLastByTagUseCase,
            getTransferByTagUseCase = getTransferByTagUseCase,
            getInProgressTransfersUseCase = getInProgressTransfersUseCase,
            monitorTransferEventsUseCase = monitorTransferEventsUseCase,
            monitorCompletedTransferEventUseCase = monitorCompletedTransferEventUseCase,
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
                    on { appData }.thenReturn("")
                    on { priority }.thenReturn(BigInteger.valueOf(i.toLong()))
                    on { transferState }.thenReturn(TransferState.STATE_COMPLETED)
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
                    on { appData }.thenReturn("")
                    on { priority }.thenReturn(BigInteger.valueOf(i.toLong()))
                    on { transferState }.thenReturn(TransferState.STATE_COMPLETED)
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
    fun `test that when a completed transfer event is received, the completed transfer list state is updated with the added completed transfer`() =
        runTest {
            val expected = mock<CompletedTransfer>()

            whenever(dbH.completedTransfers).thenReturn(emptyList())
            whenever(monitorCompletedTransferEventUseCase()).thenReturn(
                flow { emit(expected) }
            )

            underTest.completedState.test {
                assertThat(awaitItem()).isEqualTo(CompletedTransfersState.TransfersUpdated(emptyList()))
                assertThat(awaitItem()).isEqualTo(
                    CompletedTransfersState.TransferFinishUpdated(
                        listOf(expected)
                    )
                )
            }
        }

}
