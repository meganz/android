package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoveNodesToRubbishUseCaseTest {
    private lateinit var underTest: MoveNodesToRubbishUseCase

    private val moveNodeUseCase: MoveNodeUseCase = mock()
    private val nodeRepository: NodeRepository = mock()

    @BeforeAll
    fun setUp() {
        underTest = MoveNodesToRubbishUseCase(
            moveNodeUseCase = moveNodeUseCase,
            nodeRepository = nodeRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(moveNodeUseCase, nodeRepository)
    }

    @Test
    fun `test that throw NodeDoesNotExistsException when call getRubbishNode return nulls`() =
        runTest {
            whenever(nodeRepository.getRubbishNode()).thenReturn(null)
            try {
                underTest.invoke(listOf(1L))
            } catch (e: Exception) {
                Truth.assertThat(e).isInstanceOf(NodeDoesNotExistsException::class.java)
            }
        }

    @Test
    fun `test that returns correctly when move single node to rubbish bin successfully`() =
        runTest {
            val nodeHandle = 1L
            val node = mock<FileNode> {
                on { id }.thenReturn(NodeId(nodeHandle))
                on { parentId }.thenReturn(NodeId(100L))
            }
            val rubbishNode = mock<FolderNode> {
                on { id }.thenReturn(NodeId(2L))
            }
            whenever(nodeRepository.getRubbishNode()).thenReturn(rubbishNode)
            whenever(nodeRepository.getNodeByHandle(nodeHandle)).thenReturn(node)
            whenever(moveNodeUseCase.invoke(NodeId(1L), NodeId(2L))).thenReturn(NodeId(1L))
            val result = underTest.invoke(listOf(nodeHandle))
            Truth.assertThat(result.count).isEqualTo(1)
            Truth.assertThat(result.successCount).isEqualTo(1)
            Truth.assertThat(result.errorCount).isEqualTo(0)
            Truth.assertThat(result.oldParentHandle).isEqualTo(100L)
        }

    @Test
    fun `test that returns correctly when move multiple nodes to rubbish bin successfully`() =
        runTest {
            val nodeHandle1 = 1L
            val nodeHandle2 = 2L
            val rubbishNodeHandle = 3L
            val node1 = mock<FileNode> {
                on { id }.thenReturn(NodeId(nodeHandle1))
                on { parentId }.thenReturn(NodeId(100L))
            }
            val node2 = mock<FileNode> {
                on { id }.thenReturn(NodeId(nodeHandle2))
                on { parentId }.thenReturn(NodeId(100L))
            }
            val rubbishNode = mock<FolderNode> {
                on { id }.thenReturn(NodeId(rubbishNodeHandle))
            }
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(nodeRepository.getRubbishNode()).thenReturn(rubbishNode)
            whenever(nodeRepository.getNodeByHandle(nodeHandle1)).thenReturn(node1)
            whenever(nodeRepository.getNodeByHandle(nodeHandle2)).thenReturn(node2)
            whenever(
                moveNodeUseCase.invoke(
                    NodeId(nodeHandle1),
                    NodeId(rubbishNodeHandle)
                )
            ).thenReturn(NodeId(nodeHandle1))
            whenever(
                moveNodeUseCase.invoke(
                    NodeId(nodeHandle2),
                    NodeId(rubbishNodeHandle)
                )
            ).thenThrow(RuntimeException::class.java)
            val result = underTest.invoke(listOf(nodeHandle1, nodeHandle2))
            Truth.assertThat(result.count).isEqualTo(2)
            Truth.assertThat(result.successCount).isEqualTo(1)
            Truth.assertThat(result.errorCount).isEqualTo(1)
            Truth.assertThat(result.oldParentHandle).isEqualTo(-1L)
        }
}