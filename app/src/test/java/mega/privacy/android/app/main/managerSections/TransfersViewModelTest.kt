package mega.privacy.android.app.main.managerSections

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.data.mapper.transfer.TransferAppDataMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.exception.chat.ChatUploadNotRetriedException
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.chat.message.pendingmessages.RetryChatUploadUseCase
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
import mega.privacy.android.domain.usecase.transfers.completed.MonitorCompletedTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.completed.MonitorCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransferByTagUseCase
import nz.mega.sdk.MegaTransfer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
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
    private val monitorPausedTransfersUseCase = mock<MonitorPausedTransfersUseCase>()
    private val monitorCompletedTransfersUseCase: MonitorCompletedTransfersUseCase = mock()
    private val getFailedOrCanceledTransfersUseCase: GetFailedOrCanceledTransfersUseCase = mock()
    private val deleteCompletedTransferUseCase: DeleteCompletedTransferUseCase = mock()
    private val pauseTransferByTagUseCase: PauseTransferByTagUseCase = mock()
    private val cancelTransferByTagUseCase: CancelTransferByTagUseCase = mock()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val transferAppDataMapper = mock<TransferAppDataMapper>()
    private val retryChatUploadUseCase = mock<RetryChatUploadUseCase>()

    @BeforeEach
    fun setUp() {
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
            monitorCompletedTransfersUseCase = monitorCompletedTransfersUseCase,
            monitorTransferEventsUseCase = monitorTransferEventsUseCase,
            monitorCompletedTransferEventUseCase = monitorCompletedTransferEventUseCase,
            getFailedOrCanceledTransfersUseCase = getFailedOrCanceledTransfersUseCase,
            deleteCompletedTransferUseCase = deleteCompletedTransferUseCase,
            pauseTransferByTagUseCase = pauseTransferByTagUseCase,
            cancelTransferByTagUseCase = cancelTransferByTagUseCase,
            monitorPausedTransfersUseCase = monitorPausedTransfersUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            transferAppDataMapper = transferAppDataMapper,
            retryChatUploadUseCase = retryChatUploadUseCase,
        )
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
            whenever(monitorCompletedTransfersUseCase(TransfersViewModel.MAX_TRANSFERS)).thenReturn(
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

    @Test
    fun `test that areTransfersPaused emits a new value when monitorPausedTransfersUseCase emits a value`() =
        runTest {
            whenever(monitorPausedTransfersUseCase()).thenReturn(
                flowOf(false, true, false)
            )
            underTest.areTransfersPaused.test {
                assertThat(awaitItem()).isFalse()
                assertThat(awaitItem()).isTrue()
                assertThat(awaitItem()).isFalse()
                this.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that retryTransfer triggers a StartDownloadForOffline event when it is a download offline transfer`() =
        runTest {
            val nodeHandle = 564L
            val transfer = mock<CompletedTransfer> {
                on { type } doReturn MegaTransfer.TYPE_DOWNLOAD
                on { isOffline } doReturn true
                on { handle } doReturn nodeHandle
            }
            val expected = mock<TypedNode>()
            whenever(getNodeByIdUseCase(NodeId(nodeHandle))) doReturn expected

            underTest.retryTransfer(transfer)

            underTest.uiState.test {
                val newItem = awaitItem()
                assertThat(((newItem.startEvent as? StateEventWithContentTriggered<*>)?.content as? TransferTriggerEvent.StartDownloadForOffline)?.node)
                    .isEqualTo(expected)
            }
        }

    @Test
    fun `test that retryTransfer triggers a StartDownloadNode event when it is a download not offline transfer`() =
        runTest {
            val nodeHandle = 569L
            val transfer = mock<CompletedTransfer> {
                on { type } doReturn MegaTransfer.TYPE_DOWNLOAD
                on { isOffline } doReturn false
                on { handle } doReturn nodeHandle
            }
            val expected = mock<TypedNode>()
            whenever(getNodeByIdUseCase(NodeId(nodeHandle))) doReturn expected

            underTest.retryTransfer(transfer)

            underTest.uiState.test {
                val newItem = awaitItem()
                assertThat(((newItem.startEvent as? StateEventWithContentTriggered<*>)?.content as? TransferTriggerEvent.StartDownloadNode)?.nodes)
                    .isEqualTo(listOf(expected))
            }
        }

    @Test
    fun `test that retryTransfer triggers a StartUpload event when it is an upload and is not a chat upload`() =
        runTest {
            val path = "path"
            val file = File(path)
            val parentHandle = 123L
            val transfer = mock<CompletedTransfer> {
                on { type } doReturn MegaTransfer.TYPE_UPLOAD
                on { this.parentHandle } doReturn parentHandle
                on { originalPath } doReturn path
            }
            val expected = triggered(
                TransferTriggerEvent.StartUpload.Files(
                    mapOf(file.absolutePath to null),
                    NodeId(parentHandle)
                )
            )

            underTest.retryTransfer(transfer)

            underTest.uiState.map { it.startEvent }.test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }

    @Test
    fun `test that active transfers are swap when active transfers swap is called`() = runTest {
        val activeTransfer1 = mock<Transfer>()
        val activeTransfer2 = mock<Transfer>()
        val expected = listOf(activeTransfer2, activeTransfer1)
        whenever(getInProgressTransfersUseCase()) doReturn listOf(activeTransfer1, activeTransfer2)
        underTest.getAllActiveTransfers().join()


        underTest.activeTransfersSwap(0, 1)

        assertThat(underTest.getActiveTransfers()).isEqualTo(expected)
    }

    @Test
    fun `test that active transfers swap doesn't throw an error if indices are wrong`() = runTest {
        whenever(getInProgressTransfersUseCase()) doReturn listOf(mock())
        underTest.getAllActiveTransfers().join()

        assertDoesNotThrow {
            underTest.activeTransfersSwap(0, 1)
        }
    }

    @Test
    fun `test that retryTransfer resend messages when it is an upload and is a chat upload`() =
        runTest {
            val appData = "appData"
            val chatUpload = TransferAppData.ChatUpload(1L)
            val chatUploadAppData = listOf(chatUpload)
            val transfer = mock<CompletedTransfer> {
                on { type } doReturn MegaTransfer.TYPE_UPLOAD
                on { this.appData } doReturn appData
            }

            whenever(transferAppDataMapper(appData)).thenReturn(chatUploadAppData)

            underTest.retryTransfer(transfer)
            advanceUntilIdle()

            verify(retryChatUploadUseCase).invoke(chatUploadAppData)
        }

    @Test
    fun `test that retryTransfer updates state correctly when it is an upload, is a chat upload and RetryChatUploadUseCase throws ChatUploadNotRetriedException`() =
        runTest {
            val path = "path"
            val file = File(path)
            val parentHandle = 123L
            val appData = "appData"
            val transfer = mock<CompletedTransfer> {
                on { type } doReturn MegaTransfer.TYPE_UPLOAD
                on { this.appData } doReturn appData
                on { this.parentHandle } doReturn parentHandle
                on { originalPath } doReturn path
            }
            val chatUpload = TransferAppData.ChatUpload(1L)
            val chatUploadAppData = listOf(chatUpload)
            val expected = triggered(
                TransferTriggerEvent.StartUpload.Files(
                    mapOf(file.absolutePath to null),
                    NodeId(parentHandle)
                )
            )

            whenever(transferAppDataMapper(appData)).thenReturn(chatUploadAppData)
            whenever(retryChatUploadUseCase(chatUploadAppData))
                .thenThrow(ChatUploadNotRetriedException())

            underTest.retryTransfer(transfer)
            advanceUntilIdle()

            underTest.uiState.map { it.startEvent }.test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }


    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
