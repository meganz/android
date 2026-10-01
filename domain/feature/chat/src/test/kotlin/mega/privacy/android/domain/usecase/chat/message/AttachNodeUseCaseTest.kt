package mega.privacy.android.domain.usecase.chat.message


import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.chat.GetChatMessageUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttachNodeUseCaseTest {

    private lateinit var underTest: AttachNodeUseCase

    private val chatRepository = mock<ChatRepository>()
    private val chatMessageRepository = mock<ChatMessageRepository>()
    private val createSaveSentMessageRequestUseCase = mock<CreateSaveSentMessageRequestUseCase>()
    private val getChatMessageUseCase = mock<GetChatMessageUseCase>()
    private val getAttachableNodeIdUseCase = mock<GetAttachableNodeIdUseCase>()

    private val chatId = 123L
    private val nodeHandle = 456L
    private val msgId = 789L
    private val fileNode = mock<TypedFileNode> {
        on { id }.thenReturn(NodeId(nodeHandle))
    }
    private val message = mock<ChatMessage> {
        on { this.messageId } doReturn msgId
    }

    @BeforeEach
    fun setup() {
        underTest = AttachNodeUseCase(
            chatRepository,
            chatMessageRepository,
            createSaveSentMessageRequestUseCase,
            getChatMessageUseCase,
            getAttachableNodeIdUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            chatRepository,
            chatMessageRepository,
            createSaveSentMessageRequestUseCase,
            getChatMessageUseCase,
            getAttachableNodeIdUseCase,
        )
        commonStub()
    }

    private fun commonStub() = runTest {
        whenever(getAttachableNodeIdUseCase(any())) doAnswer {
            (it.arguments[0] as Node).id
        }
    }

    @Test
    fun `test that message is attached`() =
        runTest {
            whenever(chatMessageRepository.attachNode(chatId, NodeId(nodeHandle))).thenReturn(msgId)
            whenever(getChatMessageUseCase(chatId, msgId)).thenReturn(message)
            underTest.invoke(chatId = chatId, fileNode)
            verify(chatMessageRepository).attachNode(chatId, NodeId(nodeHandle))
        }


    @Test
    fun `test that message is stored`() = runTest {
        whenever(chatMessageRepository.attachNode(chatId, NodeId(nodeHandle))).thenReturn(msgId)
        whenever(getChatMessageUseCase(chatId, msgId)).thenReturn(message)
        val request = mock<CreateTypedMessageRequest>()
        whenever(createSaveSentMessageRequestUseCase(message, chatId)).thenReturn(request)
        underTest.invoke(chatId = chatId, fileNode)
        verify(chatRepository).storeMessages(listOf(request))
    }

    @Test
    fun `test that the id returned by getAttachableNodeIdUseCase is used to attach the node`() =
        runTest {
            val incomingId = nodeHandle + 15L
            whenever(getAttachableNodeIdUseCase(fileNode)).thenReturn(NodeId(incomingId))
            underTest.invoke(chatId = chatId, fileNode)
            verify(chatMessageRepository).attachNode(chatId, NodeId(incomingId))
        }
}