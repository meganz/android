package mega.privacy.android.domain.usecase.transfers.offline

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationUseCase
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
    private val getActiveOfflineDownloadTransfersUseCase = mock<GetActiveOfflineDownloadTransfersUseCase>()
    private val cancelTransferByTagUseCase = mock<CancelTransferByTagUseCase>()
    private val getOfflineNodeInformationUseCase = mock<GetOfflineNodeInformationUseCase>()
    private val getOfflineFileUseCase = mock<GetOfflineFileUseCase>()
    private val isOfflinePathUseCase = mock<IsOfflinePathUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    private val node = mock<TypedFolderNode> { on { id } doReturn NodeId(1L) }

    @BeforeAll
    fun setup() {
        underTest = CancelAndDeleteOfflineDownloadUseCase(
            transferRepository = transferRepository,
            fileSystemRepository = fileSystemRepository,
            getActiveOfflineDownloadTransfersUseCase = getActiveOfflineDownloadTransfersUseCase,
            getOfflineNodeInformationUseCase = getOfflineNodeInformationUseCase,
            getOfflineFileUseCase = getOfflineFileUseCase,
            cancelTransferByTagUseCase = cancelTransferByTagUseCase,
            isOfflinePathUseCase = isOfflinePathUseCase,
        )
    }

    @BeforeEach
    fun cleanup() = runTest {
        reset(
            transferRepository,
            getActiveOfflineDownloadTransfersUseCase,
            getOfflineNodeInformationUseCase,
            getOfflineFileUseCase,
            cancelTransferByTagUseCase,
            isOfflinePathUseCase,
            fileSystemRepository,
        )
        whenever(getActiveOfflineDownloadTransfersUseCase(node)).thenReturn(emptyList())
        whenever(transferRepository.monitorActiveTransfersByType(TransferType.DOWNLOAD))
            .thenReturn(flowOf(emptyList()))
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
        whenever(isOfflinePathUseCase(OFFLINE_PATH)).thenReturn(true)
    }

    @Test
    fun `test that invoke cancels all the transfers downloading the node`() = runTest {
        stubTransfers(listOf(transfer(tag = 1), transfer(tag = 2)))

        underTest(node)

        verify(cancelTransferByTagUseCase).invoke(1)
        verify(cancelTransferByTagUseCase).invoke(2)
    }

    @Test
    fun `test that invoke does not cancel the transfers that already finished`() = runTest {
        stubTransfers(listOf(transfer(tag = 1, isFinished = true)))

        underTest(node)

        verify(cancelTransferByTagUseCase, never()).invoke(1)
    }

    @Test
    fun `test that invoke does nothing when the node is not being downloaded`() = runTest {
        underTest(node)

        verifyNoInteractions(cancelTransferByTagUseCase, fileSystemRepository)
    }

    @Test
    fun `test that invoke cancels the remaining transfers when cancelling one of them fails`() =
        runTest {
            stubTransfers(listOf(transfer(tag = 1), transfer(tag = 2)))
            whenever(cancelTransferByTagUseCase(1)).thenThrow(RuntimeException("Already finished"))

            underTest(node)

            verify(cancelTransferByTagUseCase).invoke(2)
        }

    @Test
    fun `test that invoke deletes the offline folder of the node once its transfers are cancelled`() =
        runTest {
            stubTransfers(listOf(transfer(tag = 1)))

            underTest(node)

            inOrder(cancelTransferByTagUseCase, fileSystemRepository) {
                verify(cancelTransferByTagUseCase).invoke(1)
                verify(fileSystemRepository).deleteFolderAndItsFiles(OFFLINE_PATH)
            }
        }

    @Test
    fun `test that invoke does not delete anything when the path of the node is not an offline path`() =
        runTest {
            stubTransfers(listOf(transfer(tag = 1)))
            whenever(isOfflinePathUseCase(OFFLINE_PATH)).thenReturn(false)

            underTest(node)

            verifyNoInteractions(fileSystemRepository)
        }

    @Test
    fun `test that invoke does not delete anything when the offline path of the node is not known`() =
        runTest {
            stubTransfers(listOf(transfer(tag = 1)))
            whenever(getOfflineNodeInformationUseCase(node))
                .thenThrow(RuntimeException("Node does not exist"))

            underTest(node)

            verifyNoInteractions(fileSystemRepository)
        }

    private suspend fun stubTransfers(transfers: List<Transfer>) {
        whenever(getActiveOfflineDownloadTransfersUseCase(node)).thenReturn(transfers)
    }

    private fun transfer(tag: Int, isFinished: Boolean = false) = mock<Transfer> {
        on { it.tag } doReturn tag
        on { it.uniqueId } doReturn tag.toLong()
        on { it.isFinished } doReturn isFinished
    }

    private companion object {
        const val OFFLINE_PATH = "/offline/MyFolder"
    }
}
