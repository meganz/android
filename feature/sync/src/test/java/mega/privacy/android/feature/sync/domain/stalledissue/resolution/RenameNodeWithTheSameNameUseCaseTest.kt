package mega.privacy.android.feature.sync.domain.stalledissue.resolution

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution.AddCounterToNodeNameUseCase
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution.RenameNodeWithTheSameNameUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RenameNodeWithTheSameNameUseCaseTest {

    private val addCounterToNodeNameUseCase: AddCounterToNodeNameUseCase = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val underTest: RenameNodeWithTheSameNameUseCase =
        RenameNodeWithTheSameNameUseCase(addCounterToNodeNameUseCase, getNodeByIdUseCase)

    @AfterEach
    fun resetAndTearDown() {
        reset(addCounterToNodeNameUseCase, getNodeByIdUseCase)
    }

    @Test
    fun `test renaming nodes with the same name adds counters and skips first node when multiple nodes`() =
        runTest {
            val node1 = NodeId(123)
            val node2 = NodeId(456)
            val node3 = NodeId(789)
            val nodeIds = listOf(node1, node2, node3)

            val fileNode1 = mock<TypedNode>()
            val fileNode2 = mock<TypedNode>()
            val fileNode3 = mock<TypedNode>()

            whenever(fileNode1.name).thenReturn("file.txt")
            whenever(fileNode2.name).thenReturn("FILE.txt")
            whenever(fileNode3.name).thenReturn("File.txt")

            whenever(getNodeByIdUseCase(node1)).thenReturn(fileNode1)
            whenever(getNodeByIdUseCase(node2)).thenReturn(fileNode2)
            whenever(getNodeByIdUseCase(node3)).thenReturn(fileNode3)

            underTest(nodeIds)

            // First node should be skipped, only second and third should be renamed
            verify(addCounterToNodeNameUseCase).invoke("FILE.txt", node2, 1)
            verify(addCounterToNodeNameUseCase).invoke("File.txt", node3, 2)
            verify(addCounterToNodeNameUseCase, times(0)).invoke("file.txt", node1, 1)
        }

    @Test
    fun `test renaming single node adds counter`() = runTest {
        val node1 = NodeId(123)
        val nodeIds = listOf(node1)

        val fileNode = mock<TypedNode>()
        whenever(fileNode.name).thenReturn("file.txt")

        whenever(getNodeByIdUseCase(node1)).thenReturn(fileNode)

        underTest(nodeIds)

        verify(addCounterToNodeNameUseCase).invoke("file.txt", node1, 1)
    }

    @Test
    fun `test that null node from getNodeByIdUseCase is skipped`() = runTest {
        val node1 = NodeId(123)
        val node2 = NodeId(456)
        val nodeIds = listOf(node1, node2)

        val fileNode = mock<TypedNode>()
        whenever(fileNode.name).thenReturn("file.txt")

        whenever(getNodeByIdUseCase(node1)).thenReturn(fileNode)
        whenever(getNodeByIdUseCase(node2)).thenReturn(null)

        underTest(nodeIds)

        // When there are multiple nodes, first is skipped, so only second should be processed
        // But since second returns null, no nodes should be processed
        verifyNoInteractions(addCounterToNodeNameUseCase)
    }

    @Test
    fun `test that empty node list does not invoke any use cases`() = runTest {
        val nodeIds = emptyList<NodeId>()

        underTest(nodeIds)

        verifyNoInteractions(addCounterToNodeNameUseCase)
        verifyNoInteractions(getNodeByIdUseCase)
    }

    @Test
    fun `test that folder nodes are handled correctly`() = runTest {
        val node1 = NodeId(123)
        val node2 = NodeId(456)
        val nodeIds = listOf(node1, node2)

        val folderNode1 = mock<TypedNode>()
        val folderNode2 = mock<TypedNode>()

        whenever(folderNode1.name).thenReturn("folder")
        whenever(folderNode2.name).thenReturn("FOLDER")

        whenever(getNodeByIdUseCase(node1)).thenReturn(folderNode1)
        whenever(getNodeByIdUseCase(node2)).thenReturn(folderNode2)

        underTest(nodeIds)

        verify(addCounterToNodeNameUseCase).invoke("FOLDER", node2, 1)
        verify(addCounterToNodeNameUseCase, times(0)).invoke("folder", node1, 1)
    }
}
