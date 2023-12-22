package mega.privacy.android.app.presentation.meeting.chat.mapper

import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.model.paging.PagingLoadResult
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.concurrent.TimeUnit

class PagedTypedMessageResultUiMapperTest {
    private lateinit var underTest: PagedTypedMessageResultUiMapper

    private val uiChatMessage = mock<UiChatMessage>()
    private val uiChatMessageMapper = mock<UiChatMessageMapper> {
        on {
            invoke(
                message = any(),
                showAvatar = any(),
                showTime = any(),
                showDate = any(),
            )
        }.thenReturn(uiChatMessage)
    }

    @BeforeEach
    internal fun setUp() {
        underTest = PagedTypedMessageResultUiMapper(
            uiChatMessageMapper,
        )
    }

    private val defaultPagingLoadResult = PagingLoadResult(
        loadStatus = ChatHistoryLoadStatus.LOCAL,
        nextMessageUserHandle = null,
        nexMessageIsMine = null,
    )

    @Test
    fun `test that show avatar is false for my messages`() {
        val myMessage = mock<TypedMessage> {
            on { isMine }.thenReturn(true)
        }

        underTest(
            pagingLoadResult = defaultPagingLoadResult,
            typedMessages = listOf(myMessage),
        )

        verify(uiChatMessageMapper).invoke(
            message = any(),
            showAvatar = eq(false),
            showTime = any(),
            showDate = any(),
        )
    }

    @Test
    fun `test that showAvatar is true for a single message with no next message`() {
        val typedMessage = mock<TypedMessage>()

        underTest(
            pagingLoadResult = defaultPagingLoadResult,
            typedMessages = listOf(typedMessage),
        )

        verify(uiChatMessageMapper).invoke(
            message = any(),
            showAvatar = eq(true),
            showTime = any(),
            showDate = any(),
        )
    }

    @Test
    fun `test that avatar is true only for second message of two with the same user handle and no next message`() {
        val firstTypedMessage = mock<TypedMessage> {
            on { userHandle }.thenReturn(1L)
        }
        val secondTypedMessage = mock<TypedMessage> {
            on { userHandle }.thenReturn(1L)
        }

        underTest(
            pagingLoadResult = defaultPagingLoadResult,
            typedMessages = listOf(firstTypedMessage, secondTypedMessage),
        )

        verify(uiChatMessageMapper).invoke(
            message = eq(firstTypedMessage),
            showAvatar = eq(false),
            showTime = any(),
            showDate = any(),
        )

        verify(uiChatMessageMapper).invoke(
            message = eq(secondTypedMessage),
            showAvatar = eq(true),
            showTime = any(),
            showDate = any(),
        )
    }

    @Test
    fun `test that show avatar is false for a single message with a next message with the same user handle`() {
        val typedMessage = mock<TypedMessage> {
            on { userHandle }.thenReturn(1L)
        }

        underTest(
            pagingLoadResult = PagingLoadResult(
                loadStatus = ChatHistoryLoadStatus.LOCAL,
                nextMessageUserHandle = 1L,
                nexMessageIsMine = null,
            ),
            typedMessages = listOf(typedMessage),
        )

        verify(uiChatMessageMapper).invoke(
            message = any(),
            showAvatar = eq(false),
            showTime = any(),
            showDate = any(),
        )
    }

    @Test
    fun `test that time is displayed if first message`() {
        val typedMessage = mock<TypedMessage> {
            on { isMine }.thenReturn(true)
        }

        underTest(
            pagingLoadResult = defaultPagingLoadResult,
            typedMessages = listOf(typedMessage)
        )

        verify(uiChatMessageMapper).invoke(
            message = eq(typedMessage),
            showAvatar = any(),
            showTime = eq(true),
            showDate = any(),
        )
    }

    @Test
    fun `test that time is displayed for two messages from different senders`() {
        val initialTime = 100L
        val firstMessage = mock<TypedMessage> {
            on { isMine }.thenReturn(false)
            on { userHandle }.thenReturn(1L)
            on { time }.thenReturn(initialTime)
        }

        val newTime = initialTime + TimeUnit.MINUTES.toSeconds(3) + 1
        val secondMessage = mock<TypedMessage> {
            on { isMine }.thenReturn(false)
            on { userHandle }.thenReturn(2L)
            on { time }.thenReturn(newTime)
        }

        underTest(
            pagingLoadResult = defaultPagingLoadResult,
            typedMessages = listOf(firstMessage, secondMessage),
        )

        verify(uiChatMessageMapper).invoke(
            message = eq(firstMessage),
            showAvatar = any(),
            showTime = eq(true),
            showDate = any(),
        )

        verify(uiChatMessageMapper).invoke(
            message = eq(secondMessage),
            showAvatar = any(),
            showTime = eq(true),
            showDate = any(),
        )
    }

    @Test
    fun `test that time is only displayed for the first of two messages from the same user within 3 minutes`() {
        val initialTime = 100L
        val firstMessage = mock<TypedMessage> {
            on { isMine }.thenReturn(false)
            on { userHandle }.thenReturn(1L)
            on { time }.thenReturn(initialTime)
        }

        val newTime = initialTime + TimeUnit.MINUTES.toSeconds(3) - 1
        val secondMessage = mock<TypedMessage> {
            on { isMine }.thenReturn(false)
            on { userHandle }.thenReturn(1L)
            on { time }.thenReturn(newTime)
        }

        underTest(
            pagingLoadResult = defaultPagingLoadResult,
            typedMessages = listOf(firstMessage, secondMessage),
        )

        verify(uiChatMessageMapper).invoke(
            message = eq(firstMessage),
            showAvatar = any(),
            showTime = eq(true),
            showDate = any(),
        )

        verify(uiChatMessageMapper).invoke(
            message = eq(secondMessage),
            showAvatar = any(),
            showTime = eq(false),
            showDate = any(),
        )
    }

    @Test
    fun `test that time is displayed on both messages from the same sender if more than 3 minutes apart`() {
        val initialTime = 100L
        val firstMessage = mock<TypedMessage> {
            on { isMine }.thenReturn(false)
            on { userHandle }.thenReturn(1L)
            on { time }.thenReturn(initialTime)
        }

        val newTime = initialTime + TimeUnit.MINUTES.toSeconds(3) + 1
        val secondMessage = mock<TypedMessage> {
            on { isMine }.thenReturn(false)
            on { userHandle }.thenReturn(1L)
            on { time }.thenReturn(newTime)
        }

        underTest(
            pagingLoadResult = defaultPagingLoadResult,
            typedMessages = listOf(firstMessage, secondMessage),
        )

        verify(uiChatMessageMapper).invoke(
            message = eq(firstMessage),
            showAvatar = any(),
            showTime = eq(true),
            showDate = any(),
        )

        verify(uiChatMessageMapper).invoke(
            message = eq(secondMessage),
            showAvatar = any(),
            showTime = eq(true),
            showDate = any(),
        )
    }

    @Test
    fun `test that date is not shown for second message sent on the same date`() {
        val initialTime = 100L
        val firstMessage = mock<TypedMessage> {
            on { isMine }.thenReturn(false)
            on { userHandle }.thenReturn(1L)
            on { time }.thenReturn(initialTime)
        }

        val newTime = initialTime + TimeUnit.MINUTES.toSeconds(3) + 1
        val secondMessage = mock<TypedMessage> {
            on { isMine }.thenReturn(false)
            on { userHandle }.thenReturn(1L)
            on { time }.thenReturn(newTime)
        }

        underTest(
            pagingLoadResult = defaultPagingLoadResult,
            typedMessages = listOf(firstMessage, secondMessage),
        )

        verify(uiChatMessageMapper).invoke(
            message = eq(firstMessage),
            showAvatar = any(),
            showTime = any(),
            showDate = eq(true),
        )

        verify(uiChatMessageMapper).invoke(
            message = eq(secondMessage),
            showAvatar = any(),
            showTime = any(),
            showDate = eq(false),
        )
    }

    @Test
    fun `test that date is shown for two messages sent on different dates`() {
        val initialTime = 100L
        val firstMessage = mock<TypedMessage> {
            on { isMine }.thenReturn(false)
            on { userHandle }.thenReturn(1L)
            on { time }.thenReturn(initialTime)
        }

        val newTime = initialTime + TimeUnit.DAYS.toSeconds(1) + 1
        val secondMessage = mock<TypedMessage> {
            on { isMine }.thenReturn(false)
            on { userHandle }.thenReturn(1L)
            on { time }.thenReturn(newTime)
        }

        underTest(
            pagingLoadResult = defaultPagingLoadResult,
            typedMessages = listOf(firstMessage, secondMessage),
        )

        verify(uiChatMessageMapper).invoke(
            message = eq(firstMessage),
            showAvatar = any(),
            showTime = any(),
            showDate = eq(true),
        )

        verify(uiChatMessageMapper).invoke(
            message = eq(secondMessage),
            showAvatar = any(),
            showTime = any(),
            showDate = eq(true),
        )
    }
}