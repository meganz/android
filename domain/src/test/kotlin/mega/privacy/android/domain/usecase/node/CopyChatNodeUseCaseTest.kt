package mega.privacy.android.domain.usecase.node


import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CopyChatNodeCaseTest {

    private val getChatFileUseCase: GetChatFileUseCase = mock()
    private val copyTypedNodeUseCase: CopyTypedNodeUseCase = mock()

    private lateinit var underTest: CopyChatNodeUseCase

    @BeforeEach
    fun setUp() {
        underTest = CopyChatNodeUseCase(getChatFileUseCase, copyTypedNodeUseCase)
    }

    @BeforeEach
    fun resetMocks() = reset(
        getChatFileUseCase,
        copyTypedNodeUseCase,
    )

    @Test
    fun `test that copyTypedNodeUseCase is called when chatFile is not null`() = runTest {
        val expectedNodeId = NodeId(1L)
        val chatFile: ChatDefaultFile = mock()
        val newNodeParent = NodeId(1000000L)
        val newNodeName = "newNodeName"
        val chatId = 1L
        val messageId = 1L
        val messageIndex = 0

        whenever(getChatFileUseCase(chatId, messageId, messageIndex)).thenReturn(chatFile)
        whenever(copyTypedNodeUseCase.invoke(chatFile, newNodeParent, newNodeName)).thenReturn(
            expectedNodeId
        )

        val result = underTest(chatId, messageId, messageIndex, newNodeName, newNodeParent)

        assertThat(result).isEqualTo(expectedNodeId)
    }

    @Test
    fun `test that IllegalStateException is thrown when chatFile is null`() = runTest {
        val newNodeParent = NodeId(1000000L)
        val newNodeName = "newNodeName"
        val chatId = 1L
        val messageId = 1L
        val messageIndex = 0

        whenever(getChatFileUseCase(chatId, messageId, messageIndex)).thenReturn(null)

        assertThrows<IllegalStateException> {
            underTest(chatId, messageId, messageIndex, newNodeName, newNodeParent)
        }
    }
}