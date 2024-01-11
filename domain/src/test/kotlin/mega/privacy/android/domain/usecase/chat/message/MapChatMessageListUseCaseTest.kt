package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.FormatInvalidMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argForWhich
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.util.concurrent.TimeUnit


class MapChatMessageListUseCaseTest {
    private lateinit var underTest: MapChatMessageListUseCase

    private val createTypedMessageUseCase = mock<CreateTypedMessageUseCase>()
    private val map = mapOf(ChatMessageType.NORMAL to createTypedMessageUseCase)
    private val createInvalidMessageUseCase = mock<CreateInvalidMessageUseCase>()
    private val myHandle = 123L

    @BeforeEach
    internal fun initUseCase() {
        underTest = MapChatMessageListUseCase(
            createTypedMessageUseCases = map,
            createInvalidMessageUseCase = createInvalidMessageUseCase
        )
    }

    @Test
    fun `test that messages with matching types are mapped`() {
        val message = mock<ChatMessage> {
            on { type } doReturn ChatMessageType.NORMAL
            on { userHandle } doReturn 123L
        }
        val list = listOf(message)

        val expectedMessage = mock<TypedMessage>()
        createTypedMessageUseCase.stub {
            on {
                invoke(any())
            } doReturn expectedMessage
        }

        val result = underTest(list, 123L, 321L)

        assertThat(result).containsExactly(expectedMessage)
    }

    @Test
    fun `test that messages with non-matching types are mapped to invalid messages`() {
        val message = mock<ChatMessage> {
            on { type } doReturn ChatMessageType.UNKNOWN
            on { userHandle } doReturn 123L
        }

        val invalidMessage = mock<FormatInvalidMessage>()
        createInvalidMessageUseCase.stub {
            on {
                invoke(any())
            } doReturn invalidMessage
        }

        val list = listOf(message)

        val result = underTest(list, 123L, 321L)

        assertThat(result).hasSize(1)
        assertThat(result.first()).isEqualTo(invalidMessage)
    }


    @Test
    fun `test that show avatar is false for my messages`() {
        val myMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(myHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
        }

        val expectedMessage = mock<TypedMessage>()
        createTypedMessageUseCase.stub {
            on {
                invoke(any())
            } doReturn expectedMessage
        }

        underTest(
            chatMessages = listOf(myMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        verify(createTypedMessageUseCase).invoke(
            argForWhich {
                !shouldShowAvatar
            }
        )
    }

    @Test
    fun `test that showAvatar is true for a single message with no next message`() {

        val notMyMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(myHandle + 1)
            on { type }.thenReturn(ChatMessageType.NORMAL)
        }

        val expectedMessage = mock<TypedMessage>()
        createTypedMessageUseCase.stub {
            on {
                invoke(any())
            } doReturn expectedMessage
        }

        underTest(
            chatMessages = listOf(notMyMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        verify(createTypedMessageUseCase).invoke(
            argForWhich {
                shouldShowAvatar
            }
        )
    }

    @Test
    fun `test that avatar is true only for second message of two with the same user handle and no next message`() {

        val firstMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(myHandle + 1)
            on { type }.thenReturn(ChatMessageType.NORMAL)
        }

        val secondMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(myHandle + 1)
            on { type }.thenReturn(ChatMessageType.NORMAL)
        }

        val expectedMessage = mock<TypedMessage>()
        createTypedMessageUseCase.stub {
            on {
                invoke(any())
            } doReturn expectedMessage
        }

        underTest(
            chatMessages = listOf(firstMessage, secondMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        verify(createTypedMessageUseCase).invoke(
            argForWhich {
                !shouldShowAvatar
            }
        )

        verify(createTypedMessageUseCase).invoke(
            argForWhich {
                shouldShowAvatar
            }
        )
    }


    @Test
    fun `test that show avatar is false for a single message with a next message with the same user handle`() {
        val notMyHandle = myHandle + 1
        val notMyMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(notMyHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
        }

        val expectedMessage = mock<TypedMessage>()
        createTypedMessageUseCase.stub {
            on {
                invoke(any())
            } doReturn expectedMessage
        }

        underTest(
            chatMessages = listOf(notMyMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = notMyHandle,
        )

        verify(createTypedMessageUseCase).invoke(
            argForWhich {
                !shouldShowAvatar
            }
        )
    }

    @Test
    fun `test that time is displayed if first message`() {
        val myMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(myHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
        }

        val expectedMessage = mock<TypedMessage>()
        createTypedMessageUseCase.stub {
            on {
                invoke(any())
            } doReturn expectedMessage
        }

        underTest(
            chatMessages = listOf(myMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        verify(createTypedMessageUseCase).invoke(
            argForWhich {
                shouldShowTime
            }
        )
    }

    @Test
    fun `test that time is displayed for two messages from different senders`() {
        val initialTime = 100L
        val firstUser = myHandle + 1
        val firstMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(firstUser)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { timestamp }.thenReturn(initialTime)
        }

        val newTime = initialTime + TimeUnit.MINUTES.toSeconds(3) + 1
        val secondUser = firstUser + 1
        val secondMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(secondUser)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { timestamp }.thenReturn(newTime)
        }

        underTest(
            chatMessages = listOf(firstMessage, secondMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        verify(createTypedMessageUseCase, times(2)).invoke(
            argForWhich {
                shouldShowTime
            }
        )

    }

    @Test
    fun `test that time is only displayed for the first of two messages from the same user within 3 minutes`() {
        val initialTime = 100L
        val notMyHandle = myHandle + 1
        val firstMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(notMyHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { timestamp }.thenReturn(initialTime)
        }

        val newTime = initialTime + TimeUnit.MINUTES.toSeconds(3) - 1
        val secondMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(notMyHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { timestamp }.thenReturn(newTime)
        }

        underTest(
            chatMessages = listOf(firstMessage, secondMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        verify(createTypedMessageUseCase).invoke(
            argForWhich {
                shouldShowTime
            }
        )

        verify(createTypedMessageUseCase).invoke(
            argForWhich {
                !shouldShowTime
            }
        )
    }

    @Test
    fun `test that time is displayed on both messages from the same sender if more than 3 minutes apart`() {
        val initialTime = 100L
        val notMyHandle = myHandle + 1
        val firstMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(notMyHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { timestamp }.thenReturn(initialTime)
        }

        val newTime = initialTime + TimeUnit.MINUTES.toSeconds(3) + 1
        val secondMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(notMyHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { timestamp }.thenReturn(newTime)
        }

        underTest(
            chatMessages = listOf(firstMessage, secondMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        verify(createTypedMessageUseCase, times(2)).invoke(
            argForWhich {
                shouldShowTime
            }
        )
    }


    @Test
    fun `test that date is not shown for second message sent on the same date`() {
        val initialTime = 100L
        val notMyHandle = myHandle + 1
        val firstMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(notMyHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { timestamp }.thenReturn(initialTime)
        }

        val newTime = initialTime + TimeUnit.MINUTES.toSeconds(3) - 1
        val secondMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(notMyHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { timestamp }.thenReturn(newTime)
        }

        underTest(
            chatMessages = listOf(firstMessage, secondMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        verify(createTypedMessageUseCase).invoke(
            argForWhich {
                shouldShowTime
            }
        )

        verify(createTypedMessageUseCase).invoke(
            argForWhich {
                !shouldShowDate
            }
        )
    }

    @Test
    fun `test that date is shown for two messages sent on different dates`() {
        val initialTime = 100L
        val notMyHandle = myHandle + 1
        val firstMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(notMyHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { timestamp }.thenReturn(initialTime)
        }

        val newTime = initialTime + TimeUnit.DAYS.toSeconds(1) + 1
        val secondMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(notMyHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { timestamp }.thenReturn(newTime)
        }

        underTest(
            chatMessages = listOf(firstMessage, secondMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        verify(createTypedMessageUseCase, times(2)).invoke(
            argForWhich {
                shouldShowDate
            }
        )
    }

}