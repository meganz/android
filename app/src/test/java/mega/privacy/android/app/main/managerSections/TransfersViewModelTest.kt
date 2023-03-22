package mega.privacy.android.app.main.managerSections

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.LegacyDatabaseHandler
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.usecase.transfer.MonitorFailedTransfer
import mega.privacy.android.domain.usecase.transfer.MoveTransferBeforeByTagUseCase
import mega.privacy.android.domain.usecase.transfer.MoveTransferToFirstByTagUseCase
import mega.privacy.android.domain.usecase.transfer.MoveTransferToLastByTagUseCase
import nz.mega.sdk.MegaTransfer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigInteger

internal class TransfersViewModelTest {
    private lateinit var underTest: TransfersViewModel
    private val megaApiGateway: MegaApiGateway = mock()
    private val transfersManagement: TransfersManagement = mock()
    private val dbH: LegacyDatabaseHandler = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val monitorFailedTransfer: MonitorFailedTransfer = mock()
    private val moveTransferBeforeByTagUseCase: MoveTransferBeforeByTagUseCase = mock()
    private val moveTransferToFirstByTagUseCase: MoveTransferToFirstByTagUseCase = mock()
    private val moveTransferToLastByTagUseCase: MoveTransferToLastByTagUseCase = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(ioDispatcher)
        underTest = TransfersViewModel(
            megaApiGateway = megaApiGateway,
            transfersManagement = transfersManagement,
            dbH = dbH,
            ioDispatcher = ioDispatcher,
            monitorFailedTransfer = monitorFailedTransfer,
            moveTransferBeforeByTagUseCase = moveTransferBeforeByTagUseCase,
            moveTransferToFirstByTagUseCase = moveTransferToFirstByTagUseCase,
            moveTransferToLastByTagUseCase = moveTransferToLastByTagUseCase
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
            val transfer = mock<MegaTransfer> {
                on { tag }.thenReturn(transferTag)
            }
            whenever(moveTransferToFirstByTagUseCase.invoke(transferTag)).thenReturn(Unit)
            underTest.moveTransfer(transfer, 0)
            verify(megaApiGateway, times(1)).getTransfersByTag(transferTag)
        }

    @Test
    fun `test that moveTransfer invoke moveTransferToLastByTagUseCase success when pass newPosition as 0`() =
        runTest {
            val transfers = mutableListOf<MegaTransfer>()
            for (i in 1..5) {
                val transfer = mock<MegaTransfer> {
                    on { tag }.thenReturn(i)
                    on { isStreamingTransfer }.thenReturn(false)
                    on { appData }.thenReturn("")
                    on { priority }.thenReturn(BigInteger.valueOf(i.toLong()))
                    on { state }.thenReturn(MegaTransfer.STATE_COMPLETED)
                }
                transfers.add(transfer)
                whenever(megaApiGateway.getTransfersByTag(i)).thenReturn(transfer)
            }
            underTest.setActiveTransfers(transfers.map { it.tag })
            whenever(moveTransferToLastByTagUseCase.invoke(any())).thenReturn(Unit)
            underTest.moveTransfer(transfers.first(), transfers.lastIndex)
            verify(moveTransferToLastByTagUseCase, times(1)).invoke(any())
            verify(megaApiGateway, times(2)).getTransfersByTag(transfers.first().tag)
            underTest.activeState.test {
                Truth.assertThat(awaitItem())
                    .isInstanceOf(ActiveTransfersState.TransferMovementFinishedUpdated::class.java)
            }
        }

    @Test
    fun `test that moveTransfer invoke moveTransferBeforeByTagUseCase success when pass newPosition in the middle of the list`() =
        runTest {
            val transfers = mutableListOf<MegaTransfer>()
            for (i in 1..5) {
                val transfer = mock<MegaTransfer> {
                    on { tag }.thenReturn(i)
                    on { isStreamingTransfer }.thenReturn(false)
                    on { appData }.thenReturn("")
                    on { priority }.thenReturn(BigInteger.valueOf(i.toLong()))
                    on { state }.thenReturn(MegaTransfer.STATE_COMPLETED)
                }
                transfers.add(transfer)
                whenever(megaApiGateway.getTransfersByTag(i)).thenReturn(transfer)
            }
            underTest.setActiveTransfers(transfers.map { it.tag })
            whenever(moveTransferBeforeByTagUseCase.invoke(any(), any())).thenReturn(Unit)
            underTest.moveTransfer(transfers.first(), 2)
            verify(moveTransferBeforeByTagUseCase, times(1)).invoke(any(), any())
            verify(megaApiGateway, times(2)).getTransfersByTag(transfers.first().tag)
            underTest.activeState.test {
                Truth.assertThat(awaitItem())
                    .isInstanceOf(ActiveTransfersState.TransferMovementFinishedUpdated::class.java)
            }
        }
}