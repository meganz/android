package mega.privacy.android.domain.usecase.chat.message.paging

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.usecase.chat.message.reactions.GetMessageReactionsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateSaveMessageRequestUseCaseTest {

    private lateinit var underTest: CreateSaveMessageRequestUseCase

    private val getMessageReactionsUseCase = mock<GetMessageReactionsUseCase>()

    private val myHandle = 123L
    private val chatId = 456L

    @BeforeAll
    internal fun setUp() {
        underTest = CreateSaveMessageRequestUseCase(getMessageReactionsUseCase)
    }

    @BeforeEach
    internal fun resetMocks() {
        reset(getMessageReactionsUseCase)
    }

    @Test
    fun `test that show avatar is false for my messages`() = runTest {
        val myMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(myHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
        }

        val actual = underTest(
            chatId = chatId,
            chatMessages = listOf(myMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        assertThat(actual.all { !it.shouldShowAvatar }).isTrue()
    }

    @Test
    fun `test that showAvatar is true for a single message with no next message`() = runTest {
        val notMyMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(myHandle + 1)
            on { type }.thenReturn(ChatMessageType.NORMAL)
        }

        val actual = underTest(
            chatId = chatId,
            chatMessages = listOf(notMyMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        assertThat(actual.all { it.shouldShowAvatar }).isTrue()
    }

    @Test
    fun `test that avatar is true only for second message of two with the same user handle and no next message`() =
        runTest {
            val firstMessage = mock<ChatMessage> {
                on { userHandle }.thenReturn(myHandle + 1)
                on { type }.thenReturn(ChatMessageType.NORMAL)
            }

            val secondMessage = mock<ChatMessage> {
                on { userHandle }.thenReturn(myHandle + 1)
                on { type }.thenReturn(ChatMessageType.NORMAL)
            }

            val actual = underTest(
                chatId = chatId,
                chatMessages = listOf(firstMessage, secondMessage),
                currentUserHandle = myHandle,
                nextMessageUserHandle = null,
            )

            assertThat(actual.map { it.shouldShowAvatar }).containsExactly(false, true)
        }


    @Test
    fun `test that show avatar is false for a single message with a next message with the same user handle`() =
        runTest {
            val notMyHandle = myHandle + 1
            val notMyMessage = mock<ChatMessage> {
                on { userHandle }.thenReturn(notMyHandle)
                on { type }.thenReturn(ChatMessageType.NORMAL)
            }

            val actual = underTest(
                chatId = chatId,
                chatMessages = listOf(notMyMessage),
                currentUserHandle = myHandle,
                nextMessageUserHandle = notMyHandle,
            )

            assertThat(actual.all { !it.shouldShowAvatar }).isTrue()
        }

    @Test
    fun `test that time is displayed if first message`() = runTest {
        val myMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(myHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
        }

        val actual = underTest(
            chatId = chatId,
            chatMessages = listOf(myMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        assertThat(actual.all { it.shouldShowTime }).isTrue()
    }

    @Test
    fun `test that time is displayed for two messages from different senders`() = runTest {
        val initialTime = 100L
        val firstUser = myHandle + 1
        val firstMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(firstUser)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { timestamp }.thenReturn(initialTime)
        }

        val newTime = initialTime + TimeUnit.MINUTES.toSeconds(3) - 1
        val secondUser = firstUser + 1
        val secondMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(secondUser)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { timestamp }.thenReturn(newTime)
        }

        val actual = underTest(
            chatId = chatId,
            chatMessages = listOf(firstMessage, secondMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        assertThat(actual.all { it.shouldShowTime }).isTrue()

    }

    @Test
    fun `test that time is only displayed for the first of two messages from the same user within 3 minutes`() =
        runTest {
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

            val actual = underTest(
                chatId = chatId,
                chatMessages = listOf(firstMessage, secondMessage),
                currentUserHandle = myHandle,
                nextMessageUserHandle = null,
            )

            assertThat(actual.map { it.shouldShowTime }).containsExactly(true, false)
        }

    @Test
    fun `test that time is displayed on both messages from the same sender if more than 3 minutes apart`() =
        runTest {
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

            val actual = underTest(
                chatId = chatId,
                chatMessages = listOf(firstMessage, secondMessage),
                currentUserHandle = myHandle,
                nextMessageUserHandle = null,
            )

            assertThat(actual.map { it.shouldShowTime }).containsExactly(true, true)
        }


    @Test
    fun `test that date is not shown for second message sent on the same date`() = runTest {
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

        val actual = underTest(
            chatId = chatId,
            chatMessages = listOf(firstMessage, secondMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        assertThat(actual.map { it.shouldShowDate }).containsExactly(true, false)
    }

    @Test
    fun `test that date is shown for two messages sent on different dates`() = runTest {
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

        val actual = underTest(
            chatId = chatId,
            chatMessages = listOf(firstMessage, secondMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        assertThat(actual.map { it.shouldShowDate }).containsExactly(true, true)
    }

    @Test
    fun `test that reactions are retrieved for messages with confirmed reactions`() = runTest {
        val message = mock<ChatMessage> {
            on { hasConfirmedReactions }.thenReturn(true)
        }
        val reaction1 = mock<Reaction>()
        val reaction2 = mock<Reaction>()
        val reactions = listOf(reaction1, reaction2)
        whenever(getMessageReactionsUseCase(chatId, message.msgId, myHandle)).thenReturn(reactions)

        val actual = underTest(
            chatId = chatId,
            chatMessages = listOf(message),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )
        assertThat(actual.map { it.reactions }).containsExactly(reactions)
    }

    @Test
    fun `test that reactions are not for messages without confirmed reactions`() = runTest {
        val message = mock<ChatMessage> {
            on { hasConfirmedReactions }.thenReturn(false)
        }

        val actual = underTest(
            chatId = chatId,
            chatMessages = listOf(message),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )
        assertThat(actual.map { it.reactions }).containsExactly(emptyList<Reaction>())
        verifyNoInteractions(getMessageReactionsUseCase)
    }
}