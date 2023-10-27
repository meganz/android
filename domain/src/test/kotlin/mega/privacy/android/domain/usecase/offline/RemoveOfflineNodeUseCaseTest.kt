package mega.privacy.android.domain.usecase.offline

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveOfflineNodeUseCaseTest {

    private lateinit var underTest: RemoveOfflineNodeUseCase
    private val nodeRepository: NodeRepository = mock()
    private val fileRepository: FileSystemRepository = mock()

    @BeforeAll
    fun setup() {
        underTest = RemoveOfflineNodeUseCase(
            nodeRepository = nodeRepository,
            fileRepository = fileRepository,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileRepository, nodeRepository)
    }

    @Test
    fun `test that when delete offline node calls on folder it deletes files and folders from database and local file system`() =
        runTest {
            stubPaths()
            underTest(nodeId = nodeId1)
            verify(fileRepository).deleteFolderAndItsFiles("${fileRepository.getOfflinePath()}${File.separator}$fileName")
            verify(nodeRepository).removeOfflineNodeById(nodeId1.longValue.toInt())
            verify(nodeRepository).removeOfflineNodeById(nodeId2.longValue.toInt())
            verify(nodeRepository).removeOfflineNodeById(nodeId3.longValue.toInt())
            verify(nodeRepository).removeOfflineNodeById(nodeId4.longValue.toInt())
        }

    private fun stubPaths() = runTest {
        whenever(fileRepository.getOfflinePath()).thenReturn(offlinePath)
        val offlineRootFolderNodeInformation1: OtherOfflineNodeInformation = mock {
            whenever(it.id).thenReturn(nodeId1.longValue.toInt())
            whenever(it.parentId).thenReturn(-1)
            whenever(it.isFolder).thenReturn(true)
            whenever(it.name).thenReturn(fileName)
        }
        val offlineFileNodeInformation1: OtherOfflineNodeInformation = mock {
            whenever(it.id).thenReturn(nodeId2.longValue.toInt())
            whenever(it.parentId).thenReturn(nodeId1.longValue.toInt())
            whenever(it.isFolder).thenReturn(false)

        }
        val offlineFolderNodeInformation1: OtherOfflineNodeInformation = mock {
            whenever(it.id).thenReturn(nodeId3.longValue.toInt())
            whenever(it.parentId).thenReturn(nodeId1.longValue.toInt())
            whenever(it.isFolder).thenReturn(true)
        }
        val offlineFileNodeInformation2: OtherOfflineNodeInformation = mock {
            whenever(it.id).thenReturn(nodeId4.longValue.toInt())
            whenever(it.parentId).thenReturn(nodeId3.longValue.toInt())
            whenever(it.isFolder).thenReturn(false)
        }
        whenever(nodeRepository.getOfflineNodeInformation(nodeId1)).thenReturn(
            offlineRootFolderNodeInformation1
        )
        whenever(nodeRepository.getOfflineNodeByParentId(nodeId1.longValue.toInt())).thenReturn(
            listOf(offlineFileNodeInformation1, offlineFolderNodeInformation1)
        )
        whenever(nodeRepository.getOfflineNodeByParentId(nodeId3.longValue.toInt())).thenReturn(
            listOf(offlineFileNodeInformation2)
        )
        whenever(fileRepository.getOfflinePath()).thenReturn(offlinePath)
    }

    companion object {
        private const val fileName = "file"
        private const val offlinePath = "offlinePath"
        private val nodeId1 = NodeId(1)
        private val nodeId2 = NodeId(2)
        private val nodeId3 = NodeId(3)
        private val nodeId4 = NodeId(4)
    }
}