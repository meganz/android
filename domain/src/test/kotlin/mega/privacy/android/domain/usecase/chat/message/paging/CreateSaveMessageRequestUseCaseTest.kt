package mega.privacy.android.domain.usecase.chat.message.paging

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateSaveMessageRequestUseCaseTest {
    private lateinit var underTest: CreateSaveMessageRequestUseCase
    private val myHandle = 123L

    @BeforeAll
    internal fun setUp() {
        underTest = CreateSaveMessageRequestUseCase()
    }

    @Test
    fun `test that show avatar is false for my messages`() {
        val myMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(myHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
        }

        val actual = underTest(
            chatMessages = listOf(myMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        assertThat(actual.all { !it.shouldShowAvatar }).isTrue()
    }

    @Test
    fun `test that showAvatar is true for a single message with no next message`() {
        val notMyMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(myHandle + 1)
            on { type }.thenReturn(ChatMessageType.NORMAL)
        }

        val actual = underTest(
            chatMessages = listOf(notMyMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        assertThat(actual.all { it.shouldShowAvatar }).isTrue()
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

        val actual = underTest(
            chatMessages = listOf(firstMessage, secondMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        assertThat(actual.map { it.shouldShowAvatar }).containsExactly(false, true)
    }


    @Test
    fun `test that show avatar is false for a single message with a next message with the same user handle`() {
        val notMyHandle = myHandle + 1
        val notMyMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(notMyHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
        }

        val actual = underTest(
            chatMessages = listOf(notMyMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = notMyHandle,
        )

        assertThat(actual.all { !it.shouldShowAvatar }).isTrue()
    }

    @Test
    fun `test that time is displayed if first message`() {
        val myMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(myHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
        }

        val actual = underTest(
            chatMessages = listOf(myMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        assertThat(actual.all { it.shouldShowTime }).isTrue()
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

        val newTime = initialTime + TimeUnit.MINUTES.toSeconds(3) - 1
        val secondUser = firstUser + 1
        val secondMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(secondUser)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { timestamp }.thenReturn(newTime)
        }

        val actual = underTest(
            chatMessages = listOf(firstMessage, secondMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        assertThat(actual.all { it.shouldShowTime }).isTrue()

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

        val actual = underTest(
            chatMessages = listOf(firstMessage, secondMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        assertThat(actual.map { it.shouldShowTime }).containsExactly(true, false)
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

        val actual = underTest(
            chatMessages = listOf(firstMessage, secondMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        assertThat(actual.map { it.shouldShowTime }).containsExactly(true, true)
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

        val actual = underTest(
            chatMessages = listOf(firstMessage, secondMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        assertThat(actual.map { it.shouldShowDate }).containsExactly(true, false)
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

        val actual = underTest(
            chatMessages = listOf(firstMessage, secondMessage),
            currentUserHandle = myHandle,
            nextMessageUserHandle = null,
        )

        assertThat(actual.map { it.shouldShowDate }).containsExactly(true, true)
    }
}