package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.exception.ParentNotAFolderException
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
class GetTypedNodesFromFolderUseCaseTest {
    lateinit var underTest: GetTypedNodesFromFolderUseCase

    private val nodeRepository = mock<NodeRepository>()

    private val addNodeType =
        mock<AddNodeType> { onBlocking { invoke(any()) }.thenReturn(mock<TypedFolderNode>()) }

    @Before
    fun setUp() {
        underTest = GetTypedNodesFromFolderUseCase(
            nodeRepository = nodeRepository,
            addNodeType = addNodeType
        )
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(emptyFlow())
    }

    @Test
    fun `test that a non folder node throws an exception`() = runTest {
        val fileNode = mock<FileNode>()
        val nodeId = NodeId(1)
        whenever(nodeRepository.getNodeById(nodeId)).thenReturn(fileNode)
        underTest(nodeId).test {
            val error = awaitError()
            assertThat(error).isInstanceOf(ParentNotAFolderException::class.java)
        }
    }

    @Test
    fun `test that a new list will return when nodes are updated`() =
        runTest {
            val folderNode = mock<FolderNode>()
            val nodeId = NodeId(1)
            val fileNodeId = NodeId(2)
            val childNode = mock<FileNode> { on { id }.thenReturn(fileNodeId) }
            whenever(nodeRepository.getNodeById(nodeId)).thenReturn(folderNode)
            whenever(nodeRepository.getNodeChildren(folderNode)).thenReturn(
                listOf(childNode)
            )
            val map = mapOf<Node, List<NodeChanges>>(childNode to emptyList())
            whenever(nodeRepository.monitorNodeUpdates()).thenReturn(
                flowOf(NodeUpdate(map))
            )
            underTest(nodeId).test {
                awaitItem()
                awaitItem()
                awaitComplete()
            }
        }

    @Test
    fun `test that folder child is not empty`() {
        runTest {
            val list = listOf(mock<FolderNode> { on { id }.thenReturn(NodeId(22L)) })
            val folderNode = mock<FolderNode>()
            val nodeId = NodeId(1)
            whenever(nodeRepository.getNodeById(nodeId)).thenReturn(folderNode)
            whenever(nodeRepository.getNodeChildren(folderNode)).thenReturn(list)
            underTest(nodeId).test {
                assertThat(awaitItem()).isNotEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `test that folder child is empty`() {
        runTest {
            val folderNode = mock<FolderNode>()
            val nodeId = NodeId(1)
            whenever(nodeRepository.getNodeById(nodeId)).thenReturn(folderNode)
            whenever(nodeRepository.getNodeChildren(folderNode)).thenReturn(emptyList())
            underTest(nodeId).test {
                assertThat(awaitItem()).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `test that a new list is returned when the folder node is updated`() = runTest {
        val folderId = NodeId(1)
        val folderNode = mock<FolderNode> { on { id }.thenReturn(folderId) }
        val fileNodeId = NodeId(2)
        val childNode = mock<FileNode> { on { id }.thenReturn(fileNodeId) }
        whenever(nodeRepository.getNodeById(folderId)).thenReturn(folderNode)
        whenever(nodeRepository.getNodeChildren(folderNode)).thenReturn(
            listOf(childNode)
        )
        val map = mapOf<Node, List<NodeChanges>>(folderNode to emptyList())
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(
            flowOf(NodeUpdate(map))
        )
        underTest(folderId).test {
            awaitItem()
            awaitItem()
            awaitComplete()
        }
    }

    @Test
    fun `test that no new list is emitted if the update does not match any of the nodes`() =
        runTest {
            val folderId = NodeId(1)
            val folderNode = mock<FolderNode> { on { id }.thenReturn(folderId) }
            val fileNodeId = NodeId(2)
            val childNode = mock<FileNode> { on { id }.thenReturn(fileNodeId) }
            whenever(nodeRepository.getNodeById(folderId)).thenReturn(folderNode)
            whenever(nodeRepository.getNodeChildren(folderNode)).thenReturn(
                emptyList()
            )
            whenever(nodeRepository.monitorNodeUpdates()).thenReturn(
                flowOf(NodeUpdate(emptyMap()))
            )
            underTest(folderId).test {
                awaitItem()
                awaitComplete()
            }
        }

}