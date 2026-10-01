package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetChatsUnreadStatusUseCaseTest {

    private lateinit var underTest: GetChatsUnreadStatusUseCase
    private val testDispatcher = UnconfinedTestDispatcher()
    private val chatRepository = mock<ChatRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        underTest = GetChatsUnreadStatusUseCase(chatRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `hasUnreadChats should return true if there are non-meeting chat rooms`() =
        runTest {
            val nonMeetingChatRooms = listOf(
                ChatListItem(chatId = 1L),
                ChatListItem(chatId = 2L),
                ChatListItem(chatId = 3L)
            )
            whenever(chatRepository.getUnreadNonMeetingChatListItems()).thenReturn(nonMeetingChatRooms)
            whenever(chatRepository.getUnreadMeetingChatListItems()).thenReturn(emptyList())

            val result = underTest.invoke().first()

            assertThat(result.first).isTrue()
        }

    @Test
    fun `hasUnreadChats should return false if there are no non-meeting chat rooms`() =
        runTest {
            whenever(chatRepository.getUnreadNonMeetingChatListItems()).thenReturn(emptyList())
            whenever(chatRepository.getUnreadMeetingChatListItems()).thenReturn(emptyList())

            val result = underTest.invoke().first()

            assertThat(result.first).isFalse()
        }

    @Test
    fun `hasUnreadMeetings should return true if there are meeting chat rooms`() =
        runTest {
            val meetingChatRooms = listOf(
                ChatListItem(chatId = 4L),
                ChatListItem(chatId = 5L),
                ChatListItem(chatId = 6L)
            )
            whenever(chatRepository.getUnreadMeetingChatListItems()).thenReturn(meetingChatRooms)
            whenever(chatRepository.getUnreadNonMeetingChatListItems()).thenReturn(emptyList())

            val result = underTest.invoke().first()

            assertThat(result.second).isTrue()
        }

    @Test
    fun `hasUnreadMeetings should return false if there are no meeting chat rooms`() =
        runTest {
            whenever(chatRepository.getUnreadMeetingChatListItems()).thenReturn(emptyList())
            whenever(chatRepository.getUnreadNonMeetingChatListItems()).thenReturn(emptyList())

            val result = underTest.invoke().first()

            assertThat(result.second).isFalse()
        }

    @Test
    fun `test that updated unread status is emitted when chat list item changes`() = runTest {
        val initialNonMeetingChatRooms = listOf(ChatListItem(chatId = 1L))
        val updatedNonMeetingChatRooms = listOf(ChatListItem(chatId = 2L))
        val initialMeetingChatRooms = listOf(ChatListItem(chatId = 3L))
        val updatedMeetingChatRooms = listOf(ChatListItem(chatId = 4L))
        val chatListItemUpdate = ChatListItem(
            chatId = -1L,
            changes = ChatListItemChanges.UnreadCount
        )

        whenever(chatRepository.getUnreadNonMeetingChatListItems())
            .thenReturn(initialNonMeetingChatRooms)
            .thenReturn(updatedNonMeetingChatRooms)

        whenever(chatRepository.getUnreadMeetingChatListItems())
            .thenReturn(initialMeetingChatRooms)
            .thenReturn(updatedMeetingChatRooms)

        whenever(chatRepository.monitorChatListItemUpdates())
            .thenReturn(flowOf(chatListItemUpdate))

        val result = underTest.invoke().last()

        assertThat(result.first).isEqualTo(updatedNonMeetingChatRooms.isNotEmpty())
        assertThat(result.second).isEqualTo(updatedMeetingChatRooms.isNotEmpty())
    }
}
