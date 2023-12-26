package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeExistsInParentUseCaseTest {

    private val nodeRepository: NodeRepository = mock()

    private val node = mock<FileNode>()

    private val underTest = NodeExistsInParentUseCase(nodeRepository)

    private val node1 = mock<FolderNode> {
        whenever(it.name).thenReturn("SameName")
    }
    private val node2 = mock<FileNode> {
        whenever(it.name).thenReturn("ABC")
    }
    private val node3 = mock<FileNode> {
        whenever(it.name).thenReturn("XYZ")
    }

    @ParameterizedTest(name = "Search Node with name for {0}")
    @MethodSource("provideParams")
    fun `test that provided name and a node is list is same`(
        providedName: String,
        untypedNode: UnTypedNode,
        nodeList: List<UnTypedNode>,
        expected: Boolean
    ) = runTest {
        val parentMock = mock<FileNode> {
            whenever(it.id).thenReturn(NodeId(123L))
        }
        whenever(nodeRepository.getParentNode(untypedNode.id)).thenReturn(parentMock)
        whenever(nodeRepository.getNodeChildren(parentMock.id, null)).thenReturn(nodeList)
        val actual = underTest(untypedNode, providedName)
        Truth.assertThat(expected).isEqualTo(actual)
    }

    private fun provideParams() = Stream.of(
        Arguments.of("SameName", node, listOf(node2, node3, node1), true),
        Arguments.of("SameName", node, listOf(node2, node3), false),
    )
}