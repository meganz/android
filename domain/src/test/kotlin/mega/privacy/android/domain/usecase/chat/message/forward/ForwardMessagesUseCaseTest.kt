package mega.privacy.android.domain.usecase.chat.message.forward

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.exception.chat.CreateChatException
import mega.privacy.android.domain.exception.chat.ForwardException
import mega.privacy.android.domain.usecase.chat.CreateChatRoomUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.reset
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ForwardMessagesUseCaseTest {

    private lateinit var underTest: ForwardMessagesUseCase

    private val createChatRoomUseCase = mock<CreateChatRoomUseCase>()
    private val forwardNormalMessageUseCase = mock<ForwardNormalMessageUseCase>()
    private val forwardContactUseCase = mock<ForwardContactUseCase>()
    private val forwardNodeAttachmentUseCase = mock<ForwardNodeAttachmentUseCase>()
    private val forwardVoiceClipUseCase = mock<ForwardVoiceClipUseCase>()
    private val forwardRichPreviewUseCase = mock<ForwardRichPreviewUseCase>()
    private val forwardLocationUseCase = mock<ForwardLocationUseCase>()
    private val forwardGiphyUseCase = mock<ForwardGiphyUseCase>()

    private val targetChatId = 789L
    private val forwardMessageUseCases = setOf(
        forwardNormalMessageUseCase,
        forwardContactUseCase,
        forwardNodeAttachmentUseCase,
        forwardVoiceClipUseCase,
        forwardRichPreviewUseCase,
        forwardLocationUseCase,
        forwardGiphyUseCase,
    )
    private val message1 = mock<NormalMessage>()
    private val message2 = mock<ContactAttachmentMessage>()
    private val messagesToForward = listOf(message1, message2)
    private val contact1 = 123L
    private val contact2 = 234L
    private val contactHandles = listOf(contact1, contact2)
    private val chatHandle = 567L
    private val chatHandles = listOf(chatHandle)

    @BeforeEach
    fun setup() {
        underTest = ForwardMessagesUseCase(
            forwardMessageUseCases = forwardMessageUseCases,
            createChatRoomUseCase = createChatRoomUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            createChatRoomUseCase,
            forwardNormalMessageUseCase,
            forwardContactUseCase,
            forwardNodeAttachmentUseCase,
            forwardVoiceClipUseCase,
            forwardRichPreviewUseCase,
            forwardLocationUseCase,
            forwardGiphyUseCase,
        )
    }

    @Test
    fun `test that exception is thrown if there are no messages to forward`() = runTest {
        assertThrows<ForwardException> {
            underTest(emptyList(), chatHandles, contactHandles)
        }
    }

    @Test
    fun `test that exception is thrown if there are no chats to forward`() = runTest {
        assertThrows<ForwardException> {
            underTest(messagesToForward, emptyList(), emptyList())
        }
    }

    @Test
    fun `test that exception is thrown if there are no chats to forward and chat creation failed`() =
        runTest {
            val contactHandles = listOf(contact1)
            whenever(createChatRoomUseCase(false, contactHandles)).thenThrow(RuntimeException())
            assertThrows<CreateChatException> {
                underTest(messagesToForward, emptyList(), contactHandles)
            }
        }

    @Test
    fun `test that exception is thrown if there are no chats to forward and all chat creations failed`() =
        runTest {
            whenever(createChatRoomUseCase(false, listOf(contact1))).thenThrow(RuntimeException())
            whenever(createChatRoomUseCase(false, listOf(contact2))).thenThrow(RuntimeException())
            assertThrows<CreateChatException> {
                underTest(messagesToForward, emptyList(), contactHandles)
            }
        }

    @Test
    fun `test that messages are forwarded`() = runTest {
        val result = listOf<ForwardResult>(
            ForwardResult.Success,
            ForwardResult.Success,
            ForwardResult.Success,
            ForwardResult.Success,
            ForwardResult.Success,
            ForwardResult.Success,
        )
    }
}