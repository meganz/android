package mega.privacy.android.domain.usecase.offline

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.pdf.CheckIfShouldDeleteLastPageViewedInPdfUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveOfflineNodeUseCaseTest {

    private lateinit var underTest: RemoveOfflineNodeUseCase
    private val nodeRepository: NodeRepository = mock()
    private val fileRepository: FileSystemRepository = mock()
    private val checkIfShouldDeleteLastPageViewedInPdfUseCase =
        mock<CheckIfShouldDeleteLastPageViewedInPdfUseCase>()

    @BeforeAll
    fun setup() {
        underTest = RemoveOfflineNodeUseCase(
            nodeRepository = nodeRepository,
            fileRepository = fileRepository,
            checkIfShouldDeleteLastPageViewedInPdfUseCase = checkIfShouldDeleteLastPageViewedInPdfUseCase,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileRepository, nodeRepository, checkIfShouldDeleteLastPageViewedInPdfUseCase)
    }

    @Test
    fun `test that when delete offline node calls on folder it deletes files and folders from database and local file system`() =
        runTest {
            stubPaths()
            underTest(nodeId = nodeId1)
            verify(fileRepository).deleteFolderAndItsFiles("${fileRepository.getOfflinePath()}${File.separator}$fileName1")
            verify(nodeRepository).removeOfflineNodeById(nodeId1.longValue.toInt())
            verify(nodeRepository).removeOfflineNodeById(nodeId2.longValue.toInt())
            verify(nodeRepository).removeOfflineNodeById(nodeId3.longValue.toInt())
            verify(nodeRepository).removeOfflineNodeById(nodeId4.longValue.toInt())
        }

    @Test
    fun `test that when delete offline node calls on folder it invokes CheckIfShouldDeleteLastPageViewedInPdfUseCase for files`() =
        runTest {
            stubPaths()
            underTest(nodeId = nodeId1)
            verify(
                checkIfShouldDeleteLastPageViewedInPdfUseCase,
                times(0)
            ).invoke(nodeHandle = handle1.toLong(), fileName = fileName1, isOfflineRemoval = true)
            verify(checkIfShouldDeleteLastPageViewedInPdfUseCase).invoke(
                nodeHandle = handle2.toLong(),
                fileName = fileName2,
                isOfflineRemoval = true,
            )
            verify(
                checkIfShouldDeleteLastPageViewedInPdfUseCase,
                times(0)
            ).invoke(
                nodeHandle = handle3.toLong(),
                fileName = fileName3,
                isOfflineRemoval = true,
            )
            verify(checkIfShouldDeleteLastPageViewedInPdfUseCase).invoke(
                nodeHandle = handle4.toLong(),
                fileName = fileName4,
                isOfflineRemoval = true,
            )
        }

    private fun stubPaths() = runTest {
        whenever(fileRepository.getOfflinePath()).thenReturn(offlinePath)
        val offlineRootFolderNodeInformation1: OtherOfflineNodeInformation = mock {
            whenever(it.id).thenReturn(nodeId1.longValue.toInt())
            whenever(it.parentId).thenReturn(-1)
            whenever(it.isFolder).thenReturn(true)
            whenever(it.name).thenReturn(fileName1)
            whenever(it.handle).thenReturn(handle1)
        }
        val offlineFileNodeInformation1: OtherOfflineNodeInformation = mock {
            whenever(it.id).thenReturn(nodeId2.longValue.toInt())
            whenever(it.parentId).thenReturn(nodeId1.longValue.toInt())
            whenever(it.isFolder).thenReturn(false)
            whenever(it.name).thenReturn(fileName2)
            whenever(it.handle).thenReturn(handle2)

        }
        val offlineFolderNodeInformation1: OtherOfflineNodeInformation = mock {
            whenever(it.id).thenReturn(nodeId3.longValue.toInt())
            whenever(it.parentId).thenReturn(nodeId1.longValue.toInt())
            whenever(it.isFolder).thenReturn(true)
            whenever(it.name).thenReturn(fileName3)
            whenever(it.handle).thenReturn(handle3)
        }
        val offlineFileNodeInformation2: OtherOfflineNodeInformation = mock {
            whenever(it.id).thenReturn(nodeId4.longValue.toInt())
            whenever(it.parentId).thenReturn(nodeId3.longValue.toInt())
            whenever(it.isFolder).thenReturn(false)
            whenever(it.name).thenReturn(fileName4)
            whenever(it.handle).thenReturn(handle4)
        }
        whenever(nodeRepository.getOfflineNodeInformation(nodeId1)).thenReturn(
            offlineRootFolderNodeInformation1
        )
        whenever(nodeRepository.getOfflineNodesByParentId(nodeId1.longValue.toInt())).thenReturn(
            listOf(offlineFileNodeInformation1, offlineFolderNodeInformation1)
        )
        whenever(nodeRepository.getOfflineNodesByParentId(nodeId3.longValue.toInt())).thenReturn(
            listOf(offlineFileNodeInformation2)
        )
        whenever(fileRepository.getOfflinePath()).thenReturn(offlinePath)
    }

    companion object {
        private const val fileName1 = "file1"
        private const val fileName2 = "file2"
        private const val fileName3 = "file3"
        private const val fileName4 = "file4"
        private const val handle1 = "1234"
        private const val handle2 = "2345"
        private const val handle3 = "3456"
        private const val handle4 = "4567"
        private const val offlinePath = "offlinePath"
        private val nodeId1 = NodeId(1)
        private val nodeId2 = NodeId(2)
        private val nodeId3 = NodeId(3)
        private val nodeId4 = NodeId(4)
    }
}