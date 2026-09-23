package mega.privacy.android.domain.usecase.transfers.offline

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetActiveOfflineDownloadTransfersUseCaseTest {
    private lateinit var underTest: GetActiveOfflineDownloadTransfersUseCase

    private val transferRepository = mock<TransferRepository>()

    private val node = mock<TypedFolderNode> { on { id } doReturn NodeId(1L) }

    @BeforeAll
    fun setup() {
        underTest = GetActiveOfflineDownloadTransfersUseCase(transferRepository)
    }

    @BeforeEach
    fun cleanup() = runTest {
        reset(transferRepository)
        whenever(transferRepository.getActiveTransfersByType(TransferType.DOWNLOAD))
            .thenReturn(emptyList())
    }

    @Test
    fun `test that invoke returns an empty list when there are no transfers`() = runTest {
        assertThat(underTest(node)).isEmpty()
    }

    @Test
    fun `test that invoke returns the offline transfer of the node`() = runTest {
        val transfer = rootTransfer(node.id)
        stubActiveTransfers(listOf(transfer))

        assertThat(underTest(node)).containsExactly(transfer)
    }

    @Test
    fun `test that invoke does not return the offline transfer of another node`() = runTest {
        stubActiveTransfers(listOf(rootTransfer(NodeId(2L))))

        assertThat(underTest(node)).isEmpty()
    }

    @Test
    fun `test that invoke does not return the transfer of the node when it is not an offline download`() =
        runTest {
            stubActiveTransfers(listOf(rootTransfer(node.id, appData = emptyList())))

            assertThat(underTest(node)).isEmpty()
        }

    @Test
    fun `test that invoke returns the transfers the SDK started for the files of the folder`() =
        runTest {
            val folderTransfer = rootTransfer(node.id, tag = FOLDER_TAG)
            val childTransfer = childTransfer(folderTransferTag = FOLDER_TAG)
            stubActiveTransfers(listOf(folderTransfer, childTransfer))

            assertThat(underTest(node)).containsExactly(folderTransfer, childTransfer)
        }

    @Test
    fun `test that invoke does not return the transfers of the folder download of another node`() =
        runTest {
            val folderTransfer = rootTransfer(node.id, tag = FOLDER_TAG)
            stubActiveTransfers(
                listOf(folderTransfer, childTransfer(folderTransferTag = FOLDER_TAG + 1))
            )

            assertThat(underTest(node)).containsExactly(folderTransfer)
        }

    @Test
    fun `test that invoke returns an empty list when the node is downloaded as part of the folder download of an ancestor`() =
        runTest {
            stubActiveTransfers(listOf(childTransfer(folderTransferTag = FOLDER_TAG)))

            assertThat(underTest(node)).isEmpty()
        }

    @Test
    fun `test that invoke returns the transfer of the node when it finished successfully`() =
        runTest {
            val transfer = rootTransfer(node.id, isFinished = true, transferredBytes = 100L)
            stubActiveTransfers(listOf(transfer))

            assertThat(underTest(node)).containsExactly(transfer)
        }

    @Test
    fun `test that invoke returns the transfer of the node when it was already transferred`() =
        runTest {
            val transfer = rootTransfer(node.id, isFinished = true, isAlreadyTransferred = true)
            stubActiveTransfers(listOf(transfer))

            assertThat(underTest(node)).containsExactly(transfer)
        }

    @Test
    fun `test that invoke returns the folder transfer of the node when it finished`() = runTest {
        val transfer = rootTransfer(
            nodeId = node.id,
            isFinished = true,
            isFolderTransfer = true,
            transferredBytes = 0L,
        )
        stubActiveTransfers(listOf(transfer))

        assertThat(underTest(node)).containsExactly(transfer)
    }

    @Test
    fun `test that invoke does not return the transfer of the node when it finished with errors`() =
        runTest {
            stubActiveTransfers(
                listOf(rootTransfer(node.id, isFinished = true, transferredBytes = 50L))
            )

            assertThat(underTest(node)).isEmpty()
        }

    @Test
    fun `test that invoke does not return the transfer of the node when it was cancelled`() =
        runTest {
            stubActiveTransfers(listOf(rootTransfer(node.id, isCancelled = true)))

            assertThat(underTest(node)).isEmpty()
        }

    @Test
    fun `test that invoke only reads download transfers`() = runTest {
        underTest(node)

        verify(transferRepository).getActiveTransfersByType(TransferType.DOWNLOAD)
    }

    private suspend fun stubActiveTransfers(transfers: List<Transfer>) {
        whenever(transferRepository.getActiveTransfersByType(TransferType.DOWNLOAD))
            .thenReturn(transfers)
    }

    /**
     * A transfer started by the user action, the only one carrying the offline app data and the
     * handle of the node the user selected.
     */
    private fun rootTransfer(
        nodeId: NodeId,
        appData: List<TransferAppData> = listOf(TransferAppData.OfflineDownload),
        isFinished: Boolean = false,
        isCancelled: Boolean = false,
        isAlreadyTransferred: Boolean = false,
        isFolderTransfer: Boolean = false,
        transferredBytes: Long = 0L,
        tag: Int = FOLDER_TAG,
    ) = mock<Transfer> {
        on { it.nodeHandle } doReturn nodeId.longValue
        on { it.appData } doReturn appData
        on { it.isRootTransfer } doReturn true
        on { it.folderTransferTag } doReturn null
        on { it.tag } doReturn tag
        on { it.isFinished } doReturn isFinished
        on { it.isCancelled } doReturn isCancelled
        on { it.isAlreadyTransferred } doReturn isAlreadyTransferred
        on { it.isFolderTransfer } doReturn isFolderTransfer
        on { it.transferredBytes } doReturn transferredBytes
        on { it.totalBytes } doReturn 100L
    }

    /**
     * A file the SDK transfers in the context of the folder transfer of [folderTransferTag]. It
     * carries neither the offline app data nor the handle of the folder.
     */
    private fun childTransfer(folderTransferTag: Int) = mock<Transfer> {
        on { it.nodeHandle } doReturn 99L
        on { it.appData } doReturn emptyList()
        on { it.isRootTransfer } doReturn false
        on { it.folderTransferTag } doReturn folderTransferTag
        on { it.isFinished } doReturn false
        on { it.isCancelled } doReturn false
        on { it.isAlreadyTransferred } doReturn false
        on { it.transferredBytes } doReturn 0L
        on { it.totalBytes } doReturn 100L
    }

    private companion object {
        const val FOLDER_TAG = 10
    }
}
