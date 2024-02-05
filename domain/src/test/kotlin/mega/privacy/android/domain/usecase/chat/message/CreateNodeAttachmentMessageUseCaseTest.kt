package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateNodeAttachmentMessageUseCaseTest {
    private lateinit var underTest: CreateNodeAttachmentMessageUseCase

    private val createInvalidMessageUseCase = mock<CreateInvalidMessageUseCase>()

    @BeforeAll
    internal fun setUp() {
        underTest = CreateNodeAttachmentMessageUseCase(createInvalidMessageUseCase)
    }

    @BeforeEach
    internal fun resetMocks() = reset(createInvalidMessageUseCase)

    @Test
    fun `test that if message has no nodes it returns an invalid message`() {
        val message = mock<ChatMessage> {
            on { nodeList } doReturn emptyList()
        }
        val expected = mock<InvalidMessage>()
        whenever(createInvalidMessageUseCase(any())).thenReturn(expected)
        val actual = underTest(buildRequest(message))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that if message has no File nodes it returns an invalid message`() {
        val message = mock<ChatMessage> {
            on { nodeList } doReturn listOf(mock<FolderNode>())
        }
        val expected = mock<InvalidMessage>()
        whenever(createInvalidMessageUseCase(any())).thenReturn(expected)
        val actual = underTest(buildRequest(message))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that the use case returns the correctly mapped message`() {
        val fileNode = mock<FileNode>()
        val message = mock<ChatMessage> {
            on { nodeList } doReturn listOf(fileNode)
        }
        val request = buildRequest(message)
        val expected = with(request) {
            NodeAttachmentMessage(
                msgId = msgId,
                time = timestamp,
                isMine = isMine,
                userHandle = userHandle,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                shouldShowDate = shouldShowDate,
                fileNode = fileNode,
                reactions = emptyList(),
            )
        }
        val actual = underTest(request)
        assertThat(actual).isEqualTo(expected)
    }

    private fun buildRequest(message: ChatMessage) =
        CreateTypedMessageRequest(
            chatMessage = message,
            isMine = true,
            shouldShowAvatar = true,
            shouldShowTime = true,
            shouldShowDate = true,
            reactions = emptyList(),
        )
}