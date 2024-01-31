package mega.privacy.android.domain.usecase.filenode

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsMyNodeUseCaseTest {

    private val nodeRepository: NodeRepository = mock()
    private val underTest = IsMyNodeUseCase(nodeRepository = nodeRepository)

    @ParameterizedTest(name = "test with user handle {0} with fingerprint node {1} exists {3}")
    @MethodSource("provideParams")
    fun `test that provided node is my node`(
        fileNode: FileNode,
        fingerPrintNodes: List<Node>,
        expected: Boolean,
    ) = runTest {
        whenever(nodeRepository.getMyUserHandleBinary()).thenReturn(USER_HANDLE)
        whenever(nodeRepository.getOwnerNode(NodeId(1234L))).thenReturn(USER_HANDLE)
        whenever(nodeRepository.getOwnerNode(NodeId(2345L))).thenReturn(null)
        whenever(nodeRepository.getOwnerNode(NodeId(3456L))).thenReturn(3456L)
        whenever(nodeRepository.getNodesFromFingerPrint(FINGER_PRINT)).thenReturn(fingerPrintNodes)
        val actual = underTest(fileNode)
        assertThat(actual).isEqualTo(expected)
    }

    @AfterEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    private fun provideParams() = Stream.of(
        Arguments.of(
            mock<FileNode> {
                whenever(it.id).thenReturn(NodeId(1234L))
            },
            listOf(mock<TypedFileNode>()),
            true
        ),
        Arguments.of(
            mock<FileNode> {
                whenever(it.id).thenReturn(NodeId(2345L))
                whenever(it.fingerprint).thenReturn(FINGER_PRINT)
            },
            listOf(mock<TypedFileNode> {
                whenever(it.id).thenReturn(NodeId(2345L))
            },
                mock<TypedFileNode> {
                    whenever(it.id).thenReturn(NodeId(1234L))
                }),
            true
        ),
        Arguments.of(
            mock<FileNode> {
                whenever(it.id).thenReturn(NodeId(2345L))
                whenever(it.fingerprint).thenReturn(FINGER_PRINT)
            },
            listOf(mock<TypedFileNode> {
                whenever(it.id).thenReturn(NodeId(2345L))
            },
                mock<TypedFileNode> {
                    whenever(it.id).thenReturn(NodeId(3456L))
                }),
            false
        )
    )

    companion object {
        private const val USER_HANDLE = 1234L
        private const val FINGER_PRINT = "fingerprint"
    }
}