package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
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

        runBlocking {
            whenever(chatRepository.monitorChatListItemUpdates()).thenReturn(emptyFlow())
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `hasUnreadChats should return true if there are non-meeting chat rooms with unread messages`() =
        runTest {
            val nonMeetingChatRooms = listOf(
                CombinedChatRoom(chatId = 1L, unreadCount = 2),
                CombinedChatRoom(chatId = 2L, unreadCount = 0),
                CombinedChatRoom(chatId = 3L, unreadCount = 1)
            )
            whenever(chatRepository.getNonMeetingChatRooms()).thenReturn(nonMeetingChatRooms)
            whenever(chatRepository.getMeetingChatRooms()).thenReturn(emptyList())

            val result = underTest.invoke()

            assertThat(result.first().first).isTrue()
        }

    @Test
    fun `hasUnreadChats should return false if there are no non-meeting chat rooms with unread messages`() =
        runTest {
            val nonMeetingChatRooms = listOf(
                CombinedChatRoom(chatId = 1L, unreadCount = 0),
                CombinedChatRoom(chatId = 2L, unreadCount = 0),
                CombinedChatRoom(chatId = 3L, unreadCount = 0)
            )
            whenever(chatRepository.getNonMeetingChatRooms()).thenReturn(nonMeetingChatRooms)
            whenever(chatRepository.getMeetingChatRooms()).thenReturn(emptyList())

            val result = underTest.invoke()

            assertThat(result.first().first).isFalse()
        }

    @Test
    fun `hasUnreadMeetings should return true if there are meeting chat rooms with unread messages`() =
        runTest {
            val meetingChatRooms = listOf(
                CombinedChatRoom(chatId = 4L, unreadCount = 0),
                CombinedChatRoom(chatId = 5L, unreadCount = 3),
                CombinedChatRoom(chatId = 6L, unreadCount = 0)
            )
            whenever(chatRepository.getMeetingChatRooms()).thenReturn(meetingChatRooms)
            whenever(chatRepository.getNonMeetingChatRooms()).thenReturn(emptyList())

            val result = underTest.invoke()

            assertThat(result.first().second).isTrue()
        }

    @Test
    fun `hasUnreadMeetings should return false if there are no meeting chat rooms with unread messages`() =
        runTest {
            val meetingChatRooms = listOf(
                CombinedChatRoom(chatId = 4L, unreadCount = 0),
                CombinedChatRoom(chatId = 5L, unreadCount = 0),
                CombinedChatRoom(chatId = 6L, unreadCount = 0)
            )
            whenever(chatRepository.getMeetingChatRooms()).thenReturn(meetingChatRooms)
            whenever(chatRepository.getNonMeetingChatRooms()).thenReturn(emptyList())

            val result = underTest.invoke()

            assertThat(result.first().second).isFalse()
        }
}
