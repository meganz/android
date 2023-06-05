package mega.privacy.android.domain.usecase.offline

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaveOfflineNodeInformationUseCaseTest {

    private val nodeRepository: NodeRepository = mock()
    private val getOfflineNodeInformationUseCase: GetOfflineNodeInformationUseCase = mock()
    private val node: FileNode = mock()
    private val parent: FolderNode = mock()
    private val nodeOfflineInformation: OtherOfflineNodeInformation = mock()
    private val parentOfflineInformation: OtherOfflineNodeInformation = mock()


    private lateinit var underTest: SaveOfflineNodeInformationUseCase

    @BeforeAll
    fun setup() {
        underTest = SaveOfflineNodeInformationUseCase(
            nodeRepository, getOfflineNodeInformationUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            nodeRepository, getOfflineNodeInformationUseCase,
            node, parent, nodeOfflineInformation, parentOfflineInformation,
        )
    }

    @Test
    fun `test that information is not saved if it's already saved`() = runTest {
        stubDriveNodeWithoutParent()

        whenever(nodeRepository.getOfflineNodeInformation(nodeId)).thenReturn(mock<OtherOfflineNodeInformation>())

        underTest(nodeId)
        verify(nodeRepository, times(0)).saveOfflineNodeInformation(anyOrNull(), anyOrNull())
    }

    @Test
    fun `test that node is saved without parents when it's a root parent`() = runTest {
        stubDriveNodeWithoutParent()
        stubNodeOfflineInfo()

        whenever(nodeRepository.getOfflineNodeInformation(nodeId)).thenReturn(null)

        underTest(nodeId)
        verify(nodeRepository).saveOfflineNodeInformation(nodeOfflineInformation, null)
    }

    @Test
    fun `test that node and its parent are saved when the node has a parent`() = runTest {
        stubDriveNodeWithParent()
        stubNodeOfflineInfo()
        stubParentOfflineInfo()

        whenever(nodeRepository.getOfflineNodeInformation(nodeId)).thenReturn(null)

        underTest(nodeId)
        verify(nodeRepository).saveOfflineNodeInformation(nodeOfflineInformation, parentId)
        verify(nodeRepository).saveOfflineNodeInformation(parentOfflineInformation, null)
    }

    @Test
    fun `test that child is saved`() = runTest {
        stubDriveNodeWithParent()
        stubNodeOfflineInfo()
        stubParentOfflineInfo()

        whenever(nodeRepository.getOfflineNodeInformation(nodeId)).thenReturn(null)
        whenever(parent.fetchChildren).thenReturn { listOf(node) }

        underTest(parentId)
        verify(nodeRepository).saveOfflineNodeInformation(nodeOfflineInformation, parentId)
        verify(nodeRepository).saveOfflineNodeInformation(parentOfflineInformation, null)

    }

    private fun stubDriveNodeWithoutParent() = runTest {
        whenever(nodeRepository.getNodeById(nodeId)).thenReturn(node)
        whenever(node.id).thenReturn(nodeId)
        whenever(node.parentId).thenReturn(invalidId)
        whenever(nodeRepository.getNodeById(invalidId)).thenReturn(null)
        whenever(nodeRepository.getBackupFolderId()).thenReturn(invalidId)
    }

    private fun stubDriveNodeWithParent() = runTest {
        whenever(nodeRepository.getNodeById(nodeId)).thenReturn(node)
        whenever(node.id).thenReturn(nodeId)
        whenever(node.parentId).thenReturn(parentId)
        whenever(nodeRepository.getNodeById(parentId)).thenReturn(parent)
        whenever(parent.id).thenReturn(parentId)
        whenever(nodeRepository.getBackupFolderId()).thenReturn(invalidId)
    }

    private fun stubNodeOfflineInfo() = runTest {
        whenever(getOfflineNodeInformationUseCase(node)).thenReturn(nodeOfflineInformation)
    }

    private fun stubParentOfflineInfo() = runTest {
        whenever(getOfflineNodeInformationUseCase(parent)).thenReturn(parentOfflineInformation)

    }

    companion object {
        private val nodeId = NodeId(1L)
        private val parentId = NodeId(2L)
        private val invalidId = NodeId(-1L)
    }
}