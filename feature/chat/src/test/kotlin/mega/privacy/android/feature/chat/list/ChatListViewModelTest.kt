package mega.privacy.android.feature.chat.list

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase.ChatRoomType
import mega.privacy.android.feature.chat.list.formatter.ChatLastMessageFormatter
import mega.privacy.android.feature.chat.list.mapper.ChatRoomTimestampMapper
import mega.privacy.android.feature.chat.list.mapper.ChatRoomUiItemMapper
import mega.privacy.android.feature.chat.list.model.ChatListTabState
import mega.privacy.android.feature.chat.list.model.ChatListUiState
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
class ChatListViewModelTest {

    private lateinit var underTest: ChatListViewModel

    private val getChatsUseCase = mock<GetChatsUseCase>()
    private val chatLastMessageFormatter = mock<ChatLastMessageFormatter>()
    private val chatRoomTimestampMapper = mock<ChatRoomTimestampMapper>()
    private val chatRoomUiItemMapper = mock<ChatRoomUiItemMapper>()

    private val chatRoomItem = ChatRoomItem.IndividualChatRoomItem(
        chatId = 1L,
        title = "Chat",
    )
    private val meetingRoomItem = ChatRoomItem.MeetingChatRoomItem(
        chatId = 2L,
        title = "Meeting",
    )
    private val chatUiItem = chatRoomUiItem(chatId = 1L, title = "Chat")
    private val meetingUiItem = chatRoomUiItem(chatId = 2L, title = "Meeting")

    private var capturedLastMessage: (suspend (Long) -> String)? = null
    private var capturedLastTimeMapper: ((Long) -> String)? = null
    private var capturedMeetingTimeMapper: ((Long, Long) -> String)? = null
    private var capturedHeaderTimeMapper: ((ChatRoomItem, ChatRoomItem?) -> String?)? = null

    @BeforeEach
    fun setUp() {
        underTest = ChatListViewModel(
            getChatsUseCase = getChatsUseCase,
            chatLastMessageFormatter = chatLastMessageFormatter,
            chatRoomTimestampMapper = chatRoomTimestampMapper,
            chatRoomUiItemMapper = chatRoomUiItemMapper,
        )
    }

    @AfterEach
    fun tearDown() {
        capturedLastMessage = null
        capturedLastTimeMapper = null
        capturedMeetingTimeMapper = null
        capturedHeaderTimeMapper = null
        reset(
            getChatsUseCase,
            chatLastMessageFormatter,
            chatRoomTimestampMapper,
            chatRoomUiItemMapper,
        )
    }

    @Test
    fun `test that initial state is Loading`() = runTest {
        stubChatRooms()

        assertThat(underTest.uiState.value).isEqualTo(ChatListUiState.Loading)
    }

    @Test
    fun `test that uiState emits Data with mapped chats and meetings when chat rooms are loaded`() =
        runTest {
            stubChatRooms()

            underTest.uiState.test {
                val actual = awaitDataState()

                assertThat(actual.chats.items()).containsExactly(chatUiItem)
                assertThat(actual.meetings.items()).containsExactly(meetingUiItem)
            }
        }

    @Test
    fun `test that a tab with no chat rooms is Empty when there is no search query`() = runTest {
        stubChatRooms(chats = emptyList(), meetings = emptyList())

        underTest.uiState.test {
            val actual = awaitDataState()

            assertThat(actual.chats).isEqualTo(ChatListTabState.Empty)
            assertThat(actual.meetings).isEqualTo(ChatListTabState.Empty)
        }
    }

    @Test
    fun `test that onSearchQueryChange filters chats by title`() = runTest {
        stubSearchableChats()

        underTest.uiState.test {
            awaitDataState()

            underTest.onSearchQueryChange("ali")

            val actual = awaitDataState()
            assertThat(actual.chats.items()).containsExactly(aliceUiItem)
        }
    }

    @Test
    fun `test that onSearchQueryChange filters chats by last message`() = runTest {
        stubSearchableChats()

        underTest.uiState.test {
            awaitDataState()

            underTest.onSearchQueryChange("dinner")

            val actual = awaitDataState()
            assertThat(actual.chats.items()).containsExactly(bobUiItem)
        }
    }

    @Test
    fun `test that a blank search query restores the full chat list`() = runTest {
        stubSearchableChats()

        underTest.uiState.test {
            awaitDataState()
            underTest.onSearchQueryChange("ali")
            awaitDataState()

            underTest.onSearchQueryChange(null)

            val actual = awaitDataState()
            assertThat(actual.chats.items()).containsExactly(aliceUiItem, bobUiItem)
        }
    }

    @Test
    fun `test that a search query with no matches emits NoSearchResults`() = runTest {
        stubSearchableChats()

        underTest.uiState.test {
            awaitDataState()

            underTest.onSearchQueryChange("zzz")

            val actual = awaitDataState()
            assertThat(actual.chats).isEqualTo(ChatListTabState.NoSearchResults)
        }
    }

    @Test
    fun `test that last message lambda delegates to the chat last message formatter`() =
        runTest {
            stubChatRooms()
            whenever(chatLastMessageFormatter(1L)) doReturn "Formatted preview"

            underTest.uiState.test { awaitDataState() }

            assertThat(capturedLastMessage?.invoke(1L)).isEqualTo("Formatted preview")
        }

    @Test
    fun `test that timestamp lambdas delegate to the timestamp mapper`() = runTest {
        stubChatRooms()
        whenever(chatRoomTimestampMapper.getLastTimeFormatted(100L)) doReturn "last time"
        whenever(chatRoomTimestampMapper.getMeetingTimeFormatted(100L, 200L)) doReturn "range"

        underTest.uiState.test { awaitDataState() }

        assertThat(capturedLastTimeMapper?.invoke(100L)).isEqualTo("last time")
        assertThat(capturedMeetingTimeMapper?.invoke(100L, 200L)).isEqualTo("range")
    }

    @Test
    fun `test that header time mapper lambda returns null`() = runTest {
        stubChatRooms()

        underTest.uiState.test { awaitDataState() }

        assertThat(capturedHeaderTimeMapper?.invoke(chatRoomItem, null)).isNull()
    }

    private fun stubChatRooms(
        chats: List<ChatRoomItem> = listOf(chatRoomItem),
        meetings: List<ChatRoomItem> = listOf(meetingRoomItem),
    ) {
        getChatsUseCase.stub {
            on { invoke(eq(ChatRoomType.NON_MEETINGS), any(), any(), any(), any()) } doAnswer { invocation ->
                captureMappers(invocation)
                flow {
                    emit(chats)
                    awaitCancellation()
                }
            }
            on { invoke(eq(ChatRoomType.MEETINGS), any(), any(), any(), any()) } doAnswer { invocation ->
                captureMappers(invocation)
                flow {
                    emit(meetings)
                    awaitCancellation()
                }
            }
        }
        chatRoomUiItemMapper.stub {
            on { invoke(chatRoomItem) } doReturn chatUiItem
            on { invoke(meetingRoomItem) } doReturn meetingUiItem
        }
    }

    private fun stubSearchableChats() {
        stubChatRooms(chats = listOf(aliceRoomItem, bobRoomItem), meetings = emptyList())
        chatRoomUiItemMapper.stub {
            on { invoke(aliceRoomItem) } doReturn aliceUiItem
            on { invoke(bobRoomItem) } doReturn bobUiItem
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun captureMappers(invocation: InvocationOnMock) {
        capturedLastMessage = invocation.getArgument<Any>(1) as suspend (Long) -> String
        capturedLastTimeMapper = invocation.getArgument<Any>(2) as (Long) -> String
        capturedMeetingTimeMapper = invocation.getArgument<Any>(3) as (Long, Long) -> String
        capturedHeaderTimeMapper =
            invocation.getArgument<Any>(4) as (ChatRoomItem, ChatRoomItem?) -> String?
    }

    private fun chatRoomUiItem(
        chatId: Long,
        title: String,
        lastMessage: String? = null,
    ) = ChatRoomUiItem(
        chatId = chatId,
        title = title,
        lastMessage = lastMessage,
        lastTimestampFormatted = null,
        scheduledTimestampFormatted = null,
        unreadCount = 0,
        isMuted = false,
        highlight = false,
        isNoteToSelf = false,
        avatar = ChatRoomUiItem.ChatRoomUiAvatar.Peer(
            placeholderText = null,
            filePath = null,
            color = null,
        ),
        status = ContactItemStatus.Unknown,
    )

    private fun ChatListTabState.items(): List<ChatRoomUiItem> =
        (this as ChatListTabState.Results).items

    private suspend fun ReceiveTurbine<ChatListUiState>.awaitDataState(): ChatListUiState.Data {
        var item = awaitItem()
        while (item !is ChatListUiState.Data) {
            item = awaitItem()
        }
        return item
    }

    private companion object {
        private val aliceRoomItem = ChatRoomItem.IndividualChatRoomItem(
            chatId = 10L,
            title = "Alice",
        )
        private val bobRoomItem = ChatRoomItem.IndividualChatRoomItem(
            chatId = 11L,
            title = "Bob",
        )
    }

    private val aliceUiItem = chatRoomUiItem(chatId = 10L, title = "Alice")
    private val bobUiItem = chatRoomUiItem(
        chatId = 11L,
        title = "Bob",
        lastMessage = "Dinner at 8?",
    )
}
