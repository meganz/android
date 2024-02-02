package mega.privacy.android.domain.usecase.filenode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.AddNodeType
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
class GetOwnNodeUseCaseTest {

    private val nodeRepository: NodeRepository = mock()
    private val addNodeType: AddNodeType = mock()
    private val underTest = GetOwnNodeUseCase(
        nodeRepository = nodeRepository,
        addNodeType = addNodeType
    )
    private val expectedUntypedNode = mock<FileNode> {
        whenever(it.id).thenReturn(NodeId(FOUND_USER_HANDLE))
    }
    private val expectedTypedNode = mock<DefaultTypedFileNode> {
        whenever(it.id).thenReturn(NodeId(FOUND_USER_HANDLE))
    }

    @ParameterizedTest(name = "test with user handle {0} with fingerprint node {1} exists {3}")
    @MethodSource("provideParams")
    fun `test that provided node is my node`(
        fileNode: TypedFileNode,
        fingerPrintNodes: List<UnTypedNode>,
        expected: TypedNode?,
    ) = runTest {
        whenever(nodeRepository.getMyUserHandleBinary()).thenReturn(FOUND_USER_HANDLE)
        whenever(nodeRepository.getOwnerNodeHandle(NodeId(FOUND_USER_HANDLE))).thenReturn(
            FOUND_USER_HANDLE
        )
        whenever(nodeRepository.getOwnerNodeHandle(NodeId(NOT_FOUND_USER_HANDLE_1))).thenReturn(null)
        whenever(nodeRepository.getOwnerNodeHandle(NodeId(NOT_FOUND_USER_HANDLE_2))).thenReturn(
            NOT_FOUND_USER_HANDLE_2
        )
        whenever(nodeRepository.getNodesFromFingerPrint(FINGER_PRINT)).thenReturn(fingerPrintNodes)
        whenever(addNodeType(expectedUntypedNode)).thenReturn(expected)
        val actual = underTest(fileNode)
        assertThat(actual).isEqualTo(expected)
    }

    @AfterEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    private fun provideParams() = Stream.of(
        Arguments.of(
            expectedTypedNode,
            listOf(mock<FileNode>()),
            expectedTypedNode
        ),
        Arguments.of(
            mock<DefaultTypedFileNode> {
                whenever(it.id).thenReturn(NodeId(NOT_FOUND_USER_HANDLE_1))
                whenever(it.fingerprint).thenReturn(FINGER_PRINT)
            },
            listOf(
                expectedUntypedNode,
                mock<FileNode> {
                    whenever(it.id).thenReturn(NodeId(FOUND_USER_HANDLE))
                }),
            expectedTypedNode
        ),
        Arguments.of(
            mock<DefaultTypedFileNode> {
                whenever(it.id).thenReturn(NodeId(NOT_FOUND_USER_HANDLE_1))
                whenever(it.fingerprint).thenReturn(FINGER_PRINT)
            },
            listOf(
                mock<FileNode> {
                    whenever(it.id).thenReturn(NodeId(NOT_FOUND_USER_HANDLE_1))
                },
                mock<TypedFileNode> {
                    whenever(it.id).thenReturn(NodeId(NOT_FOUND_USER_HANDLE_2))
                }
            ),
            null
        )
    )

    companion object {
        private const val FOUND_USER_HANDLE = 1234L
        private const val NOT_FOUND_USER_HANDLE_1 = 2345L
        private const val NOT_FOUND_USER_HANDLE_2 = 3456L
        private const val FINGER_PRINT = "fingerprint"
    }
}