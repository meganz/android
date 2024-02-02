package mega.privacy.android.domain.usecase.node.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.ChatRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.chat.AttachMultipleNodesUseCase
import mega.privacy.android.domain.usecase.chat.AttachNodeUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttachMultipleNodesUseCaseTest {
    private val attachNodeUseCase: AttachNodeUseCase = mock()
    private val underTest = AttachMultipleNodesUseCase(attachNodeUseCase)

    @ParameterizedTest(name = "Test {0} and {1}")
    @MethodSource("provideParams")
    fun `test that attach Nodes to chat returns proper chat request`(
        nodeIds: List<NodeId>,
        chatIds: LongArray,
        expected: ChatRequestResult,
    ) = runTest {
        whenever(
            attachNodeUseCase(
                nodeId = NodeId(SUCCESS_NODE_HANDLE_1),
                chatId = CHAT_HANDLE_1
            )
        ).thenReturn(12L)
        whenever(
            attachNodeUseCase(
                nodeId = NodeId(SUCCESS_NODE_HANDLE_1),
                chatId = CHAT_HANDLE_2
            )
        ).thenReturn(12L)
        whenever(
            attachNodeUseCase(
                nodeId = NodeId(SUCCESS_NODE_HANDLE_2),
                chatId = CHAT_HANDLE_1
            )
        ).thenReturn(12L)
        whenever(
            attachNodeUseCase(
                nodeId = NodeId(SUCCESS_NODE_HANDLE_2),
                chatId = CHAT_HANDLE_2
            )
        ).thenReturn(12L)
        whenever(
            attachNodeUseCase(
                nodeId = NodeId(FAILED_NODE_HANDLE),
                chatId = CHAT_HANDLE_1
            )
        ).thenThrow(RuntimeException::class.java)
        whenever(
            attachNodeUseCase(
                nodeId = NodeId(FAILED_NODE_HANDLE),
                chatId = CHAT_HANDLE_2
            )
        ).thenThrow(RuntimeException::class.java)
        val actual = underTest(nodeIds = nodeIds, chatIds = chatIds)
        assertThat(actual.count).isEqualTo(expected.count)
        assertThat(actual.isSuccess).isEqualTo(expected.isSuccess)
        assertThat(actual.isAllRequestError).isEqualTo(expected.isAllRequestError)
    }

    private fun provideParams() = Stream.of(
        Arguments.of(
            listOf(
                NodeId(SUCCESS_NODE_HANDLE_1),
                NodeId(SUCCESS_NODE_HANDLE_2)
            ),
            longArrayOf(CHAT_HANDLE_1, CHAT_HANDLE_2),
            ChatRequestResult.ChatRequestAttachNode(count = 2, errorCount = 0)
        ),
        Arguments.of(
            listOf(
                NodeId(SUCCESS_NODE_HANDLE_1),
                NodeId(SUCCESS_NODE_HANDLE_2),
                NodeId(FAILED_NODE_HANDLE)
            ),
            longArrayOf(CHAT_HANDLE_1, CHAT_HANDLE_2),
            ChatRequestResult.ChatRequestAttachNode(count = 3, errorCount = 1)
        ),
        Arguments.of(
            listOf(
                NodeId(FAILED_NODE_HANDLE)
            ),
            longArrayOf(CHAT_HANDLE_1, CHAT_HANDLE_2),
            ChatRequestResult.ChatRequestAttachNode(count = 1, errorCount = 1)
        )
    )

    companion object {
        private const val SUCCESS_NODE_HANDLE_1 = 1234L
        private const val SUCCESS_NODE_HANDLE_2 = 2345L
        private const val FAILED_NODE_HANDLE = 3456L
        private const val CHAT_HANDLE_1 = 12L
        private const val CHAT_HANDLE_2 = 34L

    }

    @AfterEach
    fun resetMocks() {
        reset(attachNodeUseCase)
    }
}