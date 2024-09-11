package mega.privacy.android.app.presentation.meeting.chat.model

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.di.meeting.chat.paging.PagedChatMessageRemoteMediatorFactory
import mega.privacy.android.app.presentation.meeting.chat.mapper.ChatMessageDateSeparatorMapper
import mega.privacy.android.app.presentation.meeting.chat.mapper.ChatMessageTimeSeparatorMapper
import mega.privacy.android.app.presentation.meeting.chat.mapper.UiChatMessageMapper
import mega.privacy.android.app.presentation.meeting.chat.model.messages.normal.TextUiMessage
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.usecase.MonitorContactCacheUpdates
import mega.privacy.android.domain.usecase.chat.message.SetMessageSeenUseCase
import mega.privacy.android.domain.usecase.chat.message.paging.GetChatPagingSourceUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MessageListViewModelTest {
    private lateinit var underTest: MessageListViewModel
    private val chatId = 123L
    private val uiChatMessageMapper: UiChatMessageMapper = mock()
    private val getChatPagingSourceUseCase: GetChatPagingSourceUseCase = mock()
    private val chatMessageDateSeparatorMapper = mock<ChatMessageDateSeparatorMapper>()
    private val remoteMediatorFactory: PagedChatMessageRemoteMediatorFactory = mock()
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(
        mapOf(
            "chatId" to chatId.toString(),
            "chatAction" to Constants.ACTION_CHAT_SHOW_MESSAGES,
        )
    )
    private val setMessageSeenUseCase: SetMessageSeenUseCase = mock()
    private val monitorContactCacheUpdates: MonitorContactCacheUpdates = mock {
        onBlocking { invoke() } doReturn emptyFlow()
    }
    private val chatMessageTimeSeparatorMapper: ChatMessageTimeSeparatorMapper = mock()

    @BeforeAll
    fun setup() {
        initTestClass()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            uiChatMessageMapper,
            getChatPagingSourceUseCase,
            chatMessageDateSeparatorMapper,
            remoteMediatorFactory,
            setMessageSeenUseCase
        )
    }

    private fun initTestClass() {
        underTest = MessageListViewModel(
            uiChatMessageMapper = uiChatMessageMapper,
            getChatPagingSourceUseCase = getChatPagingSourceUseCase,
            chatMessageDateSeparatorMapper = chatMessageDateSeparatorMapper,
            remoteMediatorFactory = remoteMediatorFactory,
            savedStateHandle = savedStateHandle,
            setMessageSeenUseCase = setMessageSeenUseCase,
            monitorChatRoomMessageUpdatesUseCase = mock(),
            monitorReactionUpdatesUseCase = mock(),
            monitorContactCacheUpdates = monitorContactCacheUpdates,
            monitorPendingMessagesUseCase = mock(),
            chatMessageTimeSeparatorMapper = chatMessageTimeSeparatorMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `test that isJumpingToLastSeenMessage updates correctly when handled`() = runTest {
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isJumpingToLastSeenMessage).isEqualTo(false)
        }
        underTest.onScrolledToLastSeenMessage()
        underTest.state.test {
            assertThat(awaitItem().isJumpingToLastSeenMessage).isEqualTo(true)
        }
    }

    @Test
    fun `test that set message seen use case is called when message is seen`() = runTest {
        val lastMessageId = 123L
        underTest.setMessageSeen(lastMessageId)
        verify(setMessageSeenUseCase).invoke(chatId, lastMessageId)
    }

    @Test
    fun `test that userUpdates is updated when user updates`() = runTest {
        val updateFlow = MutableSharedFlow<UserUpdate>()
        whenever(monitorContactCacheUpdates()).thenReturn(updateFlow)
        initTestClass()
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.userUpdate).isNull()
        }
        val userUpdate = mock<UserUpdate> {
            on { changes } doReturn mapOf(UserId(1L) to listOf(UserChanges.Avatar))
        }
        updateFlow.emit(userUpdate)
        advanceUntilIdle()
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.userUpdate).isEqualTo(userUpdate)
        }
    }

    @Test
    fun `test that updateLatestMessage update state correctly`() = runTest {
        val messageId = 123L
        val typedMessage = mock<TextMessage> {
            on { msgId } doReturn messageId
            on { isMine } doReturn true
            on { status } doReturn ChatMessageStatus.NOT_SEEN
        }
        val uiMessage = mock<TextUiMessage> {
            on { message } doReturn typedMessage
            on { id } doReturn messageId
        }
        assertThat(underTest.latestMessageId.longValue).isEqualTo(-1L)
        underTest.updateLatestMessage(listOf(uiMessage))
        assertThat(underTest.latestMessageId.longValue).isEqualTo(messageId)
        underTest.updateLatestMessage(listOf(uiMessage))
        // verify call only once
        verify(setMessageSeenUseCase).invoke(chatId, messageId)
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.extraUnreadCount).isEqualTo(0)
        }
    }

    @Test
    fun `test that onUserUpdateHandled set userUpdate to null`() = runTest {
        underTest.onUserUpdateHandled()
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.userUpdate).isNull()
        }
    }

    @Test
    fun `test that all received message clear when jump to latest message`() = runTest {
        underTest.onScrollToLatestMessage()
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.receivedMessages).isEmpty()
        }
    }

    @Test
    fun `test that scrolled to the last seen message update state correctly`() = runTest {
        initTestClass()
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.isJumpingToLastSeenMessage).isFalse()
        }
        underTest.onScrolledToLastSeenMessage()
        advanceUntilIdle()
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.isJumpingToLastSeenMessage).isTrue()
        }
    }
}