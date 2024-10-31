package mega.privacy.android.domain.usecase.transfers.pending

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartAllPendingDownloadsUseCaseTest {
    private lateinit var underTest: StartAllPendingDownloadsUseCase

    private val transferRepository = mock<TransferRepository>()
    private val getPendingTransfersByTypeAndStateUseCase =
        mock<GetPendingTransfersByTypeAndStateUseCase>()
    private val updatePendingTransferStateUseCase = mock<UpdatePendingTransferStateUseCase>()
    private val getTypedNodeFromPendingTransferUseCase =
        mock<GetTypedNodeFromPendingTransferUseCase>()
    private val downloadNodesUseCase = mock<DownloadNodesUseCase>()
    private val updatePendingTransferStartedCountUseCase =
        mock<UpdatePendingTransferStartedCountUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = StartAllPendingDownloadsUseCase(
            transferRepository,
            getPendingTransfersByTypeAndStateUseCase,
            updatePendingTransferStateUseCase,
            getTypedNodeFromPendingTransferUseCase,
            downloadNodesUseCase,
            updatePendingTransferStartedCountUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            transferRepository,
            getPendingTransfersByTypeAndStateUseCase,
            updatePendingTransferStateUseCase,
            getTypedNodeFromPendingTransferUseCase,
            downloadNodesUseCase,
            updatePendingTransferStartedCountUseCase,
        )
    }

    @Test
    fun `test that the flow emits 0 when there are no pending messages in NotSentToSdk state`() =
        runTest {
            stubNotSentPendingTransfers(emptyList())

            underTest().test {
                assertThat(awaitItem()).isEqualTo(0)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the flow emits the total amount of pending messages in NotSentToSdk state`() =
        runTest {
            stubNotSentPendingTransfers(listOf(mock(), mock()))

            underTest().test {
                assertThat(awaitItem()).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the state of pending transfers is updated to SdkScanning when started`() =
        runTest {
            val pendingTransfers = listOf(mock<PendingTransfer>())
            stubNotSentPendingTransfers(pendingTransfers)

            underTest().test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            verify(updatePendingTransferStateUseCase)
                .invoke(pendingTransfers, PendingTransferState.SdkScanning)
        }

    @Test
    fun `test that the flow conflates the collected pending transfers`() =
        runTest {
            val firstEmission = listOf<PendingTransfer>(mock())
            val secondEmission = listOf<PendingTransfer>(mock(), mock())
            val thirdEmission = listOf<PendingTransfer>(mock(), mock(), mock())
            stubNotSentPendingTransfers(firstEmission, secondEmission, thirdEmission)
            whenever(
                updatePendingTransferStateUseCase(
                    firstEmission,
                    PendingTransferState.SdkScanning
                )
            ) doSuspendableAnswer { yield() }

            underTest().test {
                assertThat(awaitItem()).isEqualTo(1)
                assertThat(awaitItem()).isEqualTo(3)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that downloadNodesUseCase is invoked for each node corresponding to the pending transfer`() =
        runTest {
            val pendingTransfers = (0..5).map { index ->
                mock<PendingTransfer> {
                    on { path } doReturn "path/file$index.txt"
                    on { appData } doReturn listOf(mock<TransferAppData.ChatUpload>())
                    on { isHighPriority } doReturn (index == 2)

                }
            }
            val nodes = pendingTransfers.map { pendingTransfer ->
                mock<DefaultTypedFileNode>().also {
                    whenever(getTypedNodeFromPendingTransferUseCase(pendingTransfer)) doReturn it
                }
            }
            stubNotSentPendingTransfers(pendingTransfers)

            underTest().test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            nodes.forEachIndexed { index, node ->
                verify(downloadNodesUseCase).invoke(
                    nodes = listOfNotNull(node),
                    destinationPath = pendingTransfers[index].path,
                    appData = pendingTransfers[index].appData,
                    isHighPriority = pendingTransfers[index].isHighPriority,
                )
            }
        }

    @Test
    fun `test that pending transfers state is updated to SdkScanned when scanningFinished event is received`() =
        runTest {
            val pendingTransfer = mock<PendingTransfer>()
            stubNotSentPendingTransfers(listOf(pendingTransfer))
            val typedNode = mock<DefaultTypedFileNode>()
            val event = mock<MultiTransferEvent.SingleTransferEvent> {
                on { scanningFinished } doReturn true
            }
            whenever(getTypedNodeFromPendingTransferUseCase(pendingTransfer)) doReturn typedNode
            whenever(
                downloadNodesUseCase(
                    eq(listOf(typedNode)),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            ) doReturn flowOf(event)

            underTest().test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            verify(updatePendingTransferStateUseCase)(
                listOf(pendingTransfer),
                PendingTransferState.SdkScanned
            )
        }

    @Test
    fun `test that updatePendingTransferStartedCountUseCase is invoked when allTransfersUpdated event is received`() =
        runTest {
            val pendingTransfer = mock<PendingTransfer>()
            stubNotSentPendingTransfers(listOf(pendingTransfer))
            val typedNode = mock<DefaultTypedFileNode>()
            val event = mock<MultiTransferEvent.SingleTransferEvent> {
                on { allTransfersUpdated } doReturn true
                on { startedFiles } doReturn 45
                on { alreadyTransferred } doReturn 1
            }
            whenever(getTypedNodeFromPendingTransferUseCase(pendingTransfer)) doReturn typedNode
            whenever(
                downloadNodesUseCase(
                    eq(listOf(typedNode)),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            ) doReturn flowOf(event)

            underTest().test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            verify(updatePendingTransferStartedCountUseCase)(
                pendingTransfer,
                event.startedFiles,
                event.alreadyTransferred,
            )
        }

    @Test
    fun `test that pending transfers state is updated to ErrorStarting and failed completed transfer is added when there is an exception getting the node`() =
        runTest {
            val pendingTransfer = mock<PendingTransfer>()
            stubNotSentPendingTransfers(listOf(pendingTransfer))
            val exception = RuntimeException()
            whenever(getTypedNodeFromPendingTransferUseCase(pendingTransfer)) doThrow exception

            underTest().test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            verify(updatePendingTransferStateUseCase)(
                listOf(pendingTransfer),
                PendingTransferState.ErrorStarting
            )
            verify(transferRepository).addCompletedTransferFromFailedPendingTransfer(
                pendingTransfer,
                0,
                exception,
            )
        }

    @Test
    fun `test that pending transfers state is updated to ErrorStarting and failed completed transfer is added when node does not exist`() =
        runTest {
            val pendingTransfer = mock<PendingTransfer>()
            stubNotSentPendingTransfers(listOf(pendingTransfer))
            whenever(getTypedNodeFromPendingTransferUseCase(pendingTransfer)) doReturn null

            underTest().test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            verify(updatePendingTransferStateUseCase)(
                listOf(pendingTransfer),
                PendingTransferState.ErrorStarting
            )
            verify(transferRepository).addCompletedTransferFromFailedPendingTransfer(
                eq(pendingTransfer),
                eq(0),
                isA<NodeDoesNotExistsException>(),
            )
        }

    @Test
    fun `test that pending transfers state is updated to ErrorStarting and failed completed transfer is added when there is an exception downloading the node`() =
        runTest {
            val pendingTransfer = mock<PendingTransfer>()
            stubNotSentPendingTransfers(listOf(pendingTransfer))
            val size = 45L
            val typedNode = mock<DefaultTypedFileNode> {
                on { this.size } doReturn size
            }
            val exception = RuntimeException()
            whenever(getTypedNodeFromPendingTransferUseCase(pendingTransfer)) doReturn typedNode
            whenever(
                downloadNodesUseCase(
                    eq(listOf(typedNode)),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            ) doReturn flow {
                throw exception
            }

            underTest().test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            verify(updatePendingTransferStateUseCase)(
                listOf(pendingTransfer),
                PendingTransferState.ErrorStarting
            )
            verify(transferRepository).addCompletedTransferFromFailedPendingTransfer(
                pendingTransfer,
                size,
                exception,
            )
        }

    @Test
    fun `test that pending transfers state is updated to ErrorStarting and failed completed transfer is added when there is an exception collecting the pending transfers`() =
        runTest {
            val pendingTransfer = mock<PendingTransfer>()
            val pendingTransfers = listOf(pendingTransfer)
            val exception = RuntimeException()
            stubNotSentPendingTransfers(
                pendingTransfers,
                pendingTransfers,
                emptyList()
            )
            whenever(
                updatePendingTransferStateUseCase(
                    pendingTransfers,
                    PendingTransferState.SdkScanning
                )
            ).doThrow(exception)

            underTest().test {
                awaitComplete()
            }

            verify(updatePendingTransferStateUseCase)(
                pendingTransfers,
                PendingTransferState.ErrorStarting
            )
            verify(transferRepository).addCompletedTransferFromFailedPendingTransfer(
                eq(pendingTransfer),
                eq(0),
                any(),
            )
        }

    private fun stubNotSentPendingTransfers(vararg pendingTransfers: List<PendingTransfer>) {
        whenever(
            getPendingTransfersByTypeAndStateUseCase(
                TransferType.DOWNLOAD,
                PendingTransferState.NotSentToSdk
            )
        ) doReturn flowOf(*pendingTransfers)
    }
}