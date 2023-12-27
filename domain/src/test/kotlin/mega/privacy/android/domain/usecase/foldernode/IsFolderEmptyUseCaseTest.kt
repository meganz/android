package mega.privacy.android.domain.usecase.foldernode

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
class IsFolderEmptyUseCaseTest {
    private val nodeRepository: NodeRepository = mock()
    private val underTest = IsFolderEmptyUseCase(nodeRepository)

    @ParameterizedTest(name = "test is folder empty with expected empty {1}")
    @MethodSource("provideParams")
    fun `test that use case returns appropriate value when folder is empty `(
        node: UnTypedNode,
        expected: Boolean,
    ) = runTest {
        val node2 = mock<FolderNode> {
            whenever(it.id).thenReturn(NodeId(2L))
        }
        val node3 = mock<FolderNode> {
            whenever(it.id).thenReturn(NodeId(3L))
        }
        val node4 = mock<FileNode> {
            whenever(it.id).thenReturn(NodeId(4L))
        }
        val nonEmptyList = mutableListOf(node2, node3, node4)

        val node6 = mock<FolderNode> {
            whenever(it.id).thenReturn(NodeId(6L))
        }
        val node7 = mock<FolderNode> {
            whenever(it.id).thenReturn(NodeId(7L))
        }
        val node8 = mock<FolderNode> {
            whenever(it.id).thenReturn(NodeId(8L))
        }
        val node9 = mock<FolderNode> {
            whenever(it.id).thenReturn(NodeId(9L))
        }
        val emptyList = mutableListOf(node6, node7, node8, node9)
        whenever(nodeRepository.getNodeChildren(nodeId = NodeId(1L), order = null)).thenReturn(
            nonEmptyList
        )
        whenever(nodeRepository.getNodeChildren(nodeId = NodeId(5L), order = null)).thenReturn(
            emptyList
        )
        whenever(nodeRepository.getNodeChildren(nodeId = NodeId(2L), order = null)).thenReturn(
            mutableListOf()
        )
        whenever(nodeRepository.getNodeChildren(nodeId = NodeId(3L), order = null)).thenReturn(
            listOf(node4)
        )
        whenever(nodeRepository.getNodeChildren(nodeId = NodeId(4L), order = null)).thenReturn(
            mutableListOf()
        )
        whenever(nodeRepository.getNodeChildren(nodeId = NodeId(6L), order = null)).thenReturn(
            mutableListOf()
        )
        whenever(nodeRepository.getNodeChildren(nodeId = NodeId(7L), order = null)).thenReturn(
            mutableListOf()
        )
        whenever(nodeRepository.getNodeChildren(nodeId = NodeId(8L), order = null)).thenReturn(
            mutableListOf()
        )
        whenever(nodeRepository.getNodeChildren(nodeId = NodeId(9L), order = null)).thenReturn(
            mutableListOf()
        )
        val actual = underTest(node)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParams() = Stream.of(
        Arguments.of(mock<FileNode>(), false),
        Arguments.of(mock<FolderNode> {
            whenever(it.id).thenReturn(NodeId(1L))
        }, false),
        Arguments.of(mock<FolderNode> {
            whenever(it.id).thenReturn(NodeId(5L))
        }, true)
    )
}