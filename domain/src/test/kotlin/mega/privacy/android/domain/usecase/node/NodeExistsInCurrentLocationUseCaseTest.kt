package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeExistsInCurrentLocationUseCaseTest {
    private val nodeRepository: NodeRepository = mock()

    private val underTest = NodeExistsInCurrentLocationUseCase(nodeRepository)

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
        nodeList: List<UnTypedNode>,
        expected: Boolean
    ) = runTest {
        val currentNodeMock = mock<FileNode> {
            whenever(it.id).thenReturn(NodeId(123L))
        }
        whenever(nodeRepository.getNodeChildren(currentNodeMock.id, null)).thenReturn(nodeList)
        val actual = underTest(currentNodeMock, providedName)
        Truth.assertThat(expected).isEqualTo(actual)
    }

    private fun provideParams() = Stream.of(
        Arguments.of("SameName", listOf(node2, node3, node1), true),
        Arguments.of("SameName", listOf(node2, node3), false),
    )
}