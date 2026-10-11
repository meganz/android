package mega.privacy.android.domain.usecase.transfers.offline

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsOfflineDownloadInProgressUseCaseTest {
    private lateinit var underTest: IsOfflineDownloadInProgressUseCase

    private val transferRepository = mock<TransferRepository>()
    private val getActiveOfflineDownloadTransfersUseCase = mock<GetActiveOfflineDownloadTransfersUseCase>()

    private val node = mock<TypedFolderNode> { on { id } doReturn NodeId(1L) }

    @BeforeAll
    fun setup() {
        underTest = IsOfflineDownloadInProgressUseCase(
            transferRepository = transferRepository,
            getActiveOfflineDownloadTransfersUseCase = getActiveOfflineDownloadTransfersUseCase,
        )
    }

    @BeforeEach
    fun cleanup() = runTest {
        reset(transferRepository, getActiveOfflineDownloadTransfersUseCase)
        whenever(transferRepository.getPendingTransfersByType(TransferType.DOWNLOAD))
            .thenReturn(emptyList())
        whenever(getActiveOfflineDownloadTransfersUseCase(node)).thenReturn(emptyList())
    }

    @Test
    fun `test that invoke returns false when there is no pending nor active transfer of the node`() =
        runTest {
            assertThat(underTest(node)).isFalse()
        }

    @Test
    fun `test that invoke returns true when a not resolved offline pending transfer exists`() =
        runTest {
            stubPendingTransfers(listOf(pendingTransfer(node.id)))

            assertThat(underTest(node)).isTrue()
        }

    @Test
    fun `test that invoke returns false when the pending transfer is resolved`() = runTest {
        stubPendingTransfers(
            listOf(pendingTransfer(node.id, state = PendingTransferState.AlreadyStarted))
        )

        assertThat(underTest(node)).isFalse()
    }

    @Test
    fun `test that invoke returns false when the pending transfer is not an offline download`() =
        runTest {
            stubPendingTransfers(listOf(pendingTransfer(node.id, appData = emptyList())))

            assertThat(underTest(node)).isFalse()
        }

    @Test
    fun `test that invoke returns false when the pending transfer is for another node`() = runTest {
        stubPendingTransfers(listOf(pendingTransfer(NodeId(2L))))

        assertThat(underTest(node)).isFalse()
    }

    @Test
    fun `test that invoke returns true when the node has transfers downloading it`() = runTest {
        val transfer = mock<Transfer>()
        whenever(getActiveOfflineDownloadTransfersUseCase(node)).thenReturn(listOf(transfer))

        assertThat(underTest(node)).isTrue()
    }

    @Test
    fun `test that invoke only checks download transfers`() = runTest {
        underTest(node)

        verify(transferRepository).getPendingTransfersByType(TransferType.DOWNLOAD)
    }

    private suspend fun stubPendingTransfers(pendingTransfers: List<PendingTransfer>) {
        whenever(transferRepository.getPendingTransfersByType(TransferType.DOWNLOAD))
            .thenReturn(pendingTransfers)
    }

    private fun pendingTransfer(
        nodeId: NodeId,
        appData: List<TransferAppData> = listOf(TransferAppData.OfflineDownload()),
        state: PendingTransferState = PendingTransferState.SdkScanning,
    ) = PendingTransfer(
        pendingTransferId = nodeId.longValue,
        transferType = TransferType.DOWNLOAD,
        nodeIdentifier = PendingTransferNodeIdentifier.CloudDriveNode(nodeId),
        uriPath = UriPath("/offline/MyFolder"),
        appData = appData,
        isHighPriority = false,
        state = state,
        fileName = "file.txt",
        pitagTrigger = PitagTrigger.NotApplicable,
    )

    @Test
    fun `test that invoke does not read the pending transfers when the node is already being downloaded`() =
        runTest {
            whenever(getActiveOfflineDownloadTransfersUseCase(node)).thenReturn(listOf(mock()))

            assertThat(underTest(node)).isTrue()

            verifyNoInteractions(transferRepository)
        }
}
