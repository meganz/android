package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.mapper.chat.ChatRoomItemMapper
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.PushesRepository
import mega.privacy.android.domain.usecase.ChatRoomItemStatusMapper
import mega.privacy.android.domain.usecase.contact.GetContactEmail
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCall
import mega.privacy.android.domain.usecase.meeting.GetScheduleMeetingDataUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
internal class GetChatsUseCaseTest {

    private lateinit var underTest: GetChatsUseCase
    private val testDispatcher = UnconfinedTestDispatcher()

    private val chatRepository = mock<ChatRepository>()
    private val pushesRepository = mock<PushesRepository>()
    private val getScheduleMeetingDataUseCase = mock<GetScheduleMeetingDataUseCase>()
    private val getChatGroupAvatarUseCase = mock<GetChatGroupAvatarUseCase>()
    private val chatRoomItemMapper = mock<ChatRoomItemMapper>()
    private val chatRoomItemStatusMapper = mock<ChatRoomItemStatusMapper>()
    private val contactsRepository = mock<ContactsRepository>()
    private val getChatCall = mock<GetChatCall>()
    private val monitorChatCallUpdates = mock<MonitorChatCallUpdates>()
    private val getUserOnlineStatusByHandleUseCase = mock<GetUserOnlineStatusByHandleUseCase>()
    private val getUserEmail = mock<GetContactEmail>()

    private val lastMessage: suspend (Long) -> String = { "test" }
    private val lastTimeMapper: (Long) -> String = { "test" }
    private val meetingTimeMapper: (Long, Long) -> String = { _, _ -> "test" }
    private val headerTimeMapper: (ChatRoomItem, ChatRoomItem?) -> String = { _, _ -> "test" }

    private val chatRooms = listOf(
        CombinedChatRoom(
            chatId = 1L,
            lastTimestamp = Random.nextLong(),
            isActive = true
        ),
        CombinedChatRoom(
            chatId = 2L,
            lastTimestamp = Random.nextLong(),
            isActive = true
        ),
        CombinedChatRoom(
            chatId = 3L,
            lastTimestamp = Random.nextLong(),
            isActive = true
        ),
        CombinedChatRoom(
            chatId = 4L,
            lastTimestamp = Random.nextLong(),
            isActive = true
        ),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        underTest = GetChatsUseCase(
            chatRepository,
            pushesRepository,
            getScheduleMeetingDataUseCase,
            getChatGroupAvatarUseCase,
            chatRoomItemMapper,
            chatRoomItemStatusMapper,
            contactsRepository,
            getChatCall,
            monitorChatCallUpdates,
            getUserOnlineStatusByHandleUseCase,
            getUserEmail
        )

        runBlocking {
            whenever(chatRepository.getNonMeetingChatRooms()).thenReturn(chatRooms)
            whenever(chatRepository.getMeetingChatRooms()).thenReturn(chatRooms)
            whenever(chatRepository.getArchivedChatRooms()).thenReturn(chatRooms)
            whenever(chatRepository.isChatNotifiable(any())).thenReturn(Random.nextBoolean())
            whenever(chatRepository.isChatLastMessageGeolocation(any())).thenReturn(Random.nextBoolean())
            whenever(getChatCall(any())).thenReturn(null)
            whenever(getChatGroupAvatarUseCase(any())).thenReturn(null)
            whenever(getUserOnlineStatusByHandleUseCase(any())).thenReturn(null)
            whenever(getUserEmail(any())).thenReturn(null)
            whenever(getScheduleMeetingDataUseCase.invoke(any(), any())).thenReturn(null)
            whenever(pushesRepository.monitorPushNotificationSettings()).thenReturn(emptyFlow())
            whenever(monitorChatCallUpdates.invoke()).thenReturn(emptyFlow())
            whenever(chatRepository.monitorChatListItemUpdates()).thenReturn(emptyFlow())
            whenever(contactsRepository.monitorChatOnlineStatusUpdates()).thenReturn(emptyFlow())
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that meetings are retrieved based chat room type parameter`() =
        runTest {
            val chatRoomType = GetChatsUseCase.ChatRoomType.MEETINGS

            underTest.invoke(
                chatRoomType = chatRoomType,
                lastMessage = lastMessage,
                lastTimeMapper = lastTimeMapper,
                meetingTimeMapper = meetingTimeMapper,
                headerTimeMapper = headerTimeMapper,
            ).first()

            verify(chatRepository).getMeetingChatRooms()
        }

    @Test
    fun `test that non meetings are retrieved based chat room type parameter`() =
        runTest {
            val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

            underTest.invoke(
                chatRoomType = chatRoomType,
                lastMessage = lastMessage,
                lastTimeMapper = lastTimeMapper,
                meetingTimeMapper = meetingTimeMapper,
                headerTimeMapper = headerTimeMapper,
            ).first()

            verify(chatRepository).getNonMeetingChatRooms()
        }

    @Test
    fun `test that archived are retrieved based chat room type parameter`() =
        runTest {
            val chatRoomType = GetChatsUseCase.ChatRoomType.ARCHIVED_CHATS

            underTest.invoke(
                chatRoomType = chatRoomType,
                lastMessage = lastMessage,
                lastTimeMapper = lastTimeMapper,
                meetingTimeMapper = meetingTimeMapper,
                headerTimeMapper = headerTimeMapper,
            ).first()

            verify(chatRepository).getArchivedChatRooms()
        }

    @Test
    fun `test that chat rooms are sorted by last timestamp`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).first()

        val sortedChatRooms = chatRooms.sortedByDescending(CombinedChatRoom::lastTimestamp)

        assertThat(result.first().chatId).isEqualTo(sortedChatRooms.first().chatId)
    }

    @Test
    fun `test that isChatNotifiable is called accordingly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(chatRepository, times(chatRooms.size)).isChatNotifiable(anyLong())
    }

    @Test
    fun `test that isChatLastMessageGeolocation is called accordingly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(chatRepository, times(chatRooms.size)).isChatLastMessageGeolocation(anyLong())
    }

    @Test
    fun `test that getChatCall is called accordingly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(getChatCall, times(chatRooms.size)).invoke(anyLong())
    }

    @Test
    fun `test that getChatGroupAvatarUseCase is called accordingly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(getChatGroupAvatarUseCase, times(chatRooms.size)).invoke(anyLong())
    }
}
