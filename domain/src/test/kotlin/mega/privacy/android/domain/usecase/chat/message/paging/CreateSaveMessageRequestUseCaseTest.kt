package mega.privacy.android.domain.usecase.chat.message.paging

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.usecase.chat.message.reactions.GetReactionsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateSaveMessageRequestUseCaseTest {

    private lateinit var underTest: CreateSaveMessageRequestUseCase

    private val getReactionsUseCase = mock<GetReactionsUseCase>()

    private val myHandle = 123L
    private val chatId = 456L

    @BeforeAll
    internal fun setUp() {
        underTest = CreateSaveMessageRequestUseCase(getReactionsUseCase)
    }

    @BeforeEach
    internal fun resetMocks() {
        reset(getReactionsUseCase)
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
    fun `test that reactions are retrieved for messages with confirmed reactions`() = runTest {
        val message = mock<ChatMessage> {
            on { hasConfirmedReactions }.thenReturn(true)
        }
        val reaction1 = mock<Reaction>()
        val reaction2 = mock<Reaction>()
        val reactions = listOf(reaction1, reaction2)
        whenever(getReactionsUseCase(chatId, message.messageId, myHandle)).thenReturn(reactions)

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
        verifyNoInteractions(getReactionsUseCase)
    }
}