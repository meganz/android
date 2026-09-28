package mega.privacy.android.domain.usecase.transfers.offline

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroup
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflinePathForNodeUseCase
import mega.privacy.android.domain.usecase.offline.IsOfflinePathUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CancelAndDeleteOfflineDownloadUseCaseTest {
    private lateinit var underTest: CancelAndDeleteOfflineDownloadUseCase

    private val transferRepository = mock<TransferRepository>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val getActiveOfflineDownloadTransfersUseCase =
        mock<GetActiveOfflineDownloadTransfersUseCase>()
    private val getOfflineNodeInformationUseCase = mock<GetOfflineNodeInformationUseCase>()
    private val getOfflineFileUseCase = mock<GetOfflineFileUseCase>()
    private val getOfflinePathForNodeUseCase = mock<GetOfflinePathForNodeUseCase>()
    private val cancelTransferByTagUseCase = mock<CancelTransferByTagUseCase>()
    private val isOfflinePathUseCase = mock<IsOfflinePathUseCase>()

    private val node = mock<TypedFolderNode> { on { id } doReturn NodeId(NODE_HANDLE) }

    @BeforeAll
    fun setup() {
        underTest = CancelAndDeleteOfflineDownloadUseCase(
            transferRepository = transferRepository,
            fileSystemRepository = fileSystemRepository,
            getActiveOfflineDownloadTransfersUseCase = getActiveOfflineDownloadTransfersUseCase,
            getOfflineNodeInformationUseCase = getOfflineNodeInformationUseCase,
            getOfflineFileUseCase = getOfflineFileUseCase,
            getOfflinePathForNodeUseCase = getOfflinePathForNodeUseCase,
            cancelTransferByTagUseCase = cancelTransferByTagUseCase,
            isOfflinePathUseCase = isOfflinePathUseCase,
        )
    }

    @BeforeEach
    fun cleanup() = runTest {
        reset(
            transferRepository,
            fileSystemRepository,
            getActiveOfflineDownloadTransfersUseCase,
            getOfflineNodeInformationUseCase,
            getOfflineFileUseCase,
            getOfflinePathForNodeUseCase,
            cancelTransferByTagUseCase,
            isOfflinePathUseCase,
        )
        whenever(getActiveOfflineDownloadTransfersUseCase(node)).thenReturn(emptyList())
        whenever(transferRepository.monitorActiveTransfersByType(TransferType.DOWNLOAD))
            .thenReturn(flowOf(emptyList()))
        whenever(getOfflinePathForNodeUseCase(node)).thenReturn(PARENT_OFFLINE_PATH)
        val offlineNodeInformation = OtherOfflineNodeInformation(
            id = 1,
            path = "MyFolder",
            name = "MyFolder",
            handle = NODE_HANDLE.toString(),
            isFolder = true,
            lastModifiedTime = null,
            parentId = -1,
        )
        whenever(getOfflineNodeInformationUseCase(node)).thenReturn(offlineNodeInformation)
        whenever(getOfflineFileUseCase(offlineNodeInformation)).thenReturn(File(OFFLINE_PATH))
        whenever(isOfflinePathUseCase(OFFLINE_PATH)).thenReturn(true)
    }

    @Test
    fun `test that invoke does nothing when the node is not being downloaded`() = runTest {
        underTest(node)

        verifyNoInteractions(cancelTransferByTagUseCase, fileSystemRepository)
    }

    @Test
    fun `test that invoke cancels the transfer downloading the node`() = runTest {
        stubTransfers(listOf(ownTransfer(tag = 5)))

        underTest(node)

        verify(cancelTransferByTagUseCase).invoke(5)
    }

    @Test
    fun `test that invoke cancels the folder transfer and its files when the download was started for the node`() =
        runTest {
            stubTransfers(listOf(fileOfFolderTransfer(tag = 60), fileOfFolderTransfer(tag = 61)))
            stubGroupDestination(PARENT_OFFLINE_PATH)

            underTest(node)

            verify(cancelTransferByTagUseCase).invoke(FOLDER_TAG)
            verify(cancelTransferByTagUseCase).invoke(60)
            verify(cancelTransferByTagUseCase).invoke(61)
        }

    @Test
    fun `test that invoke does not cancel anything when the node is downloaded as part of the download of an ancestor`() =
        runTest {
            stubTransfers(listOf(fileOfFolderTransfer(tag = 60)))
            stubGroupDestination("/offline/Ancestor/")

            underTest(node)

            verifyNoInteractions(cancelTransferByTagUseCase, fileSystemRepository)
        }

    @Test
    fun `test that invoke cancels the download of the node when a download of an ancestor is running at the same time`() =
        runTest {
            stubTransfers(
                listOf(
                    fileOfFolderTransfer(
                        tag = 60,
                        groupId = ANCESTOR_GROUP_ID,
                        folderTransferTag = ANCESTOR_FOLDER_TAG,
                    ),
                    fileOfFolderTransfer(tag = 61),
                )
            )
            stubGroupDestination("/offline/Ancestor/", ANCESTOR_GROUP_ID)
            stubGroupDestination(PARENT_OFFLINE_PATH)

            underTest(node)

            verify(cancelTransferByTagUseCase).invoke(61)
            verify(cancelTransferByTagUseCase).invoke(FOLDER_TAG)
        }

    @Test
    fun `test that invoke does not cancel the transfers of an ancestor download running at the same time`() =
        runTest {
            stubTransfers(
                listOf(
                    fileOfFolderTransfer(
                        tag = 60,
                        groupId = ANCESTOR_GROUP_ID,
                        folderTransferTag = ANCESTOR_FOLDER_TAG,
                    ),
                    fileOfFolderTransfer(tag = 61),
                )
            )
            stubGroupDestination("/offline/Ancestor/", ANCESTOR_GROUP_ID)
            stubGroupDestination(PARENT_OFFLINE_PATH)

            underTest(node)

            verify(cancelTransferByTagUseCase, never()).invoke(60)
            verify(cancelTransferByTagUseCase, never()).invoke(ANCESTOR_FOLDER_TAG)
        }

    @Test
    fun `test that invoke does not cancel anything when the group of the download is not found`() =
        runTest {
            stubTransfers(listOf(fileOfFolderTransfer(tag = 60)))
            whenever(transferRepository.getActiveTransferGroupById(GROUP_ID.toInt()))
                .thenReturn(null)

            underTest(node)

            verifyNoInteractions(cancelTransferByTagUseCase, fileSystemRepository)
        }

    @Test
    fun `test that invoke deletes the offline folder of the node once its transfers are cancelled`() =
        runTest {
            stubTransfers(listOf(ownTransfer(tag = 5)))

            underTest(node)

            inOrder(cancelTransferByTagUseCase, fileSystemRepository) {
                verify(cancelTransferByTagUseCase).invoke(5)
                verify(fileSystemRepository).deleteFolderAndItsFiles(OFFLINE_PATH)
            }
        }

    @Test
    fun `test that invoke does not delete anything when the path of the node is not an offline path`() =
        runTest {
            stubTransfers(listOf(ownTransfer(tag = 5)))
            whenever(isOfflinePathUseCase(OFFLINE_PATH)).thenReturn(false)

            underTest(node)

            verifyNoInteractions(fileSystemRepository)
        }

    @Test
    fun `test that invoke does not cancel the transfers that already finished`() = runTest {
        stubTransfers(listOf(ownTransfer(tag = 5, isFinished = true)))

        underTest(node)

        verify(cancelTransferByTagUseCase, never()).invoke(5)
    }

    @Test
    fun `test that invoke cancels the remaining transfers when cancelling one of them fails`() =
        runTest {
            stubTransfers(listOf(ownTransfer(tag = 5), ownTransfer(tag = 6)))
            whenever(cancelTransferByTagUseCase(5)).thenThrow(RuntimeException("Already finished"))

            underTest(node)

            verify(cancelTransferByTagUseCase).invoke(6)
        }

    private suspend fun stubTransfers(transfers: List<Transfer>) {
        whenever(getActiveOfflineDownloadTransfersUseCase(node)).thenReturn(transfers)
    }

    private suspend fun stubGroupDestination(destination: String, groupId: Long = GROUP_ID) {
        val group = mock<ActiveTransferActionGroup> { on { it.destination } doReturn destination }
        whenever(transferRepository.getActiveTransferGroupById(groupId.toInt())).thenReturn(group)
    }

    private fun ownTransfer(tag: Int, isFinished: Boolean = false) = mock<Transfer> {
        on { it.tag } doReturn tag
        on { it.uniqueId } doReturn tag.toLong()
        on { it.folderTransferTag } doReturn null
        on { it.isRootTransfer } doReturn true
        on { it.nodeHandle } doReturn NODE_HANDLE
        on { it.isFinished } doReturn isFinished
        on { it.appData } doReturn listOf(TransferAppData.OfflineDownload())
    }

    private fun fileOfFolderTransfer(
        tag: Int,
        groupId: Long = GROUP_ID,
        folderTransferTag: Int = FOLDER_TAG,
    ) = mock<Transfer> {
        on { it.tag } doReturn tag
        on { it.uniqueId } doReturn tag.toLong()
        on { it.folderTransferTag } doReturn folderTransferTag
        on { it.isRootTransfer } doReturn false
        on { it.nodeHandle } doReturn 99L
        on { it.isFinished } doReturn false
        on { it.appData } doReturn listOf(
            TransferAppData.OfflineDownload(),
            TransferAppData.TransferGroup(groupId),
        )
    }

    private companion object {
        const val NODE_HANDLE = 1L
        const val FOLDER_TAG = 59
        const val GROUP_ID = 4L
        const val ANCESTOR_GROUP_ID = 7L
        const val ANCESTOR_FOLDER_TAG = 71
        const val PARENT_OFFLINE_PATH = "/offline/"
        const val OFFLINE_PATH = "/offline/MyFolder"
    }
}
