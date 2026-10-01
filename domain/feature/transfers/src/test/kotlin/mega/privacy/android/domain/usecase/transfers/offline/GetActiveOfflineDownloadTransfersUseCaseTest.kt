package mega.privacy.android.domain.usecase.transfers.offline

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationUseCase
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
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetActiveOfflineDownloadTransfersUseCaseTest {
    private lateinit var underTest: GetActiveOfflineDownloadTransfersUseCase

    private val transferRepository = mock<TransferRepository>()
    private val getOfflineNodeInformationUseCase = mock<GetOfflineNodeInformationUseCase>()
    private val getOfflineFileUseCase = mock<GetOfflineFileUseCase>()

    private val node = mock<TypedFolderNode> { on { id } doReturn NodeId(1L) }

    @BeforeAll
    fun setup() {
        underTest = GetActiveOfflineDownloadTransfersUseCase(
            transferRepository = transferRepository,
            getOfflineNodeInformationUseCase = getOfflineNodeInformationUseCase,
            getOfflineFileUseCase = getOfflineFileUseCase,
        )
    }

    @BeforeEach
    fun cleanup() = runTest {
        reset(transferRepository, getOfflineNodeInformationUseCase, getOfflineFileUseCase)
        whenever(transferRepository.getActiveTransfersByType(TransferType.DOWNLOAD))
            .thenReturn(emptyList())
        val offlineNodeInformation = OtherOfflineNodeInformation(
            id = 1,
            path = "MyFolder",
            name = "MyFolder",
            handle = node.id.longValue.toString(),
            isFolder = true,
            lastModifiedTime = null,
            parentId = -1,
        )
        whenever(getOfflineNodeInformationUseCase(node)).thenReturn(offlineNodeInformation)
        whenever(getOfflineFileUseCase(offlineNodeInformation)).thenReturn(File(OFFLINE_PATH))
    }

    @Test
    fun `test that invoke returns an empty list when there are no transfers`() = runTest {
        assertThat(underTest(node)).isEmpty()
    }

    @Test
    fun `test that invoke returns the transfer downloading the node`() = runTest {
        val transfer = ownTransfer(node.id)
        stubActiveTransfers(listOf(transfer))

        assertThat(underTest(node)).containsExactly(transfer)
    }

    @Test
    fun `test that invoke does not return the transfer downloading another node`() = runTest {
        stubActiveTransfers(listOf(ownTransfer(NodeId(2L))))

        assertThat(underTest(node)).isEmpty()
    }

    @Test
    fun `test that invoke does not return the transfer of the node when it is not an offline download`() =
        runTest {
            stubActiveTransfers(listOf(ownTransfer(node.id, appData = emptyList())))

            assertThat(underTest(node)).isEmpty()
        }

    @Test
    fun `test that invoke returns the files being downloaded into the offline folder of the node`() =
        runTest {
            val file = fileOfFolderTransfer("$OFFLINE_PATH${File.separator}photo.jpg")
            stubActiveTransfers(listOf(folderTransfer(), file))

            assertThat(underTest(node)).containsExactly(file)
        }

    @Test
    fun `test that invoke returns the files being downloaded into a nested offline folder of the node`() =
        runTest {
            val file = fileOfFolderTransfer(
                "$OFFLINE_PATH${File.separator}nested${File.separator}photo.jpg"
            )
            stubActiveTransfers(listOf(folderTransfer(), file))

            assertThat(underTest(node)).containsExactly(file)
        }

    @Test
    fun `test that invoke does not return the files being downloaded into another offline folder`() =
        runTest {
            val file = fileOfFolderTransfer("${OFFLINE_PATH}Other${File.separator}photo.jpg")
            stubActiveTransfers(listOf(folderTransfer(), file))

            assertThat(underTest(node)).isEmpty()
        }

    @Test
    fun `test that invoke does not return the transfer of the node when it was cancelled`() =
        runTest {
            stubActiveTransfers(listOf(ownTransfer(node.id, isCancelled = true)))

            assertThat(underTest(node)).isEmpty()
        }

    @Test
    fun `test that invoke does not return the transfer of the node when it finished with errors`() =
        runTest {
            stubActiveTransfers(
                listOf(ownTransfer(node.id, isFinished = true, transferredBytes = 50L))
            )

            assertThat(underTest(node)).isEmpty()
        }

    @Test
    fun `test that invoke returns the transfer of the node when it finished successfully`() =
        runTest {
            val transfer = ownTransfer(node.id, isFinished = true, transferredBytes = 100L)
            stubActiveTransfers(listOf(transfer))

            assertThat(underTest(node)).containsExactly(transfer)
        }

    @Test
    fun `test that invoke does not get the offline path when no file is being downloaded`() =
        runTest {
            stubActiveTransfers(listOf(ownTransfer(node.id)))

            underTest(node)

            verifyNoInteractions(getOfflineNodeInformationUseCase, getOfflineFileUseCase)
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
     * The transfer downloading a file the user selected: it carries the offline app data and the
     * handle of its node.
     */
    private fun ownTransfer(
        nodeId: NodeId,
        appData: List<TransferAppData> = listOf(TransferAppData.OfflineDownload()),
        isFinished: Boolean = false,
        isCancelled: Boolean = false,
        transferredBytes: Long = 0L,
    ) = mock<Transfer> {
        on { it.nodeHandle } doReturn nodeId.longValue
        on { it.appData } doReturn appData
        on { it.isRootTransfer } doReturn true
        on { it.folderTransferTag } doReturn null
        on { it.tag } doReturn FOLDER_TAG
        on { it.isFinished } doReturn isFinished
        on { it.isCancelled } doReturn isCancelled
        on { it.isAlreadyTransferred } doReturn false
        on { it.isFolderTransfer } doReturn false
        on { it.transferredBytes } doReturn transferredBytes
        on { it.totalBytes } doReturn 100L
        on { it.localPath } doReturn OFFLINE_PATH
    }

    /**
     * The transfer the SDK creates for a folder: it carries no app data, no node handle and no
     * path, only its tag.
     */
    private fun folderTransfer() = mock<Transfer> {
        on { it.nodeHandle } doReturn -1L
        on { it.appData } doReturn emptyList()
        on { it.isRootTransfer } doReturn true
        on { it.folderTransferTag } doReturn null
        on { it.tag } doReturn FOLDER_TAG
        on { it.isFinished } doReturn false
        on { it.isCancelled } doReturn false
        on { it.isAlreadyTransferred } doReturn false
        on { it.isFolderTransfer } doReturn true
        on { it.transferredBytes } doReturn 0L
        on { it.totalBytes } doReturn 100L
        on { it.localPath } doReturn ""
    }

    /**
     * A file the SDK transfers in the context of a folder transfer. These carry the offline app
     * data, the handle of their own node and the path they are downloaded to.
     */
    private fun fileOfFolderTransfer(localPath: String) = mock<Transfer> {
        on { it.nodeHandle } doReturn 99L
        on { it.appData } doReturn listOf(TransferAppData.OfflineDownload())
        on { it.isRootTransfer } doReturn false
        on { it.folderTransferTag } doReturn FOLDER_TAG
        on { it.isFinished } doReturn false
        on { it.isCancelled } doReturn false
        on { it.isAlreadyTransferred } doReturn false
        on { it.isFolderTransfer } doReturn false
        on { it.transferredBytes } doReturn 0L
        on { it.totalBytes } doReturn 100L
        on { it.localPath } doReturn localPath
    }

    private companion object {
        const val FOLDER_TAG = 59
        const val OFFLINE_PATH = "/offline/MyFolder"
    }
}
