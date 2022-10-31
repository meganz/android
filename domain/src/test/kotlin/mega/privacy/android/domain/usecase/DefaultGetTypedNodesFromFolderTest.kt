package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.exception.ParentNotAFolderException
import mega.privacy.android.domain.repository.FileRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetTypedNodesFromFolderTest {
    lateinit var underTest: GetTypedNodesFromFolder

    private val fileRepository = mock<FileRepository>()

    private val addNodeType = mock<AddNodeType> { onBlocking { invoke(any()) }.thenReturn(mock()) }

    @Before
    fun setUp() {
        underTest = DefaultGetTypedNodesFromFolder(
            fileRepository = fileRepository,
            addNodeType = addNodeType
        )
        whenever(fileRepository.monitorNodeUpdates()).thenReturn(emptyFlow())
    }

    @Test
    fun `test that a non folder node throws an exception`() = runTest {
        val fileNode = mock<FileNode>()
        val nodeId = NodeId(1)
        whenever(fileRepository.getNodeById(nodeId)).thenReturn(fileNode)
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
            whenever(fileRepository.getNodeById(nodeId)).thenReturn(folderNode)
            whenever(fileRepository.getNodeChildren(folderNode)).thenReturn(
                listOf(childNode)
            )
            whenever(fileRepository.monitorNodeUpdates()).thenReturn(
                flowOf(listOf(childNode))
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
            val list = listOf(mock<UnTypedNode> { on { id }.thenReturn(NodeId(22L)) })
            val folderNode = mock<FolderNode>()
            val nodeId = NodeId(1)
            whenever(fileRepository.getNodeById(nodeId)).thenReturn(folderNode)
            whenever(fileRepository.getNodeChildren(folderNode)).thenReturn(list)
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
            whenever(fileRepository.getNodeById(nodeId)).thenReturn(folderNode)
            whenever(fileRepository.getNodeChildren(folderNode)).thenReturn(emptyList())
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
        whenever(fileRepository.getNodeById(folderId)).thenReturn(folderNode)
        whenever(fileRepository.getNodeChildren(folderNode)).thenReturn(
            listOf(childNode)
        )
        whenever(fileRepository.monitorNodeUpdates()).thenReturn(
            flowOf(listOf(folderNode))
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
            whenever(fileRepository.getNodeById(folderId)).thenReturn(folderNode)
            whenever(fileRepository.getNodeChildren(folderNode)).thenReturn(
                emptyList()
            )
            whenever(fileRepository.monitorNodeUpdates()).thenReturn(
                flowOf(listOf(childNode))
            )
            underTest(folderId).test {
                awaitItem()
                awaitComplete()
            }
        }

}