package mega.privacy.android.domain.usecase.chat.message.forward

import com.google.common.truth.Truth.assertThat
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
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ForwardMessagesUseCaseTest {

    private lateinit var underTest: ForwardMessagesUseCase

    private val createChatRoomUseCase = mock<CreateChatRoomUseCase>()
    private val forwardNormalMessageUseCase = mock<ForwardMessageUseCase>()
    private val forwardContactUseCase = mock<ForwardMessageUseCase>()

    private val forwardMessageUseCases = setOf(
        forwardNormalMessageUseCase,
        forwardContactUseCase,
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
    fun `test that number of results match expected value if all messages are forwarded`() =
        runTest {

            forwardNormalMessageUseCase.stub {
                onBlocking { invoke(any(), any()) } doReturn emptyList()
                onBlocking {
                    invoke(
                        any(),
                        argWhere { it is NormalMessage })
                } doAnswer { invocationOnMock ->
                    val chatCount = (invocationOnMock.arguments[0] as List<*>).size
                    buildList { repeat(chatCount) { add(ForwardResult.Success(-1)) } }
                }
            }

            forwardContactUseCase.stub {
                onBlocking { invoke(any(), any()) } doReturn emptyList()
                onBlocking {
                    invoke(
                        any(),
                        argWhere { it is ContactAttachmentMessage })
                } doAnswer { invocationOnMock ->
                    val chatCount = (invocationOnMock.arguments[0] as List<*>).size
                    buildList { repeat(chatCount) { add(ForwardResult.Success(-1)) } }
                }
            }

            val expectedSize = messagesToForward.size * chatHandles.size

            val actual = underTest(messagesToForward, chatHandles, null)

            assertThat(actual.size).isEqualTo(expectedSize)
            assertThat(actual.all { it == ForwardResult.Success(-1) }).isTrue()
        }

    @Test
    internal fun `test that messages are forwarded in order of time sent`() = runTest {
        message2.stub { on { time } doReturn 0 }
        message1.stub { on { time } doReturn 10 }

        forwardNormalMessageUseCase.stub {
            onBlocking { invoke(any(), any()) } doReturn emptyList()
        }

        forwardContactUseCase.stub {
            onBlocking { invoke(any(), any()) } doReturn emptyList()
        }

        underTest(messagesToForward, chatHandles, null)

        inOrder(forwardNormalMessageUseCase, forwardContactUseCase) {
            forwardNormalMessageUseCase(chatHandles, message2)
            forwardContactUseCase(chatHandles, message2)
            forwardNormalMessageUseCase(chatHandles, message1)
            forwardContactUseCase(chatHandles, message1)
        }
    }
}